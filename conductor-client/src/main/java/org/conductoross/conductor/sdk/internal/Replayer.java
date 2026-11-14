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
package org.conductoross.conductor.sdk.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Functions;
import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.WorkflowFailedException;

/**
 * Runs workflow code against an execution's recorded tasks and produces the next fragment.
 *
 * <p>The worker is stateless: every continuation is processed by re-running the workflow method
 * from the top. Each lane (the workflow method, and each {@code async} block) runs on its own
 * virtual thread, but only one runs at a time and control is handed between them explicitly, so
 * the code observes a single-threaded world.
 *
 * <p>Segments are replayed in the order the live run processed them (the {@code step} the server
 * recorded on each continuation marker), which reproduces reference-name allocation exactly. A
 * call whose reference name is already in the definition returns the recorded result, or pauses
 * the lane if that task has not finished. Before a lane resumes at a continuation, the
 * {@code @Variable} fields are restored to the values recorded when that continuation was first
 * processed, so code between two pauses sees exactly what it saw the first time. The active
 * continuation (the one being processed now) runs against the current variables, and every new
 * call it makes goes into the fragment; the last entry of the fragment carries the marker of the
 * continuation that follows it.
 */
public final class Replayer {

    private enum Mode {
        REPLAY,
        ACTIVE
    }

    private enum State {
        NEW,
        RUNNING,
        PAUSED,
        DONE
    }

    /** Result of processing a continuation. */
    public static final class Outcome {
        public final Map<String, Object> output = new LinkedHashMap<>();
        public final Map<String, BooleanSupplier> pendingConditions = new LinkedHashMap<>();
        public Object instance;
    }

    private final WorkflowSpec spec;
    private final Object instance;
    private final Execution exec;
    private final Set<String> used = new HashSet<>();
    private final Map<String, Lane> lanes = new LinkedHashMap<>();
    private final Participant driver = new Participant();
    private final Map<String, BooleanSupplier> pendingConditions = new LinkedHashMap<>();
    private int groupCounter;
    private Mode mode = Mode.REPLAY;
    private volatile boolean aborting;
    private volatile RuntimeException fatal;
    /** Values recorded by the active continuation, for its marker. */
    private final Map<String, Object> recorded = new LinkedHashMap<>();
    private String activeLaneId;
    private int activeN;
    public Replayer(WorkflowSpec spec, Object instance, Execution exec) {
        this.spec = spec;
        this.instance = instance;
        this.exec = exec;
    }

    /**
     * Replays every applied continuation, then runs the active one (lane and number of the
     * continuation being processed) and returns its completion. With a null active lane only the
     * replay runs, which checks that the code still matches the recording.
     */
    public Outcome process(String activeLane, int activeContinuation) {
        Outcome outcome = new Outcome();
        outcome.instance = instance;
        Lane main = new Lane("main", true, this::invokeWorkflowMethod);
        main.counter = 1;
        main.waitingFor = 1;
        lanes.put(main.id, main);
        try {
            for (Execution.Marker seg : exec.segments()) {
                runSegment(seg.lane, seg.n, Mode.REPLAY, seg.resumedWith);
            }
            assertNothingRecordedWasDropped();
            if (activeLane == null) {
                outcome.pendingConditions.putAll(pendingConditions);
                return outcome;
            }
            Map<String, Object> current = new LinkedHashMap<>(exec.variables);
            this.activeLaneId = activeLane;
            this.activeN = activeContinuation;
            Lane lane = runSegment(activeLane, activeContinuation, Mode.ACTIVE, current);
            Map<String, Object> out = outcome.output;
            out.put("resumedWith", spec.captureVariablesOf(current));
            out.put("recorded", recorded);
            List<Map<String, Object>> fragment = lane.root;
            if (lane.state == State.DONE) {
                out.put("done", true);
                if (lane.error != null) {
                    Map<String, Object> error = errorOf(lane.error);
                    if (lane.thrownRef != null) {
                        error.put("ref", lane.thrownRef);
                    }
                    out.put("error", error);
                } else if (lane.main) {
                    // the workflow's output
                    out.put("result", Json.encodeOutput(lane.result));
                } else {
                    out.put("result", Map.of("value", Json.plain(lane.result)));
                }
            } else {
                out.put("done", false);
            }
            out.put("fragment", fragment);
            out.put("variables", spec.captureVariables(instance));
            outcome.pendingConditions.putAll(pendingConditions);
            return outcome;
        } finally {
            abortAll();
        }
    }

    /**
     * The graph the code describes, read from the compiled workflow method by {@link CodeReader}:
     * every call becomes an entry (switches with all their cases, async blocks as forks, loops as
     * DO_WHILE). No workflow code runs.
     */
    public static List<Map<String, Object>> discover(WorkflowSpec spec) {
        return new Replayer(spec, null, null).new Drawing("main", false, null).draw(CodeReader.read(spec.method));
    }

    /** Turns steps read from the code into definition entries. Nothing here runs workflow code. */
    private final class Drawing {
        private final String lane;
        private final boolean preview;
        /** The case key this drawing is for, composed for nested cases, in preview names. */
        private final String key;
        /** The Java loops around what this drawing draws, outermost first (not for previews). */
        private final List<String> loops;

        Drawing(String lane, boolean preview, String key) {
            this(lane, preview, key, List.of());
        }

        private Drawing(String lane, boolean preview, String key, List<String> loops) {
            this.lane = lane;
            this.preview = preview;
            this.key = key;
            this.loops = loops;
        }

        List<Map<String, Object>> draw(List<CodeReader.Step> steps) {
            List<Map<String, Object>> entries = new ArrayList<>();
            drawInto(steps, entries, new java.util.ArrayDeque<>());
            return entries;
        }

