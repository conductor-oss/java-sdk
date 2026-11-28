/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package durability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The part of {@link Failover} that reads the server's records of a kitchensink execution ({@code
 * GET /workflow/{id}?includeTasks=true}): what the execution is doing, which worker holds it, and
 * whether a kill of that worker was handed over to another one.
 */
final class FailoverTargets {

    private FailoverTargets() {}

    /** What a kill is aimed at, in the order the bench aims: the point of the code it lands in. */
    static final List<String> TARGETS = List.of("long-task", "java-loop", "doWhile", "server-loop", "switch", "fork",
            "timer", "human", "external", "child", "continuation");

    private static final Set<String> IN_FLIGHT = Set.of("SCHEDULED", "IN_PROGRESS");
    /** The SDK names a plain Java loop after its class and line; doWhile loops by the name the code gives. */
    private static final Pattern JAVA_LOOP = Pattern.compile(".*_loop_\\d+");
    /** sleep and sleepUntil; any other WAIT in the kitchensink is external(ref). */
    private static final Pattern TIMER = Pattern.compile("sleep(_until)?(_\\d+)?");
    /** A DO_WHILE iteration's copy of a task: its reference name and {@code __<iteration>}. */
    private static final Pattern ITERATION = Pattern.compile("__\\d+$");

    /**
     * One fault on the worker that held an execution.
     *
     * @param type {@code kill} (SIGKILL), {@code pause} (SIGSTOP past every lease, then SIGCONT),
     *     {@code gc-pause} (a burst of short SIGSTOPs, as stop-the-world collections) or {@code
     *     partition} (the worker cut off from the server, still running)
     * @param at when it began: for a kill, when the worker was seen dead
     * @param until when the worker came back: resumed or reconnected; {@link Long#MAX_VALUE} for a kill
     */
    record Fault(String type, long at, long until, String workflowId, String target, String worker,
            String slowStepTaskId, Set<String> features) {}

    /** What became of an execution whose worker had a fault. */
    enum Outcome {
        /** another worker took the execution over and it completed */
        HANDED_OVER,
        /** a GC pause: the execution waited it out and completed */
        RODE_OUT,
        /** paused or cut off while nothing of the execution came due: nothing to hand over */
        NOTHING_DUE,
        /**
         * a continuation waited for the paused or cut-off worker while another fault hit the
         * workers that could have taken it over: neither a handover nor a failure of one
         */
        OVERLAPPED,
        FAILED
    }

    /**
     * @param continuedBy the worker that took the execution over, or why none is named
     * @param delayMs how long the execution waited on the failover: from the fault, or from when a
     *     continuation could run if that was later, to when another worker's applied
     */
    record Handover(Outcome outcome, String why, String continuedBy, Long delayMs) {
        boolean ok() {
            return outcome != Outcome.FAILED;
        }
    }

    /**
     * How long a continuation that came due while its worker was paused or cut off may take to be
     * taken over. The server offers it to the worker the execution is pinned to and puts the same
     * message on the shared queue, visible after the pin timeout (5 s by default); another worker
     * polls it and replays the execution. A stopped or cut-off worker cannot lease anything, so
     * nothing else holds it back.
     */
    static final long TAKEOVER_MS = 20_000;

    /**
     * The same for a continuation already due when the fault began, which the worker may have
     * leased: it waits out its lease (30 s) first, then is offered again.
     */
    static final long LEASED_TAKEOVER_MS = 45_000;

    /** How long a killed worker is missing: the bench restarts it after 20 s, and it boots in 15. */
    static final long KILLED_FOR_MS = 40_000;

