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
package parked;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.conductoross.conductor.sdk.Conductor.forEach;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.random;
import static org.conductoross.conductor.sdk.Conductor.switchOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Code kept in memory between continuations does what a replay of the execution does.
 *
 * <p>Every continuation here is processed twice against the same recorded execution: by a
 * replayer kept since the execution began, whose lanes are parked where they paused and are woken
 * with the task they waited for; and by a new replayer that runs the method from the top, as a
 * host that kept nothing does. Their completions must be the same. The kept one's completion is
 * then applied to the recording, so the next replay also checks that what stayed in memory is
 * what was recorded.
 */
class ParkedLanesTest {

    static final Tasks TASKS = Conductor.stub(Tasks.class);

    public static class Tasks {
        @WorkerTask("pk_step")
        public String step(@InputParam("x") String x) {
            return x;
        }
    }

    /** A switch, a loop with a failure caught, an if, two lanes and a helper, values recorded, a doWhile. */
    public static class Shipping {

        @Variable
        String stage = "new";

        @Variable
        List<String> notes = new ArrayList<>();

        @Workflow(name = "pk_shipping")
        public String run(@InputParam("order") String order) {
            double drawn = random();
            String route = switchOn("route", drawn < 2 ? "air" : "sea")
                    .when("air", () -> TASKS.step("air"))
                    .when("sea", () -> TASKS.step("sea"))
                    .get();
            stage = "routed";
            int reserved = 0;
            for (String sku : List.of("a", "b")) {
                try {
                    TASKS.step("reserve-" + sku);
                    reserved++;
                } catch (TaskFailure e) {
                    notes.add("no " + sku);
                }
            }
            if (reserved > 1) {
                TASKS.step("bulk");
            }
            Future<String> left = async(() -> TASKS.step("L") + label("L2"));
            Future<String> right = async(() -> TASKS.step("R"));
            join(left, right);
            String code = once("code", () -> "c-" + order);
            int[] pass = {0};
            doWhile("poll", () -> {
                TASKS.step("p" + pass[0]);
                pass[0]++;
            }, () -> pass[0] < 2);
            String last = TASKS.step("notify");
            stage = "shipped";
            return route + "|" + reserved + "|" + get(left) + "|" + get(right) + "|" + code + "|" + last
                    + "|" + drawn + "|" + notes;
        }

        // a second frame on the stack at the pause
        private String label(String side) {
            return TASKS.step(side);
        }