        @SuppressWarnings("unchecked")
        private void drawInto(
                List<CodeReader.Step> steps, List<Map<String, Object>> entries, java.util.Deque<String> running) {
            for (CodeReader.Step step : steps) {
                if (step instanceof CodeReader.InLoops in) {
                    // a preview stays in the loops its switch is in; the definition shows each loop
                    List<String> around = new ArrayList<>(loops);
                    in.loops().stream().filter(l -> !around.contains(l)).forEach(around::add);
                    (preview ? this : new Drawing(lane, false, key, List.copyOf(around)))
                            .drawInto(in.steps(), entries, running);
                    continue;
                }
                int before = entries.size();
                if (step instanceof CodeReader.Call c) {
                    Command cmd = c.command();
                    entries.add(finish(Entries.of(cmd, ref(cmd.explicitRef != null ? cmd.explicitRef : cmd.refBase), lane), c.site()));
                } else if (step instanceof CodeReader.Decision d) {
                    entries.add(decision(d));
                } else if (step instanceof CodeReader.Fork f) {
                    String ref = ref("fork");
                    List<List<Map<String, Object>>> branches = new ArrayList<>();
                    f.branches().forEach(b -> branches.add(draw(b)));
                    Map<String, Object> fork = structural("fork", ref, "FORK_JOIN", f.site());
                    fork.put("forkTasks", branches);
                    entries.add(fork);
                    Map<String, Object> join = join("fork", ref, f.site());
                    if (!f.joined()) {
                        // the engine needs a JOIN after a FORK_JOIN; this one does not wait
                        ((Map<String, Object>) join.get("metadata")).put("detached", true);
                        running.push(ref);
                    }
                    entries.add(join);
                } else if (step instanceof CodeReader.Wait w) {
                    Map<String, Object> wait = structural("fork_wait", ref("fork_wait"), "JOIN", w.site());
                    wait.put("joinOn", List.of());
                    if (!running.isEmpty()) {
                        ((Map<String, Object>) wait.get("metadata")).put("waitForFork", running.pop());
                    }
                    entries.add(wait);
                } else if (step instanceof CodeReader.FanOut f) {
                    String ref = ref(f.name());
                    Map<String, Object> fork = structural(f.name(), ref, "FORK_JOIN_DYNAMIC", f.site());
                    fork.put("dynamicForkTasksParam", "dynamicTasks");
                    fork.put("dynamicForkTasksInputParamName", "dynamicTasksInput");
                    entries.add(fork);
                    entries.add(join(f.name(), ref, f.site()));
                } else if (step instanceof CodeReader.Loop l) {
                    entries.add(loop(l));
                }
                if (!preview && !loops.isEmpty()) {
                    for (Map<String, Object> entry : entries.subList(before, entries.size())) {
                        ((Map<String, Object>) entry.get("metadata")).putIfAbsent("loops", loops);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> decision(CodeReader.Decision d) {
            Map<String, Object> sw = new LinkedHashMap<>();
            String ref = ref(d.name());
            sw.put("name", ref);
            sw.put("taskReferenceName", ref);
            sw.put("type", "SWITCH");
            sw.put("evaluatorType", "native-code"); // the code decided: the server reads the case it took
            sw.put("expression", "switchCaseValue");
            sw.put("inputParameters", Map.of("switchCaseValue", ""));
            Map<String, List<Map<String, Object>>> cases = new LinkedHashMap<>();
            d.cases().forEach((k, body) -> cases.put(k, nested(k).draw(body)));
            sw.put("decisionCases", cases);
            sw.put("defaultCase", nested("default").draw(d.otherwise()));
            sw.put("metadata", Entries.metadata(lane));
            return finish(sw, d.site());
        }

        private Map<String, Object> loop(CodeReader.Loop l) {
            String ref = ref(l.name());
            Map<String, Object> entry = structural(ref, ref, "DO_WHILE", l.site());
            entry.put("loopCondition", "false");
            List<Map<String, Object>> body = new ArrayList<>();
            for (CodeReader.Step step : l.body()) {
                if (step instanceof CodeReader.Call c && l.perItem()) {
                    body.add(finish(Entries.of(c.command(), ref + "_task", lane), c.site()));
                } else {
                    body.addAll(draw(List.of(step)));
                }
            }
            entry.put("loopOver", body);
            return entry;
        }

        private Drawing nested(String caseKey) {
            return new Drawing(lane, preview, key == null ? caseKey : key + "_" + caseKey, loops);
        }

        private Map<String, Object> structural(String name, String ref, String type, String site) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("taskReferenceName", ref);
            entry.put("type", type);
            entry.put("metadata", Entries.metadata(lane));
            return finish(entry, site);
        }

        private Map<String, Object> join(String forkName, String forkRef, String site) {
            Map<String, Object> entry = structural(forkName + "_join", forkRef + "_join", "JOIN", site);
            entry.put("joinOn", List.of());
            return entry;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> finish(Map<String, Object> entry, String site) {
            Map<String, Object> metadata = (Map<String, Object>) entry.get("metadata");
            metadata.put("site", site);
            if (preview) {
                metadata.put("preview", true);
                entry.remove("optional");
            }
            return entry;
        }

        /** The name of an entry: as an execution would allocate it, or marked with its case. */
        private String ref(String base) {
            if (!preview) {
                return alloc(base);
            }
            String stem = base + "_if_" + (key == null ? "preview" : key.replaceAll("[^A-Za-z0-9]", "_"));
            String ref = stem;
            // previews are drawn only when a switch is first emitted, so a name an earlier
            // fragment took is not in used; the recording has it
            for (int n = 2; used.contains(ref) || (exec != null && exec.entries.containsKey(ref)); n++) {
                ref = stem + "_" + n;
            }
            used.add(ref);
            return ref;
        }
    }

    private Lane runSegment(String laneId, int continuation, Mode m, Map<String, Object> vars) {
        Lane lane = lanes.get(laneId);
        if (lane == null) {
            throw new NonDeterminismException("the recording continues lane " + laneId + ", which the code never started");
        }
        if (lane.state == State.DONE) {
            throw new NonDeterminismException("the recording continues lane " + laneId + ", which already returned");
        }
        if (lane.waitingFor != continuation) {
            throw new NonDeterminismException(
                    "lane " + laneId + " paused at continuation " + lane.waitingFor + " but the recording resumes continuation " + continuation);
        }
        mode = m;
        spec.restoreVariables(instance, vars);
        if (m == Mode.ACTIVE) {
            lane.root = new ArrayList<>();
            lane.out = lane.root;
        }
        resume(lane);
        return lane;
    }

    private Object invokeWorkflowMethod() {
        Object[] args = WorkflowSpec.decodeInput(spec.method, exec.input);
        try {
            return spec.method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new WorkflowFailedException(cause.toString());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ control transfer

    private static class Participant {
        final Semaphore sem = new Semaphore(0);
    }

    private static final class LaneAbort extends Error {
        LaneAbort() {
            super(null, null, false, false);
        }
    }

    private static final class Capture extends RuntimeException {
        final Command command;

        Capture(Command command) {
            super(null, null, false, false);
            this.command = command;
        }
    }

    private void transfer(Participant from, Participant to, boolean fromWaits) {
        to.sem.release();
        if (fromWaits) {
            from.sem.acquireUninterruptibly();
            if (aborting && from instanceof Lane) {
                throw new LaneAbort();
            }
        }
    }

    private void resume(Lane lane) {
        lane.returnTo = driver;
        if (lane.state == State.NEW) {
            lane.start();
        }
        transfer(driver, lane, true);
        if (fatal != null) {
            throw fatal;
        }
    }

    private void pause(Lane lane) {
        lane.counter++;
        lane.waitingFor = lane.counter;
        lane.state = State.PAUSED;
        transfer(lane, lane.returnTo, true);
        lane.state = State.RUNNING;
    }

    /** The name of the first call recorded for a lane that this replay has not reached, or null. */
    private String nextRecordedCall(String laneId) {
        for (Map.Entry<String, Map<String, Object>> entry : exec.entries.entrySet()) {
            if (used.contains(entry.getKey())
                    || !(entry.getValue().get("metadata") instanceof Map<?, ?> metadata)
                    || !laneId.equals(metadata.get("lane"))
                    || Boolean.TRUE.equals(metadata.get("preview"))) {
                continue;
            }
            return String.valueOf(entry.getValue().get("name"));
        }
        return null;
    }

    /** After replay every recorded step must have been reached: the code may not have dropped one. */
    @SuppressWarnings("unchecked")
    private void assertNothingRecordedWasDropped() {
        Set<String> inLoops = new HashSet<>();
        for (Map<String, Object> entry : exec.entries.values()) {
            if ("DO_WHILE".equals(entry.get("type")) && entry.get("loopOver") instanceof List<?> body) {
                addRefs((List<Map<String, Object>>) body, inLoops);
            }
        }
        for (Map.Entry<String, Map<String, Object>> entry : exec.entries.entrySet()) {
            if (!used.contains(entry.getKey())
                    && !inLoops.contains(entry.getKey())
                    && entry.getValue().get("metadata") instanceof Map<?, ?> metadata
                    && metadata.get("lane") != null
                    && !Boolean.TRUE.equals(metadata.get("preview"))) {
                throw diverged("the recorded execution has " + entry.getKey() + " but the code no longer reaches it");
            }
        }
    }

    /** Adds the reference names of the entries at any depth. */
    @SuppressWarnings("unchecked")
    private static void addRefs(List<Map<String, Object>> entries, Set<String> refs) {
        for (Map<String, Object> e : entries) {
            refs.add((String) e.get("taskReferenceName"));
            if (e.get("forkTasks") instanceof List<?> branches) {
                branches.forEach(b -> addRefs((List<Map<String, Object>>) b, refs));
            }
            if (e.get("decisionCases") instanceof Map<?, ?> cases) {
                cases.values().forEach(c -> addRefs((List<Map<String, Object>>) c, refs));
            }
            if (e.get("defaultCase") instanceof List<?> d) {
                addRefs((List<Map<String, Object>>) d, refs);
            }
            if (e.get("loopOver") instanceof List<?> l) {
                addRefs((List<Map<String, Object>>) l, refs);
            }
        }
    }

    private RuntimeException diverged(String message) {
        NonDeterminismException e = new NonDeterminismException(message);
        fatal = e;
        return e;
    }

    private void abortAll() {
        aborting = true;
        for (Lane lane : lanes.values()) {
            if (lane.thread != null && lane.thread.isAlive()) {
                lane.sem.release();
            }
        }
        for (Lane lane : lanes.values()) {
            if (lane.thread != null) {
                try {
                    lane.thread.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ------------------------------------------------------------------ allocation

    private String alloc(String base) {
        String ref = base;
        for (int n = 2; used.contains(ref); n++) {
            ref = base + "_" + n;
        }
        used.add(ref);
        return ref;
    }


    /** The error a lane ended with; for the main lane its message is the workflow's failure reason. */
    private static Map<String, Object> errorOf(Throwable t) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put(
                "message",
                t instanceof WorkflowFailedException || t instanceof TaskFailure
                        ? t.getMessage()
                        : t.getClass().getSimpleName() + ": " + t.getMessage());
        if (t instanceof TaskFailure tf) {
            e.put("ref", tf.getTaskReferenceName());
        }
        return e;
    }

    // ------------------------------------------------------------------ lanes

    private final class Group {
        final int index;
        final String forkName;
        final List<Lane> members = new ArrayList<>();
        /** The fork's reference, once it has been emitted. */
        String forkRef;
        /** The Java loops the group was started in: it may be emitted after they end. */
        List<String> loops = List.of();

        Group(int index, String forkName) {
            this.index = index;
            this.forkName = forkName;
        }
    }

    private boolean allDone(Group g) {
        return g.members.stream().allMatch(l -> l.state == State.DONE);
    }

    /**
     * Runs before anything durable the code does next, and when a lane returns. Lanes that finished
     * without pausing go inline. Lanes still running are forked with a JOIN that does not wait, so
     * they run on their own; a later join or get waits for them with a JOIN of its own.
     */
    @SuppressWarnings("unchecked")
    private void flushSyncGroup(Lane lane) {
        Group g = lane.pendingGroup;
        if (g == null) {
            return;
        }
        lane.pendingGroup = null;
        if (allDone(g)) {
            if (mode == Mode.ACTIVE) {
                for (Lane l : g.members) {
                    for (Map<String, Object> e : l.root) {
                        lane.keepLoops(e, g.loops);
                        lane.emit(e);
                    }
                }
            }
            return;
        }
        String forkRef = alloc(g.forkName != null ? g.forkName : "fork");
        g.forkRef = forkRef;
        String joinRef = alloc(forkRef + "_join");
        if (mode == Mode.ACTIVE) {
            lane.emitFork(g, forkRef);
            Map<String, Object> join = lane.joinEntry(joinRef);
            ((Map<String, Object>) join.get("metadata")).put("detached", true);
            lane.tagLoops(join, g.loops);
            lane.emit(join);
        } else if (!exec.entries.containsKey(forkRef)) {
            throw lane.divergedAt("the code forks as " + forkRef + " but the recorded execution has no such fork");
        }
    }

    /** The lanes a wait on this one needs: itself, or what it left running if it has returned. */
    private void unfinished(Lane lane, List<Lane> out) {
        if (lane.state != State.DONE) {
            if (!out.contains(lane)) {
                out.add(lane);
            }
            return;
        }
        for (Group g : lane.groups) {
            for (Lane member : g.members) {
                unfinished(member, out);
            }
        }
    }

    /** An arm of a plain Java if or switch a lane is in, and how it was drawn. */
    private static final class Inside {
        final int depth;
        final CodeReader.Branch branch;
        final CodeReader.Arm arm;
        String ref;
        boolean drawn;
        List<Map<String, Object>> outer;
        List<Map<String, Object>> caseList;
        int pausesBefore;
        int lastLine;

        Inside(int depth, CodeReader.Branch branch, CodeReader.Arm arm) {
            this.depth = depth;
            this.branch = branch;
            this.arm = arm;
        }
    }

    /** A frame of workflow code: its method and the line it calls from. */
    private record Site(Class<?> type, String method, String descriptor, int line) {
        boolean sameMethod(Site other) {
            return type == other.type && method.equals(other.method) && descriptor.equals(other.descriptor);
        }

        CodeReader.Shape shape() {
            return CodeReader.shape(type, method, descriptor);
        }
    }

    private static final StackWalker FRAMES = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /** The workflow-code line an exception was thrown from, as {@code Class.method:line}. */
    private static String siteOf(Throwable t) {
        for (StackTraceElement f : t.getStackTrace()) {
            String c = f.getClassName();
            if (!c.startsWith("org.conductoross.conductor.sdk.") && !c.startsWith("java.") && !c.startsWith("jdk.")
                    && !c.startsWith("sun.") && !TaskStubs.isStubClass(c) && f.getLineNumber() > 0) {
                return c.substring(c.lastIndexOf('.') + 1) + "." + f.getMethodName() + ":" + f.getLineNumber();
            }
        }
        return null;
    }

    /** The frames of workflow code making the current call, outermost first. */
    private static List<Site> sites() {
        List<Site> sites = new ArrayList<>();
        FRAMES.forEach(f -> {
            String c = f.getClassName();
            if (c.startsWith("org.conductoross.conductor.sdk.") || c.startsWith("java.") || c.startsWith("jdk.")
                    || c.startsWith("sun.") || TaskStubs.isStubClass(c) || f.getLineNumber() < 0) {
                return;
            }
            sites.add(0, new Site(f.getDeclaringClass(), f.getMethodName(), f.getDescriptor(), f.getLineNumber()));
        });
        return sites;
    }

    private final class Lane extends Participant implements Engine {
        /** A finally block running while the lane is torn down at a pause must not make calls. */
        @Override
        public void assertRunning() {
            if (aborting) {
                throw new LaneAbort();
            }
        }

        final String id;
        final boolean main;
        final Supplier<?> body;
        Thread thread;
        State state = State.NEW;
        int counter;
        int waitingFor;
        Object result;
        Throwable error;
        Participant returnTo;
        /** The fragment this continuation produces (what is sent to the server). */
        List<Map<String, Object>> root = new ArrayList<>();
        /** Where new entries go: the fragment, or the case list of a switch the lane is inside. */
        List<Map<String, Object>> out = root;
        /** Set when the lane left a switch case: the next entry goes after the switch itself. */
        String anchorAfter;
        /** The entry that marks where the main lane's code threw, when it did. */
        String thrownRef;
        /** Branches the lane is in an arm of, but cannot tell whether the code decided them again since. */
        final java.util.Set<CodeReader.Branch> unsure = new java.util.HashSet<>();

        /** The plain Java ifs and switches the lane's code is inside, outermost first. */
        final List<Inside> inside = new ArrayList<>();
        /** The workflow frames of the lane's last call, outermost first. */
        List<Site> lastSites = List.of();
        Group pendingGroup;
        /** Every group this lane started, for joins that wait on what it left running. */
        final List<Group> groups = new ArrayList<>();
        /** The group this lane belongs to. */
        Group group;
        boolean capture;
        final LanePromise<Object> promise = new LanePromise<>(this);

        Lane(String id, boolean main, Supplier<?> body) {
            this.id = id;
            this.main = main;
            this.body = body;
        }

        void start() {
            // A throwaway replay runs code that may never yield, on a thread that can be abandoned
            // without pinning a carrier of the virtual-thread pool.
            Thread.Builder builder = Thread.ofVirtual();
            thread = builder.name("conductor-lane-" + id).unstarted(this::run);
            thread.start();
        }

        /** The Java loops around the code that started this lane, which its stack cannot show. */
        List<String> startedInLoops = List.of();

        private List<String> loopsHere() {
            List<String> all = new ArrayList<>(startedInLoops);
            all.addAll(CodeLoops.here());
            return all;
        }

        /** An entry emitted later, from elsewhere, stays in the loops it was started in. */
        @SuppressWarnings("unchecked")
        private void keepLoops(Map<String, Object> entry, List<String> loops) {
            if (!((Map<String, Object>) entry.computeIfAbsent("metadata", k -> new LinkedHashMap<>()))
                    .containsKey("loops")) {
                tagLoops(entry, loops);
            }
        }

        /** Marks an entry as inside these Java loops, for the execution diagram. */
        @SuppressWarnings("unchecked")
        private void tagLoops(Map<String, Object> entry, List<String> loops) {
            Map<String, Object> metadata =
                    (Map<String, Object>) entry.computeIfAbsent("metadata", k -> new LinkedHashMap<>());
            metadata.put("loops", loops);
        }

        /** Adds an entry to the fragment, anchored after a switch the lane has just left. */
        @SuppressWarnings("unchecked")
        private Map<String, Object> emit(Map<String, Object> entry) {
            if (!((Map<String, Object>) entry.computeIfAbsent("metadata", k -> new LinkedHashMap<>()))
                    .containsKey("loops")) {
                List<String> around = loopsHere();
                if (!around.isEmpty()) {
                    tagLoops(entry, around);
                }
            }
            if (anchorAfter != null) {
                ((Map<String, Object>) entry.computeIfAbsent("metadata", k -> new LinkedHashMap<>()))
                        .put("after", anchorAfter);
                anchorAfter = null;
            }
            out.add(entry);
            return entry;
        }

        private void run() {
            sem.acquireUninterruptibly();
            if (aborting) {
                return;
            }
            Engine.CURRENT.set(this);
            state = State.RUNNING;
            try {
                result = body.get();
                ended();
            } catch (LaneAbort abort) {
                return;
            } catch (Throwable t) {
                error = t;
            }
            try {
                flushSyncGroup(this); // lanes it never joined still run
            } catch (LaneAbort abort) {
                return;
            } catch (Throwable t) {
                if (error == null) {
                    error = t;
                }
            }
            if (error != null && main && fatal == null
                    && !(error instanceof TaskFailure) && !(error instanceof NonDeterminismException)) {
                // the ifs and switches the code threw inside, as at a call; the mark's reference
                // is claimed on every run, so a replay of the failed execution finds its entries
                List<Site> at = sitesOf(error);
                try {
                    if (!at.isEmpty()) {
                        follow(at);
                    }
                    thrownRef = alloc("throw");
                    if (mode == Mode.ACTIVE && !exec.entries.containsKey(thrownRef)) {
                        emitThrown(error, thrownRef, at);
                    }
                } catch (RuntimeException e) {
                    error.addSuppressed(e); // drawing never hides the exception itself
                }
            }
            state = State.DONE;
            transfer(this, returnTo, false);
        }

        /**
         * Marks where the code threw: a TERMINATE entry named after the exception, at the line it
         * was thrown from, where the next call would have gone (inside the switch case the code
         * was in, for one). The server makes it the workflow's failed task, so the execution shows
         * where it failed; it is never scheduled.
         */
        private void emitThrown(Throwable t, String ref, List<Site> at) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", t.getClass().getSimpleName());
            entry.put("taskReferenceName", ref);
            entry.put("type", "TERMINATE");
            entry.put("inputParameters", Map.of(
                    "terminationStatus", "FAILED",
                    "terminationReason", Entries.escape(String.valueOf(errorOf(t).get("message")))));
            Map<String, Object> metadata = Entries.metadata(id);
            String site = siteOf(t);
            if (site != null) {
                metadata.put("site", site);
            }
            entry.put("metadata", metadata);
            emit(entry);
            // what the code would have called next, had it not thrown: previews, never scheduled.
            // The rest of each arm it threw inside goes in that arm's case, what follows after it.
            for (int d = at.size() - 1; d >= 0; d--) {
                Site frame = at.get(d);
                List<List<CodeReader.Step>> segments =
                        CodeReader.stepsAfter(frame.type(), frame.method(), frame.descriptor(), frame.line());
                for (int k = 0; k < segments.size(); k++) {
                    for (Map<String, Object> preview : new Drawing(id, true, "no_error").draw(segments.get(k))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> md = (Map<String, Object>) preview.get("metadata");
                        md.put("previewAfter", ref);
                        emit(preview);
                    }
                    if (k < segments.size() - 1) {
                        leaveInnermost(d);
                    }
                }
            }
        }

        /** Leaves the innermost if or switch the lane is inside in the frame at depth {@code d}. */
        private void leaveInnermost(int d) {
            for (int i = inside.size() - 1; i >= 0; i--) {
                if (inside.get(i).depth == d) {
                    while (inside.size() > i) {
                        leave(inside.remove(inside.size() - 1));
                    }
                    return;
                }
            }
        }

        /**
         * The workflow frames an exception was thrown from, outermost first, as the frames of a
         * call: empty when it did not come from this lane's workflow method (a lane's failure
         * raised at a join keeps that lane's stack) or a frame's method is overloaded.
         */
        private List<Site> sitesOf(Throwable t) {
            List<Site> at = new ArrayList<>();
            for (StackTraceElement f : t.getStackTrace()) {
                String c = f.getClassName();
                if (c.startsWith("org.conductoross.conductor.sdk.") || c.startsWith("java.") || c.startsWith("jdk.")
                        || c.startsWith("sun.") || TaskStubs.isStubClass(c) || f.getLineNumber() <= 0) {
                    continue;
                }
                Method m = uniqueMethod(c, f.getMethodName());
                if (m == null) {
                    return List.of();
                }
                at.add(0, new Site(m.getDeclaringClass(), m.getName(),
                        org.objectweb.asm.Type.getMethodDescriptor(m), f.getLineNumber()));
                if (m.equals(spec.method)) {
                    return at;
                }
            }
            return List.of();
        }

        private Method uniqueMethod(String className, String name) {
            try {
                Class<?> type = Class.forName(className, false, spec.type.getClassLoader());
                Method found = null;
                for (Method m : type.getDeclaredMethods()) {
                    if (m.getName().equals(name)) {
                        if (found != null) {
                            return null;
                        }
                        found = m;
                    }
                }
                return found;
            } catch (ClassNotFoundException | LinkageError e) {
                return null;
            }
        }

        // ---------------------------------------------------------------- Engine

        @Override
        public String workflowId() {
            return exec.workflowId;
        }

        @Override
        public Object call(Command cmd) {
            if (capture) {
                throw new Capture(cmd);
            }
            flushSyncGroup(this);
            follow();
            // a name given in the code is a base like a task's: a call made again, in a loop or
            // written out again, is numbered by how many came before (x, x_2, x_3), never by the
            // shape of the bytecode
            String ref = alloc(cmd.explicitRef != null ? cmd.explicitRef : cmd.refBase);
            if (exec.entries.containsKey(ref)) {
                if (!cmd.blocking()) {
                    return null;
                }
                // Every blocking call was a pause in the live run, so it is a pause in the replay
                // too, even when the result is already recorded: the lane continues only when the
                // continuation that followed this call is replayed.
                pause(this);
                Execution.TaskView t = exec.latest(ref);
                if (t == null || !t.terminal()) {
                    throw diverged("lane " + id + " resumed before task " + ref + " finished");
                }
                return decode(cmd, ref, t);
            }
            if (mode != Mode.ACTIVE) {
                String recorded = nextRecordedCall(id);
                throw diverged(
                        "The running code differs from what this execution recorded: at " + Entries.site()
                                + " it calls " + cmd.name + ", but the recording has "
                                + (recorded == null ? "no further call" : recorded)
                                + " there. Deploy a compatible build to continue.");
            }
            Map<String, Object> entry = emit(Entries.of(cmd, ref, id));
            if (!cmd.blocking()) {
                return null;
            }
            Entries.continuation(entry, id, counter + 1);
            pause(this);
            throw new IllegalStateException("a lane resumed within the continuation that paused it");
        }

        @Override
        public void fire(Command cmd) {
            call(cmd);
        }

        /**
         * A value computed once: the live run computes it and the continuation's marker records
         * it ({@code recorded}); every replay reads it back.
         */
        @SuppressWarnings("unchecked")
        private Object recordValue(String ref, Supplier<?> supplier) {
            Object known = exec.recorded.containsKey(ref) ? exec.recorded.get(ref) : recorded.get(ref);
            if (known instanceof Map<?, ?> record) {
                Object v = record.get("value");
                Object type = record.get("type");
                if (v == null || type == null) {
                    return v;
                }
                try {
                    return Json.convert(v, Class.forName(type.toString(), false, spec.type.getClassLoader()));
                } catch (ClassNotFoundException e) {
                    return v;
                }
            }
            if (mode != Mode.ACTIVE) {
                throw diverged("the code records " + ref + " (" + Entries.site() + ") but the recorded execution has no such value");
            }
            Object v = supplier.get();
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("value", Json.plain(v));
            if (v != null) {
                record.put("type", v.getClass().getName());
            }
            recorded.put(ref, record);
            return v;
        }

        @Override
        public Object once(String name, Supplier<?> supplier) {
            return recordValue(alloc(name), supplier);
        }

        @Override
        public Instant now() {
            return (Instant) recordValue(alloc("now"), Instant::now);
        }

        @Override
        public double random() {
            return ((Number) recordValue(alloc("random"), () -> ThreadLocalRandom.current().nextDouble())).doubleValue();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Promise<T> async(Supplier<T> asyncBody) {
            follow();
            if (pendingGroup == null) {
                pendingGroup = new Group(++groupCounter, null);
                pendingGroup.loops = loopsHere();
            }
            return (Promise<T>) spawn(pendingGroup, asyncBody).promise;
        }

        private Lane spawn(Group g, Supplier<?> laneBody) {
            Lane child = new Lane("fork_" + g.index + "." + g.members.size(), false, laneBody);
            child.startedInLoops = loopsHere();
            g.members.add(child);
            child.group = g;
            if (!groups.contains(g)) {
                groups.add(g);
            }
            lanes.put(child.id, child);
            child.returnTo = this;
            child.start();
            transfer(this, child, true);
            if (fatal != null) {
                throw fatal;
            }
            return child;
        }

        /**
         * Waits for the lanes, then raises the first failure among the joined promises, in the
         * order given, so a lane's uncaught failure fails the caller even without a get().
         */
        @Override
        public void join(List<? extends Promise<?>> promises) {
            follow();
            joinGroup(promises);
            for (Promise<?> p : promises) {
                p.get();
            }
        }

        /**
         * Waits for the promised lanes, or for the whole pending group when none are named. Lanes
         * not forked yet are forked here with a JOIN on the ones wanted; lanes already running get
         * a JOIN of their own, which names them. A lane that finished waits for nothing.
         */
        @SuppressWarnings("unchecked")
        private void joinGroup(List<? extends Promise<?>> promises) {
            Group g = pendingGroup;
            if (g != null && allDone(g)) {
                flushSyncGroup(this);
                g = null;
            }
            List<Lane> wanted = new ArrayList<>();
            if (promises.isEmpty() && g != null) {
                g.members.forEach(l -> unfinished(l, wanted));
            }
            for (Promise<?> p : promises) {
                if (p instanceof LanePromise<?> lp) {
                    unfinished(lp.lane, wanted);
                }
            }
            if (wanted.isEmpty()) {
                return;
            }
            if (g != null && wanted.stream().noneMatch(g.members::contains)) {
                flushSyncGroup(this);
                g = null;
            }
            String joinRef;
            boolean whole = false;
            if (g != null) {
                String forkRef = alloc(g.forkName != null ? g.forkName : "fork");
                g.forkRef = forkRef;
                joinRef = alloc(forkRef + "_join");
                List<Lane> running = new ArrayList<>();
                g.members.forEach(l -> unfinished(l, running));
                whole = running.containsAll(wanted) && wanted.containsAll(running);
                if (mode == Mode.ACTIVE) {
                    emitFork(g, forkRef);
                } else if (!exec.entries.containsKey(forkRef)) {
                    throw diverged("the code forks as " + forkRef + " but the recorded execution has no such fork");
                }
                pendingGroup = null;
            } else {
                joinRef = alloc((wanted.get(0).group.forkRef != null ? wanted.get(0).group.forkRef : "fork") + "_wait");
            }
            if (mode == Mode.ACTIVE) {
                Map<String, Object> join = joinEntry(joinRef);
                if (!whole) {
                    ((Map<String, Object>) join.get("metadata")).put("waitFor", wanted.stream().map(l -> l.id).toList());
                }
                emit(Entries.continuation(join, id, counter + 1));
            } else if (!exec.entries.containsKey(joinRef)) {
                throw diverged("the code waits at " + joinRef + " but the recorded execution has no such join");
            }
            pause(this);
            for (Lane l : wanted) {
                if (l.state != State.DONE) {
                    throw diverged("lane " + id + " resumed after " + joinRef + " but a joined lane has not finished");
                }
            }
        }

        private void emitFork(Group g, String forkRef) {
            List<List<Map<String, Object>>> branches = new ArrayList<>();
            for (Lane l : g.members) {
                if (l.state == State.DONE) {
                    for (Map<String, Object> e : l.root) {
                        keepLoops(e, g.loops);
                        emit(e);
                    }
                } else {
                    branches.add(l.root);
                }
            }
            Map<String, Object> fork = new LinkedHashMap<>();
            fork.put("name", forkRef);
            fork.put("taskReferenceName", forkRef);
            fork.put("type", "FORK_JOIN");
            fork.put("forkTasks", branches);
            fork.put("metadata", Entries.metadata(id));
            tagLoops(fork, g.loops);
            emit(fork);
        }

        private Map<String, Object> joinEntry(String joinRef) {
            Map<String, Object> join = new LinkedHashMap<>();
            join.put("name", joinRef);
            join.put("taskReferenceName", joinRef);
            join.put("type", "JOIN");
            join.put("joinOn", List.of());
            join.put("metadata", Entries.metadata(id));
            return join;
        }

        RuntimeException divergedAt(String message) {
            return diverged(message);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I, R> List<R> fanOut(String name, List<I> items, Function<I, R> fn) {
            flushSyncGroup(this);
            follow();
            Group g = new Group(++groupCounter, name);
            g.loops = loopsHere();
            pendingGroup = g;
            for (I item : items) {
                spawn(g, () -> fn.apply(item));
            }
            joinGroup(List.of());
            List<R> results = new ArrayList<>();
            for (Lane l : g.members) {
                results.add((R) l.promise.get());
            }
            return results;
        }

        @Override
        public boolean awaitCondition(BooleanSupplier condition, Duration timeout) {
            flushSyncGroup(this);
            follow();
            if (condition.getAsBoolean()) {
                return true;
            }
            String ref = alloc("condition");
            if (exec.entries.containsKey(ref)) {
                Execution.TaskView t = exec.latest(ref);
                if (t == null || !t.terminal()) {
                    pendingConditions.put(ref, condition);
                }
                pause(this);
                pendingConditions.remove(ref);
                return condition.getAsBoolean();
            }
            if (mode != Mode.ACTIVE) {
                throw diverged("the code waits for a condition as " + ref + " but the recorded execution has no such wait");
            }
            Command wait = new Command(Command.Kind.WAIT, ref, ref);
            wait.input.put("duration", Conductors.duration(timeout));
            emit(Entries.continuation(Entries.of(wait, ref, id), id, counter + 1));
            pause(this);
            throw new IllegalStateException("a lane resumed within the continuation that paused it");
        }

        /**
         * A SWITCH task with every declared case, and the taken case's body run inline: its
         * calls go inside that case (in this fragment, or after the task the code paused on
         * there); the first call after the body goes after the switch itself.
         */
        @Override
        @SuppressWarnings("unchecked")
        public Object switchOn(String name, Object value, Map<String, Supplier<?>> cases, Supplier<?> otherwise) {
            flushSyncGroup(this);
            follow();
            String ref = alloc(name);
            Object plain = Json.plain(value);
            String key = plain == null ? "null" : String.valueOf(plain);
            Supplier<?> body = cases.containsKey(key) ? cases.get(key) : otherwise;
            if (body == null) {
                if (mode == Mode.ACTIVE && !exec.entries.containsKey(ref)) {
                    // the decision is still drawn, with what every case would call
                    Map<String, Object> sw = switchEntry(ref, key, cases.keySet());
                    drawUntaken(sw, key, cases, null);
                    emit(sw);
                }
                throw new WorkflowFailedException("switch " + name + " has no case for " + key);
            }
            Map<String, Object> entry = exec.entries.get(ref);
            List<Map<String, Object>> outer = out;
            List<Map<String, Object>> caseList = null;
            int pausesBefore = counter;
            if (entry != null) {
                Map<String, Object> in = (Map<String, Object>) entry.get("inputParameters");
                Object recorded = in == null ? null : in.get("switchCaseValue");
                if (!Entries.escape(key).equals(String.valueOf(recorded))) {
                    throw diverged("switch " + ref + " took case " + recorded + " in the recorded execution but the code now takes " + key);
                }
            } else {
                if (mode != Mode.ACTIVE) {
                    throw diverged("the code switches as " + ref + " (" + Entries.site() + ") but the recorded execution has no such switch");
                }
                Map<String, Object> sw = switchEntry(ref, key, cases.keySet());
                Map<String, List<Map<String, Object>>> decisionCases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
                List<Map<String, Object>> defaultCase = (List<Map<String, Object>>) sw.get("defaultCase");
                emit(sw);
                drawUntaken(sw, key, cases, otherwise);
                caseList = cases.containsKey(key) ? decisionCases.get(key) : defaultCase;
                out = caseList;
            
            }
            int insideBefore = inside.size();
            try {
                return body.get();
            } finally {
                while (inside.size() > insideBefore) {
                    leave(inside.remove(inside.size() - 1)); // ifs and switches in the body end with it
                }
                if (caseList != null && out == caseList) {
                    out = outer; // the body made no pause: back to the list that holds the switch
                } else if (mode == Mode.ACTIVE && counter != pausesBefore) {
                    // this continuation resumed inside the case (the task the code paused on is in
                    // there) and the body has now returned: the next entry goes after the switch
                    anchorAfter = ref;
                }
            }
        }

        /**
         * Fills the untaken cases with what each would have called, read from the compiled code:
         * entries the engine never schedules. Nothing runs.
         */
        @SuppressWarnings("unchecked")
        private void drawUntaken(Map<String, Object> sw, String key, Map<String, Supplier<?>> cases, Supplier<?> otherwise) {
            Map<String, List<Map<String, Object>>> decisionCases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
            for (Map.Entry<String, Supplier<?>> c : cases.entrySet()) {
                if (!c.getKey().equals(key)) {
                    decisionCases.get(c.getKey()).addAll(previewOf(c.getValue(), c.getKey()));
                }
            }
            if (otherwise != null && cases.containsKey(key)) {
                ((List<Map<String, Object>>) sw.get("defaultCase")).addAll(previewOf(otherwise, "default"));
            }
        }

        /** What an untaken case would call, read from its compiled body. */
        private List<Map<String, Object>> previewOf(Supplier<?> body, String caseKey) {
            try {
                java.io.Serializable source = body instanceof ActionCase<?> action ? action.action() : (java.io.Serializable) body;
                return new Drawing(id, true, caseKey).draw(CodeReader.read(source));
            } catch (RuntimeException e) {
                return new ArrayList<>(); // a case that cannot be read is drawn empty; the run goes on
            }
        }

        /** What an arm not taken would call. */
        private List<Map<String, Object>> previewOf(List<CodeReader.Step> steps, String caseKey) {
            try {
                return new Drawing(id, true, caseKey).draw(steps);
            } catch (RuntimeException e) {
                return new ArrayList<>();
            }
        }

        // ------------------------------------------------------------ plain ifs and switches

        /**
         * Keeps the plain Java ifs and switches the code is inside in step with where it calls from.
         * The compiled code says which lines each arm calls from, so the frames of a call say
         * which arms it is in: leaving an arm ends its case; entering one draws its if or switch
         * as a SWITCH with every arm, the others shown as previews, and the calls that follow go
         * in that case, as in a {@code switchOn}. An if or switch the code went past without a
         * call is drawn too, with the arm taken, when only one arm can be passed without a call.
         */
        private void follow() {
            if (capture) {
                return;
            }
            follow(sites());
        }

        private void follow(List<Site> sites) {
            int same = 0;
            while (same < sites.size() && same < lastSites.size() && sites.get(same).sameMethod(lastSites.get(same))) {
                same++;
                if (sites.get(same - 1).line() != lastSites.get(same - 1).line()) {
                    break; // a different call from this frame: the frames below it are new
                }
                if (same < sites.size() && same < lastSites.size() && sites.get(same).sameMethod(lastSites.get(same))
                        && !sites.get(same).shape().paths().reachable(lastSites.get(same).line(), sites.get(same).line())) {
                    // the same call, but the method it called cannot get from its last call to this
                    // one: it returned and was called again (a loop around the call)
                    break;
                }
            }
            List<Inside> path = new ArrayList<>();
            for (int d = 0; d < sites.size(); d++) {
                for (CodeReader.Branch b : sites.get(d).shape().branches()) {
                    CodeReader.Arm arm = b.armAt(sites.get(d).line());
                    if (arm != null) {
                        path.add(new Inside(d, b, arm));
                    }
                }
            }
            // a branch the lane is unsure about is not followed until it is decided again for sure
            final int sameFrames = same;
            path.removeIf(in -> {
                if (!unsure.contains(in.branch)) {
                    return false;
                }
                boolean again = sameFrames > in.depth && lastSites.size() > in.depth
                        && sites.get(in.depth).shape().paths().decidesAgain(lastSites.get(in.depth).line(), sites.get(in.depth).line(), in.branch);
                if (again) {
                    unsure.remove(in.branch);
                }
                return !again;
            });
            int keep = 0;
            while (keep < inside.size() && keep < path.size() && stillIn(inside.get(keep), path.get(keep), sites, same)) {
                Inside open = inside.get(keep);
                if (open.depth == same - 1 && lastSites.size() > open.depth
                        && sites.get(open.depth).shape().paths().mayDecideAgain(open.lastLine, sites.get(open.depth).line(), open.branch)) {
                    // the code may have gone round and decided it again, or not: no way to tell, so the
                    // call is not drawn in it, nor in a new one, until it is decided again for sure
                    unsure.add(open.branch);
                    break;
                }
                keep++;
            }
            // Between the two calls the code returned out of the frames it called from (from their
            // calls to their returns, deepest first), went along the frame both share (from one call
            // line to the other), and into the frames it calls from now (from their start)
            List<Went> olds = new ArrayList<>();
            for (int k = lastSites.size() - 1; k >= same; k--) {
                olds.add(new Went(lastSites, k, lastSites.get(k).line(), -1));
            }
            Went went = same > 0 ? new Went(sites, same - 1, lastSites.get(same - 1).line(), sites.get(same - 1).line()) : null;
            List<Went> news = new ArrayList<>();
            for (int k = same; k < sites.size(); k++) {
                news.add(new Went(sites, k, 0, sites.get(k).line())); // the lane's first call: from the top of its code
            }
            while (inside.size() > keep) {
                Inside in = inside.get(inside.size() - 1);
                for (Went w : olds) {
                    w.drawIn(in); // before the arm ends: they go in its case
                }
                if (went != null) {
                    went.drawIn(in);
                }
                leave(inside.remove(inside.size() - 1));
            }
            List<Inside> entering = new ArrayList<>();
            for (Inside in : path.subList(keep, path.size())) {
                if (!unsure.contains(in.branch)) {
                    entering.add(in);
                }
            }
            // a branch the call is no longer in is not unsure any more
            unsure.removeIf(b -> sites.stream().noneMatch(site -> b.armAt(site.line()) != null && site.shape().branches().contains(b)));
            for (Went w : olds) {
                w.drawRest(entering);
            }
            if (went != null) {
                went.drawRest(entering);
            }
            int next = 0;
            for (Went w : news) {
                // a frame's own ifs come after the branches of the frames around it are entered
                while (next < entering.size() && entering.get(next).depth < w.depth) {
                    enterNext(entering, next++, sites, went, news);
                }
                w.drawRest(entering.subList(next, entering.size()));
            }
            while (next < entering.size()) {
                enterNext(entering, next++, sites, went, news);
            }
            for (Inside in : inside) {
                in.lastLine = sites.get(in.depth).line();
            }
            lastSites = sites;
        }

        private void enterNext(List<Inside> entering, int k, List<Site> sites, Went went, List<Went> news) {
            Inside in = entering.get(k);
            enter(in, sites);
            List<Inside> later = entering.subList(k + 1, entering.size());
            if (went != null) {
                went.drawEntered(in, later);
            }
            for (Went w : news) {
                w.drawEntered(in, later);
            }
        }

        /** Whether a call is still in the same pass through an arm, not in a later one. */
        private boolean stillIn(Inside open, Inside wanted, List<Site> sites, int same) {
            if (open.depth != wanted.depth || open.branch != wanted.branch || open.arm != wanted.arm || open.depth >= same) {
                return false;
            }
            int d = open.depth;
            if (d < same - 1) {
                return true; // the frame still calls from the same line
            }
            int q = sites.get(d).line();
            int l = open.lastLine;
            boolean deepest = d == sites.size() - 1 && d == lastSites.size() - 1;
            if (q > l || (q == l && !deepest)) {
                // forward in the code, unless every way there decides the if or switch again: a
                // continue that jumps forward to a loop's condition comes round to it
                return !sites.get(d).shape().paths().decidesAgain(l, q, open.branch);
            }
            for (int[] loop : open.arm.loops()) {
                if (loop[0] <= q && l <= loop[1]) {
                    return true; // around a loop inside the arm
                }
            }
            return false; // around a loop outside it: the if or switch was decided again
        }

        /**
         * The ifs and switches a frame went through between two of its calls without making one: each
         * is drawn with the arm it took when every way between the calls goes through it exactly once
         * and in one arm only ({@link CodeReader.Paths#passed}). One inside an arm the lane is leaving
         * goes in that arm's case, one inside another drawn here in that one's case, and one inside an
         * if or switch that is not drawn is not drawn either.
         */
        private final class Went {
            private final List<Site> sites;
            private final int depth;
            private final List<CodeReader.Passed> todo = new ArrayList<>();
            private final CodeReader.Shape shape;
            /** Those drawn so far, innermost last, with their case lists. */
            private final List<Object[]> open = new ArrayList<>();

            Went(List<Site> sites, int depth, int fromLine, int toLine) {
                this.sites = sites;
                this.depth = depth;
                this.shape = sites.get(depth).shape();
                for (CodeReader.Passed p : shape.paths().passed(fromLine, toLine)) {
                    if (toLine <= 0 || p.branch().armAt(toLine) == null) {
                        todo.add(p); // one the next call is in is entered instead
                    }
                }
            }

            /** Draws those whose innermost enclosing branch the lane is in is {@code in}. */
            void drawIn(Inside in) {
                for (java.util.Iterator<CodeReader.Passed> it = todo.iterator(); it.hasNext(); ) {
                    CodeReader.Passed p = it.next();
                    if (container(p) == in) {
                        it.remove();
                        draw(p);
                    }
                }
                open.clear();
            }

            /** Draws those outside the branches the next call enters (those are drawn as each is entered). */
            void drawRest(List<Inside> entering) {
                Inside innermost = inside.isEmpty() ? null : inside.get(inside.size() - 1);
                for (java.util.Iterator<CodeReader.Passed> it = todo.iterator(); it.hasNext(); ) {
                    CodeReader.Passed p = it.next();
                    if (!within(p, entering) && container(p) == innermost) {
                        it.remove();
                        draw(p);
                    }
                }
                open.clear();
            }

            /** Draws those in the case of a branch just entered, and not in a branch entered after it. */
            void drawEntered(Inside in, List<Inside> later) {
                for (java.util.Iterator<CodeReader.Passed> it = todo.iterator(); it.hasNext(); ) {
                    CodeReader.Passed p = it.next();
                    if (!within(p, later) && container(p) == in) {
                        it.remove();
                        draw(p);
                    }
                }
                open.clear();
            }

            private boolean within(CodeReader.Passed p, List<Inside> branches) {
                return branches.stream().anyMatch(i -> i.depth == depth && i.branch.contains(p.branch()));
            }

            private Inside container(CodeReader.Passed p) {
                for (int k = inside.size() - 1; k >= 0; k--) {
                    Inside in = inside.get(k);
                    if (in.depth < depth || (in.depth == depth && in.branch != p.branch() && in.branch.contains(p.branch()))) {
                        return in;
                    }
                }
                return null;
            }

            private void draw(CodeReader.Passed p) {
                CodeReader.Branch b = p.branch();
                while (!open.isEmpty() && !((CodeReader.Branch) open.get(open.size() - 1)[0]).contains(b)) {
                    open.remove(open.size() - 1);
                }
                for (CodeReader.Branch other : shape.branches()) {
                    boolean drawnAround = open.stream().anyMatch(o -> o[0] == other)
                            || inside.stream().anyMatch(i -> i.depth == depth && i.branch == other);
                    if (other != b && other.contains(b) && !drawnAround) {
                        return; // inside an if or switch that is not drawn
                    }
                }
                String ref = alloc(b.name());
                Map<String, Object> entry = exec.entries.get(ref);
                if (entry != null) {
                    checkTaken(ref, entry, p.arm());
                    open.add(new Object[] {b, null});
                    return;
                }
                if (mode != Mode.ACTIVE) {
                    return;
                }
                Map<String, Object> sw = branchEntry(b, ref, p.arm());
                tagLoops(sw, loopsAt(sites, depth, b.line()));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parent = open.isEmpty() ? null : (List<Map<String, Object>>) open.get(open.size() - 1)[1];
                if (parent != null) {
                    parent.add(sw);
                } else {
                    emit(sw);
                }
                open.add(new Object[] {b, caseList(sw, p.arm())});
            }
        }

        /** The Java loops around a line of the frame at depth {@code d}: those of its callers, and its own. */
        private List<String> loopsAt(List<Site> sites, int d, int line) {
            List<String> loops = new ArrayList<>(startedInLoops);
            for (int i = 0; i <= d; i++) {
                Site s = sites.get(i);
                loops.addAll(CodeLoops.around(s.type(), s.method(), s.descriptor(), i == d ? line : s.line()));
            }
            return loops;
        }

        /**
         * The lane's code returned: its ifs and switches end, and one it went past after its last
         * call is drawn when every return of the method comes after it.
         */
        private void ended() {
            // every frame the last call was made from returned: from that call to its return, deepest first
            List<Went> wents = new ArrayList<>();
            for (int k = lastSites.size() - 1; k >= 0; k--) {
                wents.add(new Went(lastSites, k, lastSites.get(k).line(), -1));
            }
            if (lastSites.isEmpty() && id.equals("main")) {
                // code that made no call at all still went through its ifs and switches
                Site root = new Site(spec.method.getDeclaringClass(), spec.method.getName(),
                        org.objectweb.asm.Type.getMethodDescriptor(spec.method), 0);
                wents.add(new Went(List.of(root), 0, 0, -1));
            }
            while (!inside.isEmpty()) {
                Inside in = inside.get(inside.size() - 1);
                for (Went w : wents) {
                    w.drawIn(in);
                }
                leave(inside.remove(inside.size() - 1));
            }
            for (Went w : wents) {
                w.drawRest(List.of());
            }
        }

        private void enter(Inside in, List<Site> sites) {
            in.ref = alloc(in.branch.name());
            in.pausesBefore = counter;
            in.outer = out;
            inside.add(in);
            Map<String, Object> entry = exec.entries.get(in.ref);
            if (entry != null) {
                checkTaken(in.ref, entry, in.arm);
                in.drawn = true; // the calls go in this fragment, after the task paused on in there
                return;
            }
            if (mode != Mode.ACTIVE) {
                return; // recorded by a build that did not draw it: the calls go where they went
            }
            Map<String, Object> sw = branchEntry(in.branch, in.ref, in.arm);
            tagLoops(sw, loopsAt(sites, in.depth, in.branch.line()));
            emit(sw);
            in.caseList = caseList(sw, in.arm);
            out = in.caseList;
            in.drawn = true;
        }

        private void leave(Inside in) {
            if (!in.drawn) {
                return;
            }
            if (in.caseList != null && out == in.caseList) {
                out = in.outer;
            } else if (mode == Mode.ACTIVE && counter != in.pausesBefore) {
                anchorAfter = in.ref; // it ended in a later continuation: what follows goes after it
            }
        }

        @SuppressWarnings("unchecked")
        private void checkTaken(String ref, Map<String, Object> entry, CodeReader.Arm arm) {
            Map<String, Object> in = (Map<String, Object>) entry.get("inputParameters");
            Object recorded = in == null ? null : in.get("switchCaseValue");
            if (!Entries.escape(arm.key()).equals(String.valueOf(recorded))) {
                throw diverged(ref + " took " + recorded + " in the recorded execution but the code now takes " + arm.key());
            }
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> branchEntry(CodeReader.Branch b, String ref, CodeReader.Arm taken) {
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            for (CodeReader.Arm arm : b.arms()) {
                if (!arm.otherwise()) {
                    keys.add(arm.key());
                }
            }
            Map<String, Object> sw = switchEntry(ref, taken.key(), keys);
            for (CodeReader.Arm arm : b.arms()) {
                if (arm != taken) {
                    caseList(sw, arm).addAll(previewOf(arm.steps(), arm.key()));
                }
            }
            ((Map<String, Object>) sw.get("metadata")).put("site", b.site());
            return sw;
        }

        @SuppressWarnings("unchecked")
        private List<Map<String, Object>> caseList(Map<String, Object> sw, CodeReader.Arm arm) {
            return arm.otherwise()
                    ? (List<Map<String, Object>>) sw.get("defaultCase")
                    : ((Map<String, List<Map<String, Object>>>) sw.get("decisionCases")).get(arm.key());
        }

        private Map<String, Object> switchEntry(String ref, String key, java.util.Set<String> caseKeys) {
            Map<String, List<Map<String, Object>>> decisionCases = new LinkedHashMap<>();
            for (String k : caseKeys) {
                decisionCases.put(k, new ArrayList<>());
            }
            Map<String, Object> sw = new LinkedHashMap<>();
            sw.put("name", ref);
            sw.put("taskReferenceName", ref);
            sw.put("type", "SWITCH");
            sw.put("evaluatorType", "native-code"); // the code decided: the server reads the case it took
            sw.put("expression", "switchCaseValue");
            sw.put("inputParameters", Map.of("switchCaseValue", Entries.escape(key)));
            sw.put("decisionCases", decisionCases);
            sw.put("defaultCase", new ArrayList<Map<String, Object>>());
            sw.put("metadata", Entries.metadata(id));
            return sw;
        }

        @Override
        public Object repeatUntil(Supplier<?> loopBody, Functions.Pred<?> condition, Backoff backoff) {
            flushSyncGroup(this);
            Command check;
            capture = true;
            try {
                loopBody.get();
                throw new IllegalArgumentException("the repeatUntil body must make exactly one task call");
            } catch (Capture c) {
                check = c.command;
            } finally {
                capture = false;
            }
            String property = MethodRefs.propertyName(MethodRefs.resolve(condition));
            follow();
            String loopRef = alloc("poll");
            String checkRef = loopRef + "_check";
            used.add(loopRef + "_backoff");
            used.add(loopRef + "_wait");
            used.add(checkRef);
            if (exec.entries.containsKey(loopRef)) {
                pause(this);
                Execution.TaskView loop = exec.latest(loopRef);
                if (loop == null || !loop.terminal()) {
                    throw diverged("lane " + id + " resumed before loop " + loopRef + " finished");
                }
                Execution.TaskView last = exec.latestByBase(checkRef);
                if (last == null) {
                    throw new TaskFailure(loopRef, loop.status, loop.reason);
                }
                return decode(check, checkRef, last);
            }
            if (mode != Mode.ACTIVE) {
                throw diverged("the code repeats " + check.name + " as " + loopRef + " but the recorded execution has no such loop");
            }
            emit(Entries.continuation(loopEntry(loopRef, check, checkRef, property, backoff), id, counter + 1));
            pause(this);
            throw new IllegalStateException("a lane resumed within the continuation that paused it");
        }

        /**
         * One task per item, run sequentially by the server as a DO_WHILE over the items; the
         * code is woken once, when every item is done. Each iteration's task reads its item from
         * the loop's {@code loopItem} output.
         */
        @Override
        public List<Object> forEach(String name, List<?> items, Command task, String itemParameter) {
            flushSyncGroup(this);
            follow();
            if (items.isEmpty()) {
                return new ArrayList<>();
            }
            String loopRef = alloc(name);
            String taskRef = loopRef + "_task";
            used.add(taskRef);
            if (exec.entries.containsKey(loopRef)) {
                pause(this);
                Execution.TaskView loop = exec.latest(loopRef);
                if (loop == null || !loop.terminal()) {
                    throw diverged("lane " + id + " resumed before loop " + loopRef + " finished");
                }
                List<Object> results = new ArrayList<>();
                for (int i = 1; i <= items.size(); i++) {
                    Execution.TaskView t = exec.latest(taskRef + "__" + i);
                    if (t == null) {
                        throw new TaskFailure(loopRef, loop.status, "item " + i + " did not run: " + loop.reason);
                    }
                    results.add(decode(task, taskRef, t));
                }
                return results;
            }
            if (mode != Mode.ACTIVE) {
                throw diverged("the code iterates " + task.name + " as " + loopRef + " but the recorded execution has no such loop");
            }
            task.rawInput.put(itemParameter, "${" + loopRef + ".output.loopItem}");
            Map<String, Object> loop = new LinkedHashMap<>();
            loop.put("name", loopRef);
            loop.put("taskReferenceName", loopRef);
            loop.put("type", "DO_WHILE");
            loop.put("inputParameters", Map.of("_items", Entries.escape(Json.plain(items))));
            loop.put("loopCondition", "true");
            loop.put("loopOver", List.of(Entries.of(task, taskRef, id)));
            loop.put("metadata", Entries.metadata(id));
            emit(Entries.continuation(loop, id, counter + 1));
            pause(this);
            throw new IllegalStateException("a lane resumed within the continuation that paused it");
        }

        private Map<String, Object> loopEntry(String loopRef, Command check, String checkRef, String property, Backoff b) {
            Map<String, Object> backoffInput = new LinkedHashMap<>();
            backoffInput.put("evaluatorType", "graaljs");
            backoffInput.put(
                    "expression",
                    "(function(){ var i = $.iteration || 1; var s = Math.round("
                            + b.interval().toSeconds() + " * Math.pow(" + b.factor() + ", i - 1)); return Math.max(1, Math.min(s, "
                            + b.max().toSeconds() + ")) + 's'; })()");
            backoffInput.put("iteration", "${" + loopRef + ".output.iteration}");
            Map<String, Object> backoffTask = new LinkedHashMap<>();
            backoffTask.put("name", loopRef + "_backoff");
            backoffTask.put("taskReferenceName", loopRef + "_backoff");
            backoffTask.put("type", "INLINE");
            backoffTask.put("inputParameters", backoffInput);
            backoffTask.put("metadata", Entries.metadata(id));
            Map<String, Object> waitTask = new LinkedHashMap<>();
            waitTask.put("name", loopRef + "_wait");
            waitTask.put("taskReferenceName", loopRef + "_wait");
            waitTask.put("type", "WAIT");
            waitTask.put("inputParameters", Map.of("duration", "${" + loopRef + "_backoff.output.result}"));
            waitTask.put("metadata", Entries.metadata(id));
            Map<String, Object> checkTask = Entries.of(check, checkRef, id);
            Map<String, Object> loop = new LinkedHashMap<>();
            loop.put("name", loopRef);
            loop.put("taskReferenceName", loopRef);
            loop.put("type", "DO_WHILE");
            loop.put("inputParameters", new LinkedHashMap<>());
            loop.put(
                    "loopCondition",
                    "$." + checkRef + "['" + property + "'] != true && $." + loopRef + "['iteration'] < " + b.attempts());
            loop.put("loopOver", List.of(backoffTask, waitTask, checkTask));
            loop.put("metadata", Entries.metadata(id));
            return loop;
        }
    }

    private Object decode(Command cmd, String ref, Execution.TaskView t) {
        if (t.successful()) {
            Map<String, Object> output = t.output;
            if (cmd.emptyIsEmpty && (output == null || output.isEmpty())) {
                return Optional.empty();
            }
            Object v;
            if (cmd.httpBody) {
                Object response = output.get("response");
                Object body = response instanceof Map<?, ?> r ? r.get("body") : null;
                v = Json.convert(body, cmd.resultType);
            } else if (cmd.outputKey != null) {
                v = Json.convert(output.get(cmd.outputKey), cmd.resultType);
            } else {
                v = Json.decodeOutput(output, cmd.resultType);
            }
            return cmd.optional ? Optional.ofNullable(v) : v;
        }
        if (cmd.timeoutIsEmpty && "TIMED_OUT".equals(t.status)) {
            return Optional.empty();
        }
        throw new TaskFailure(ref, t.status, t.reason);
    }

    /** The promise of an async lane. */
    private final class LanePromise<T> implements Promise<T> {
        private final Lane lane;

        LanePromise(Lane lane) {
            this.lane = lane;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            ((Lane) Engine.current()).joinGroup(List.of(this));
            if (lane.error instanceof RuntimeException re) {
                throw re;
            }
            if (lane.error instanceof Error err) {
                throw err;
            }
            if (lane.error != null) {
                throw new WorkflowFailedException(lane.error.toString());
            }
            return (T) lane.result;
        }

        @Override
        public boolean isDone() {
            return lane.state == State.DONE;
        }
    }

    /** Invokes a handler method on the instance with arguments decoded from a message. */
    public static Object invokeHandler(Object instance, Method handler, Object args) {
        Object[] values;
        if (handler.getParameterCount() == 0) {
            values = new Object[0];
        } else if (handler.getParameterCount() == 1) {
            values = new Object[] {Json.convert(args, handler.getGenericParameterTypes()[0])};
        } else {
            List<?> list = args instanceof List<?> l ? l : List.of();
            values = new Object[handler.getParameterCount()];
            for (int i = 0; i < values.length; i++) {
                values[i] = i < list.size() ? Json.convert(list.get(i), handler.getGenericParameterTypes()[i]) : null;
            }
        }
        try {
            handler.setAccessible(true);
            return handler.invoke(instance, values);
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            throw c instanceof RuntimeException re ? re : new IllegalStateException(c);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