    /** What the execution is doing now: the features of its tasks in flight, and of a continuation leased. */
    static Set<String> features(Map<String, Object> wf) {
        Set<String> out = new TreeSet<>();
        Map<String, Set<String>> inside = enclosing(list(map(wf.get("workflowDefinition")).get("tasks")));
        for (Map<String, Object> t : tasks(wf)) {
            if (leased(t.get("continuation"))) {
                out.add("continuation");
            }
            if (!IN_FLIGHT.contains(t.get("status"))) {
                continue;
            }
            String ref = String.valueOf(t.get("referenceTaskName"));
            switch (String.valueOf(t.get("taskType"))) {
                case "slow_step" -> {
                    if ("IN_PROGRESS".equals(t.get("status"))) {
                        out.add("long-task");
                    }
                }
                case "DO_WHILE" -> out.add("server-loop");
                case "HUMAN" -> out.add("human");
                case "SUB_WORKFLOW" -> out.add("child");
                case "WAIT" -> out.add(TIMER.matcher(ref).matches() ? "timer"
                        : ITERATION.matcher(ref).find() ? "server-loop" : "external");
                default -> {
                }
            }
            Map<String, Object> meta = metadata(t);
            for (Object loop : list(meta.get("loops"))) {
                out.add(JAVA_LOOP.matcher(String.valueOf(loop)).matches() ? "java-loop" : "doWhile");
            }
            Object lane = meta.get("lane");
            if (lane != null && !"main".equals(lane) && !"handlers".equals(lane)) {
                out.add("fork");
            }
            out.addAll(inside.getOrDefault(ITERATION.matcher(ref).replaceFirst(""), Set.of()));
        }
        Map<String, Object> state = map(wf.get("nativeState"));
        if (leased(state.get("bootstrap")) || leased(state.get("handlers"))) {
            out.add("continuation");
        }
        return out;
    }

    /** The task id of the slow_step running in the execution; null when none is. */
    static String runningSlowStep(Map<String, Object> wf) {
        for (Map<String, Object> t : tasks(wf)) {
            if ("slow_step".equals(t.get("taskType")) && "IN_PROGRESS".equals(t.get("status"))) {
                return String.valueOf(t.get("taskId"));
            }
        }
        return null;
    }

    /**
     * The worker to kill to fail {@code target} over: the one running the slow_step (from {@code
     * runners}, task id to worker id), or the one the execution is pinned to. Null when there is none.
     */
    static String holder(Map<String, Object> wf, String target, Map<String, String> runners) {
        if ("long-task".equals(target)) {
            String taskId = runningSlowStep(wf);
            return taskId == null ? null : runners.get(taskId);
        }
        return pinnedWorker(map(wf.get("nativeState")).get("pinnedTo"));
    }

    /** The worker id in a pin domain, {@code pin-<workerId>-<random>}; null for anything else. */
    static String pinnedWorker(Object domain) {
        if (!(domain instanceof String d) || !d.startsWith("pin-")) {
            return null;
        }
        int last = d.lastIndexOf('-');
        return last <= 3 ? null : d.substring(4, last);
    }

    /**
     * What became of the execution after a fault on its worker. The execution must have completed
     * in every case.
     *
     * <ul>
     *   <li>kill: a continuation applied 2 s or more after the worker was seen dead, or the
     *       execution pinned to another worker after the kill, shows that a live worker ran the
     *       code, as the dead one cannot. A killed slow_step must have run again on another worker.
     *   <li>pause, partition: every continuation that came due {@link #FAILOVER_BOUND_MS} or more
     *       before the worker came back must have applied before it did, so by another worker. With
     *       none due, there was nothing to hand over.
     *   <li>gc-pause: no handover is expected; the execution rides the pauses out.
     * </ul>
     *
     * @param pins the execution's pinnedTo over time, as the bench sampled it: {@code t} and {@code
     *     pinnedTo}
     * @param all every fault of the run, to tell a continuation that waited for the paused worker
     *     from one that waited because the others were down too
     */
    static Handover handover(Fault fault, Map<String, Object> wf, List<Map<String, Object>> pins,
            Map<String, String> runners, List<Fault> all) {
        String status = String.valueOf(wf.get("status"));
        String movedTo = pinnedElsewhere(fault, pins);
        switch (fault.type()) {
            case "gc-pause" -> {
                return "COMPLETED".equals(status)
                        ? new Handover(Outcome.RODE_OUT, movedTo == null ? "rode it out" : "moved to " + movedTo, movedTo, null)
                        : new Handover(Outcome.FAILED, "ended " + status, null, null);
            }
            case "pause", "partition" -> {
                return withheld(fault, wf, status, movedTo, all);
            }
            default -> {
                return killed(fault, wf, status, movedTo, runners);
            }
        }
    }