        private static String get(Future<String> lane) {
            try {
                return lane.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** A loop the server runs, whose result is read from the loop's tasks. */
    public static class EachItem {
        @Workflow(name = "pk_each")
        public String run(@InputParam("order") String order) {
            String first = TASKS.step("first");
            List<String> all = forEach("each", List.of("a", "b"), TASKS::step);
            return first + all + TASKS.step("last");
        }
    }

    @Test
    void codeKeptInMemoryCompletesEveryContinuationAsAReplayDoes() {
        Run run = new Run(Shipping.class, Map.of("order", "o-1"));

        run.step(null);
        run.complete(run.pending("air"), "AIR");
        run.step(run.done("air"));
        run.complete(run.pending("reserve-a"), "ok");
        run.step(run.done("reserve-a"));
        run.fail(run.pending("reserve-b"), "out of stock");
        run.step(run.done("reserve-b"));

        // two lanes and the join the main lane waits at; the right lane's task ends first
        run.complete(run.pending("R"), "R");
        run.step(run.done("R"));
        run.complete(run.pending("L"), "L");
        run.step(run.done("L"));
        run.complete(run.pending("L2"), "L2");
        run.step(run.done("L2"));
        String joined = run.pendingJoin();
        run.complete(joined, Map.of());
        run.step(joined);

        run.complete(run.pending("p0"), "p");
        run.step(run.done("p0"));
        run.complete(run.pending("p1"), "p");
        run.step(run.done("p1"));
        run.complete(run.pending("notify"), "sent");
        Map<String, Object> last = run.step(run.done("notify"));

        assertEquals(true, last.get("done"));
        @SuppressWarnings("unchecked")
        String result = String.valueOf(((Map<String, Object>) last.get("result")).get("result"));
        // the number drawn in the first continuation is the one the last continuation returns
        Object drawn = ((Map<?, ?>) run.bootstrap.get("recorded")).get("random");
        assertEquals("AIR|1|LL2|R|c-o-1|sent|" + ((Map<?, ?>) drawn).get("value") + "|[no b]", result);
        assertEquals("shipped", run.variables.get("stage"));
        // nothing is waited for any more: there is nothing to keep
        assertFalse(run.kept.hasPausedLanes());
        // 11 continuations: the first was the kept replayer's only replay
        assertEquals(10, run.continuedInMemory);
    }

    @Test
    void aLaneThatWaitedOnALoopTheServerRanContinuesWithTheExecutionReadAgain() {
        Run run = new Run(EachItem.class, Map.of("order", "o-1"));

        run.step(null);
        run.complete(run.pending("first"), "F");
        run.step(run.done("first"));

        // the loop ran its task once per item; the lane reads those tasks, not the loop's own
        String loop = run.pendingOfType("DO_WHILE");
        assertTrue(run.kept.readsTasks("main"));
        run.iteration("each_task__1", "A");
        run.iteration("each_task__2", "B");
        run.complete(loop, Map.of("iteration", 2));
        run.step(loop);
        assertFalse(run.kept.readsTasks("main"));

        run.complete(run.pending("last"), "Z");
        Map<String, Object> last = run.step(run.done("last"));

        assertEquals(Map.of("result", "F[A, B]Z"), last.get("result"));
        assertEquals(1, run.readAgain);
    }

    @Test
    void aReplayerAwaitsOnlyTheContinuationItsLanePausedAt() {
        Run run = new Run(Shipping.class, Map.of("order", "o-1"));
        run.step(null);

        assertTrue(run.kept.awaits("main", 2));
        assertFalse(run.kept.awaits("main", 1)); // already ran: a retry of it must replay
        assertFalse(run.kept.awaits("main", 3));
        assertFalse(run.kept.awaits("fork_1.0", 1)); // no such lane yet

        run.kept.close();
        assertFalse(run.kept.awaits("main", 2)); // its lanes are gone
        assertFalse(run.kept.hasPausedLanes());
    }

    // ------------------------------------------------------------------ the recorded execution

    /**
     * An execution as the server records it, as far as a replay reads it: the definition grown by
     * each fragment, a task for every entry the engine would schedule, and the continuation
     * markers. The test completes the tasks.
     */
    static final class Run {
        private static final ObjectMapper JSON = new ObjectMapper();

        final WorkflowSpec spec;
        final Map<String, Object> input;
        final Map<String, Object> variables = new LinkedHashMap<>();
        final List<Map<String, Object>> definition = new ArrayList<>();
        final List<Map<String, Object>> tasks = new ArrayList<>();
        final Map<String, Object> bootstrap = new LinkedHashMap<>();
        long clock = 1000;
        /** The replayer kept since the first continuation. */
        Replayer kept;
        int continuedInMemory;
        int readAgain;

        Run(Class<?> workflow, Map<String, Object> input) {
            this.spec = WorkflowSpec.of(workflow).get(0);
            this.input = input;
        }

        /**
         * Processes the continuation that follows the task {@code ref} (null: the first one) both
         * ways, checks that the completions agree, and applies the kept replayer's.
         */
        Map<String, Object> step(String ref) {
            Map<String, Object> awaited = ref == null ? null : task(ref);
            String lane = ref == null ? "main" : String.valueOf(metadata(awaited).get("lane"));
            int n = ref == null ? 1 : ((Number) metadata(awaited).get("continuation")).intValue();

            // a host that kept nothing: the method from the top, against the recording
            Map<String, Object> replayed = new Replayer(spec, spec.newInstance(), execution()).process(lane, n).output;

            Replayer.Outcome outcome;
            if (kept == null) {
                kept = new Replayer(spec, spec.newInstance(), execution());
                outcome = kept.begin(lane, n);
            } else {
                assertTrue(kept.awaits(lane, n), "the kept lanes do not wait for " + lane + "#" + n);
                if (kept.readsTasks(lane)) {
                    readAgain++;
                    outcome = kept.proceed(lane, n, execution());
                } else {
                    // all a poll hands out: the task the lane waited for, and the variables
                    outcome = kept.proceed(lane, n, copy(variables), Execution.TaskView.of(
                            (String) awaited.get("taskId"), ref, (String) awaited.get("taskType"),
                            (String) awaited.get("status"), copy(output(awaited)),
                            (String) awaited.get("reasonForIncompletion")));
                }
                continuedInMemory++;
            }
            Map<String, Object> completion = copy(outcome.output);
            assertEquals(comparable(copy(replayed)), comparable(copy(completion)), "continuation " + lane + "#" + n);
            apply(awaited, completion);
            kept.forgetHistory();
            return completion;
        }

        /** The values a continuation records are its own (a number drawn, the time): their names are compared. */
        private static Map<String, Object> comparable(Map<String, Object> completion) {
            Map<String, Object> c = new LinkedHashMap<>(completion);
            c.put("recorded", new ArrayList<>(((Map<?, ?>) completion.get("recorded")).keySet()));
            return c;
        }

        @SuppressWarnings("unchecked")
        private void apply(Map<String, Object> awaited, Map<String, Object> completion) {
            Map<String, Object> marker = awaited == null ? bootstrap : (Map<String, Object>) awaited.get("continuation");
            marker.put("endTime", ++clock);
            marker.put("recorded", completion.get("recorded"));
            marker.put("resumedWith", completion.get("resumedWith"));
            marker.put("done", completion.get("done"));
            variables.putAll((Map<String, Object>) completion.get("variables"));
            List<Map<String, Object>> fragment = (List<Map<String, Object>>) completion.get("fragment");
            definition.addAll(fragment);
            schedule(fragment);
        }

        /** A task for each entry the engine would schedule: not a preview, and of a switch only the case taken. */
        @SuppressWarnings("unchecked")
        private void schedule(List<Map<String, Object>> entries) {
            for (Map<String, Object> entry : entries) {
                Map<String, Object> md = (Map<String, Object>) entry.get("metadata");
                if (Boolean.TRUE.equals(md.get("preview"))) {
                    continue;
                }
                String type = (String) entry.get("type");
                switch (type) {
                    case "FORK_JOIN" -> {
                        add(entry, "COMPLETED");
                        ((List<List<Map<String, Object>>>) entry.get("forkTasks")).forEach(this::schedule);
                    }
                    case "SWITCH" -> {
                        add(entry, "COMPLETED");
                        Object taken = ((Map<String, Object>) entry.get("inputParameters")).get("switchCaseValue");
                        List<Map<String, Object>> list =
                                ((Map<String, List<Map<String, Object>>>) entry.get("decisionCases")).get(taken);
                        schedule(list != null ? list : (List<Map<String, Object>>) entry.get("defaultCase"));
                    }
                    case "JOIN" -> add(entry, Boolean.TRUE.equals(md.get("detached")) ? "COMPLETED" : "IN_PROGRESS");
                    case "TERMINATE" -> { }
                    default -> add(entry, "IN_PROGRESS");
                }
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> add(Map<String, Object> entry, String status) {
            Map<String, Object> md = (Map<String, Object>) entry.get("metadata");
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("taskId", "id-" + entry.get("taskReferenceName"));
            t.put("referenceTaskName", entry.get("taskReferenceName"));
            t.put("taskType", entry.get("type"));
            t.put("status", status);
            t.put("seq", tasks.size() + 1);
            t.put("inputData", entry.get("inputParameters"));
            t.put("outputData", new LinkedHashMap<>());
            t.put("workflowTask", Map.of("type", entry.get("type"), "metadata", md));
            if (md.get("continuation") != null) {
                t.put("continuation", new LinkedHashMap<>()); // the code paused on it: not run yet
            }
            tasks.add(t);
            return t;
        }

        Execution execution() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("workflowId", "wf-1");
            json.put("status", "RUNNING");
            json.put("input", input);
            json.put("variables", variables);
            json.put("workflowDefinition", Map.of("name", spec.name, "tasks", definition));
            json.put("tasks", tasks);
            json.put("nativeState", Map.of("bootstrap", bootstrap));
            return new Execution(copy(json));
        }

        Map<String, Object> task(String ref) {
            return tasks.stream().filter(t -> ref.equals(t.get("referenceTaskName"))).findFirst()
                    .orElseThrow(() -> new AssertionError("no task " + ref + " in " + refs()));
        }

        private List<String> refs() {
            return tasks.stream().map(t -> t.get("referenceTaskName") + "=" + t.get("status")).toList();
        }

        /** The reference of the running task whose input {@code x} is this. */
        String pending(String x) {
            return byInput(x, "IN_PROGRESS");
        }

        /** The reference of the ended task whose input {@code x} is this. */
        String done(String x) {
            return tasks.stream()
                    .filter(t -> !"IN_PROGRESS".equals(t.get("status")) && t.get("inputData") instanceof Map<?, ?> in && x.equals(in.get("x")))
                    .map(t -> (String) t.get("referenceTaskName")).findFirst()
                    .orElseThrow(() -> new AssertionError("no ended task with x=" + x + " in " + refs()));
        }

        private String byInput(String x, String status) {
            return tasks.stream()
                    .filter(t -> status.equals(t.get("status")) && t.get("inputData") instanceof Map<?, ?> in && x.equals(in.get("x")))
                    .map(t -> (String) t.get("referenceTaskName")).findFirst()
                    .orElseThrow(() -> new AssertionError("no " + status + " task with x=" + x + " in " + refs()));
        }

        String pendingJoin() {
            return pendingOfType("JOIN");
        }

        String pendingOfType(String type) {
            return tasks.stream()
                    .filter(t -> "IN_PROGRESS".equals(t.get("status")) && type.equals(t.get("taskType")))
                    .map(t -> (String) t.get("referenceTaskName")).findFirst()
                    .orElseThrow(() -> new AssertionError("no running " + type + " in " + refs()));
        }

        void complete(String ref, String result) {
            complete(ref, Map.of("result", result));
        }

        void complete(String ref, Map<String, Object> output) {
            Map<String, Object> t = task(ref);
            t.put("status", "COMPLETED");
            t.put("outputData", new LinkedHashMap<>(output));
        }

        void fail(String ref, String reason) {
            Map<String, Object> t = task(ref);
            t.put("status", "FAILED");
            t.put("reasonForIncompletion", reason);
        }

        /** A task one pass of a server loop ran. */
        void iteration(String ref, String result) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("taskId", "id-" + ref);
            t.put("referenceTaskName", ref);
            t.put("taskType", "pk_step");
            t.put("status", "COMPLETED");
            t.put("seq", tasks.size() + 1);
            t.put("outputData", Map.of("result", result));
            tasks.add(t);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> metadata(Map<String, Object> task) {
            return (Map<String, Object>) ((Map<String, Object>) task.get("workflowTask")).get("metadata");
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> output(Map<String, Object> task) {
            return (Map<String, Object>) task.get("outputData");
        }

        /** Through JSON, as everything between a worker and the server goes. */
        @SuppressWarnings("unchecked")
        private static Map<String, Object> copy(Map<String, Object> map) {
            try {
                return JSON.readValue(JSON.writeValueAsString(map), LinkedHashMap.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