    private static Handover killed(Fault kill, Map<String, Object> wf, String status, String movedTo,
            Map<String, String> runners) {
        if (kill.slowStepTaskId() != null) {
            for (Map<String, Object> t : tasks(wf)) {
                if ("slow_step".equals(t.get("taskType")) && !kill.slowStepTaskId().equals(t.get("taskId"))
                        && "COMPLETED".equals(t.get("status")) && num(t.get("startTime")) > kill.at()) {
                    String by = runners.get(String.valueOf(t.get("taskId")));
                    if (by != null && !by.equals(kill.worker())) {
                        return finish(status, by, num(t.get("startTime")) - kill.at());
                    }
                }
            }
            return new Handover(Outcome.FAILED, "slow_step was not run again on another worker", null, null);
        }
        // the first continuation that applied after the kill, and how long it waited from when it
        // could run (a task's end can be recorded after the continuation it woke applied: no less than 0)
        long[] first = null;
        for (long[] m : applied(wf)) {
            if (m[1] >= kill.at() + 2_000 && (first == null || m[1] < first[1])) {
                first = m;
            }
        }
        Long delay = first == null ? null : Math.max(0, first[1] - Math.max(kill.at(), first[0]));
        if (movedTo == null && delay == null) {
            return new Handover(Outcome.FAILED, "no continuation applied after the kill", null, null);
        }
        return finish(status, movedTo != null ? movedTo : "a live worker (it was not pinned again)", delay);
    }

    private static Handover withheld(Fault fault, Map<String, Object> wf, String status, String movedTo,
            List<Fault> all) {
        String how = "pause".equals(fault.type()) ? "paused" : "cut off";
        long worst = -1;
        int due = 0;
        // the continuations that applied: a marker that never did belongs to an attempt that was
        // retried (a timed-out or failed task), whose retry carries the one that counts
        for (long[] m : applied(wf)) {
            if (m[1] < fault.at() + 2_000 || m[0] <= 0) {
                continue; // applied before the fault
            }
            long dueAt = Math.max(m[0], fault.at());
            if (dueAt + (m[0] >= fault.at() ? TAKEOVER_MS : LEASED_TAKEOVER_MS) > fault.until()) {
                continue; // came due too late in the fault to have been taken over by its end
            }
            due++;
            if (m[1] >= fault.until()) {
                long into = (dueAt - fault.at()) / 1000;
                String waited = "a continuation due " + (into == 0 ? "as it began" : into + " s into it") + " waited "
                        + (m[1] - dueAt) / 1000 + " s";
                // another worker hit while it could have been taking the continuation over, or
                // holding a lease on it
                long from = dueAt - LEASED_TAKEOVER_MS;
                long to = m[1];
                List<String> others = all.stream()
                        .filter(f -> f != fault && !f.worker().equals(fault.worker()) && f.at() <= to
                                && (f.until() == Long.MAX_VALUE ? f.at() + KILLED_FOR_MS : f.until()) >= from)
                        .map(f -> f.type() + " of " + f.worker()).distinct().toList();
                if (!others.isEmpty()) {
                    return new Handover(Outcome.OVERLAPPED, waited + ", while the other workers had faults too: "
                            + String.join(", ", others), null, null);
                }
                return new Handover(Outcome.FAILED, waited + ", for the " + how + " worker to come back", null, null);
            }
            worst = Math.max(worst, m[1] - dueAt);
        }
        if (!"COMPLETED".equals(status)) {
            return new Handover(Outcome.FAILED, "ended " + status, movedTo, null);
        }
        if (due == 0) {
            return new Handover(Outcome.NOTHING_DUE, "nothing came due while it was " + how, movedTo, null);
        }
        String by = movedTo != null ? movedTo : "a live worker (it was not pinned again)";
        return new Handover(Outcome.HANDED_OVER, due + " continuations taken over by " + by + " while it was " + how, by, worst);
    }

    /** The first worker other than the faulted one that the execution was pinned to after the fault began. */
    private static String pinnedElsewhere(Fault fault, List<Map<String, Object>> pins) {
        for (Map<String, Object> p : pins) {
            String w = pinnedWorker(p.get("pinnedTo"));
            if (num(p.get("t")) > fault.at() && w != null && !w.equals(fault.worker())) {
                return w;
            }
        }
        return null;
    }

    private static Handover finish(String status, String by, Long delay) {
        if (!"COMPLETED".equals(status)) {
            return new Handover(Outcome.FAILED, "taken over by " + by + ", but ended " + status, by, delay);
        }
        return new Handover(Outcome.HANDED_OVER, "taken over by " + by, by, delay);
    }

    /**
     * Each continuation that applied: when it could run and when it applied. The first from the
     * run's start; each other from the end of the task the code paused on (as {@link Checker#markers}).
     */
    static List<long[]> applied(Map<String, Object> wf) {
        List<long[]> out = new ArrayList<>();
        Map<String, Object> bootstrap = map(map(wf.get("nativeState")).get("bootstrap"));
        if (num(bootstrap.get("endTime")) > 0) {
            out.add(new long[] {num(wf.get("createTime")), num(bootstrap.get("endTime"))});
        }
        for (Map<String, Object> t : tasks(wf)) {
            Map<String, Object> marker = map(t.get("continuation"));
            if (num(marker.get("endTime")) > 0) {
                out.add(new long[] {num(t.get("endTime")), num(marker.get("endTime"))});
            }
        }
        return out;
    }

    /** The executions a kitchensink execution started: its children and the workflows it started by name. */
    static List<String> children(Map<String, Object> wf) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> t : tasks(wf)) {
            Object id = "SUB_WORKFLOW".equals(t.get("taskType")) ? t.get("subWorkflowId")
                    : "START_WORKFLOW".equals(t.get("taskType")) ? map(t.get("outputData")).get("workflowId") : null;
            if (id != null) {
                out.add(id.toString());
            }
        }
        return out;
    }

    /** By reference name, the constructs of the definition a task sits inside. */
    private static Map<String, Set<String>> enclosing(List<Object> tasks) {
        Map<String, Set<String>> out = new HashMap<>();
        walk(tasks, Set.of(), out);
        return out;
    }

    private static void walk(List<Object> tasks, Set<String> around, Map<String, Set<String>> out) {
        for (Object o : tasks) {
            Map<String, Object> t = map(o);
            out.put(String.valueOf(t.get("taskReferenceName")), around);
            Set<String> inSwitch = with(around, "switch");
            map(t.get("decisionCases")).values().forEach(arm -> walk(list(arm), inSwitch, out));
            walk(list(t.get("defaultCase")), inSwitch, out);
            Set<String> inFork = with(around, "fork");
            list(t.get("forkTasks")).forEach(lane -> walk(list(lane), inFork, out));
            walk(list(t.get("loopOver")), with(around, "server-loop"), out);
        }
    }

    private static Set<String> with(Set<String> set, String item) {
        Set<String> out = new HashSet<>(set);
        out.add(item);
        return out;
    }

    private static boolean leased(Object marker) {
        return Boolean.TRUE.equals(map(marker).get("leased"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tasks(Map<String, Object> wf) {
        return (List<Map<String, Object>>) (List<?>) list(wf.get("tasks"));
    }

    private static Map<String, Object> metadata(Map<String, Object> task) {
        return map(map(task.get("workflowTask")).get("metadata"));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    static long num(Object o) {
        return o instanceof Number n ? n.longValue() : 0;
    }
}
