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
package plainbranches;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain Java ifs and switches whose arms call the server are drawn as SWITCH tasks, read from the
 * compiled code. Outside the SDK's packages, as application code is.
 */
@SuppressWarnings("unchecked")
class PlainBranchesTest {

    public static class Tasks {
        @WorkerTask("pb_a")
        public String a(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("pb_b")
        public String b(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("pb_done")
        public String done(@InputParam("x") String x) {
            return x;
        }
    }

    public static class IfElse {
        private final Tasks tasks;

        public IfElse(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_if_else")
        public String run(@InputParam("route") String route) {
            if (route.equals("a")) {
                tasks.a("x");
            } else {
                tasks.b("y");
            }
            return tasks.done("z");
        }
    }

    public static class IfOnly {
        private final Tasks tasks;

        public IfOnly(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_if_only")
        public String run(@InputParam("route") String route) {
            if (route.equals("a")) {
                tasks.a("x");
            }
            return tasks.done("z");
        }
    }

    public static class Kinds {
        private final Tasks tasks;

        public Kinds(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_kinds")
        public String run(@InputParam("kind") String kind) {
            switch (kind) {
                case "odd" -> tasks.a("o");
                case "even", "zero" -> tasks.b("e");
                default -> {}
            }
            return tasks.done("z");
        }
    }

    public enum Speed {
        FAST,
        SLOW,
        NONE
    }

    public static class Speeds {
        private final Tasks tasks;

        public Speeds(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_speeds")
        public String run(@InputParam("speed") Speed speed) {
            switch (speed) {
                case FAST:
                    tasks.a("f");
                    break;
                case SLOW:
                    tasks.b("s");
                    break;
                default:
                    break;
            }
            return tasks.done("z");
        }
    }

    public static class Loops {
        private final Tasks tasks;

        public Loops(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_loops")
        public String run(@InputParam("routes") List<String> routes) {
            for (String route : routes) {
                if (route.equals("a")) {
                    tasks.a(route);
                }
            }
            int i = 0;
            while (i < 2) {
                tasks.b("w" + i);
                i++;
            }
            do {
                tasks.done("d" + i);
                i--;
            } while (i > 0);
            return "ok";
        }
    }

    public static class Conditions {
        private final Tasks tasks;

        public Conditions(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_conditions")
        public String run(@InputParam("route") String route) {
            if (route == null) {
                return "none"; // a guard without a call is plain code
            }
            if (route.length() > 3 && route.startsWith("x")) {
                tasks.a("and");
            }
            if (route.isEmpty() || route.startsWith("y")) {
                tasks.b("or");
            }
            return tasks.done("z");
        }
    }

    public static class Tail {
        private final Tasks tasks;

        public Tail(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_tail")
        public String run(@InputParam("route") String route) {
            tasks.a("first");
            if (route.equals("x")) {
                tasks.b("only when x");
            }
            return "ok";
        }
    }

    public static class LoopInArm {
        private final Tasks tasks;

        public LoopInArm(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_loop_in_arm")
        public String run(@InputParam("route") String route) {
            if (route.equals("a")) {
                for (int i = 0; i < 2; i++) {
                    tasks.a("x" + i);
                }
            }
            return "ok";
        }
    }

    // ------------------------------------------------------------------ the definition

    private static WorkflowSpec spec(Class<?> type) {
        return WorkflowSpec.of(type).get(0);
    }

    private static List<String> refs(List<Map<String, Object>> entries) {
        return entries.stream().map(e -> String.valueOf(e.get("taskReferenceName"))).toList();
    }

    private static Map<String, List<Map<String, Object>>> cases(Map<String, Object> sw) {
        return (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
    }

    private static List<Map<String, Object>> defaultCase(Map<String, Object> sw) {
        return (List<Map<String, Object>>) sw.get("defaultCase");
    }

    @Test
    void anIfElseIsASwitchWithATrueCaseAndTheElseAsDefault() {
        List<Map<String, Object>> entries = Replayer.discover(spec(IfElse.class));
        assertEquals(List.of("if_route", "pb_done"), refs(entries));
        Map<String, Object> sw = entries.get(0);
        assertEquals("SWITCH", sw.get("type"));
        assertEquals(List.of("true"), new ArrayList<>(cases(sw).keySet()));
        assertEquals(List.of("pb_a"), refs(cases(sw).get("true")));
        assertEquals(List.of("pb_b"), refs(defaultCase(sw)));
    }

    @Test
    void aStringSwitchHasACasePerArmLabelledAsWritten() {
        Map<String, Object> sw = Replayer.discover(spec(Kinds.class)).get(0);
        assertEquals("switch_kind", sw.get("taskReferenceName"));
        assertEquals(List.of("odd", "even,zero"), new ArrayList<>(cases(sw).keySet()));
        assertEquals(List.of("pb_a"), refs(cases(sw).get("odd")));
        assertEquals(List.of("pb_b"), refs(cases(sw).get("even,zero")));
        assertTrue(defaultCase(sw).isEmpty());
    }

    @Test
    void anEnumSwitchIsLabelledByTheConstants() {
        Map<String, Object> sw = Replayer.discover(spec(Speeds.class)).get(0);
        assertEquals("switch_speed", sw.get("taskReferenceName"));
        assertEquals(List.of("FAST", "SLOW"), new ArrayList<>(cases(sw).keySet()));
    }

    @Test
    void loopConditionsAreNotBranchesButAnIfInsideALoopIs() {
        List<Map<String, Object>> entries = Replayer.discover(spec(Loops.class));
        assertEquals(List.of("if_route", "pb_b", "pb_done"), refs(entries));
    }

    @Test
    void aCompoundConditionIsOneBranchAndAGuardWithoutACallIsPlainCode() {
        List<Map<String, Object>> entries = Replayer.discover(spec(Conditions.class));
        assertEquals(List.of("if_startsWith", "if_startsWith_2", "pb_done"), refs(entries));
        assertEquals(List.of("pb_a"), refs(cases(entries.get(0)).get("true")));
        assertEquals(List.of("pb_b"), refs(cases(entries.get(1)).get("true")));
    }

    // ------------------------------------------------------------------ executions

    private static Execution execution(Map<String, Object> input, List<Map<String, Object>> defTasks,
                                       List<Map<String, Object>> tasks, Map<String, Object> bootstrap) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", input);
        json.put("workflowDefinition", Map.of("name", "pb", "tasks", defTasks));
        json.put("tasks", tasks);
        json.put("nativeState", Map.of("bootstrap", bootstrap));
        return new Execution(json);
    }

    private static Map<String, Object> marker(int n, String state, long step) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lane", "main");
        m.put("n", n);
        m.put("state", state);
        m.put("step", step);
        m.put("resumedWith", Map.of());
        m.put("recorded", Map.of());
        return m;
    }

    private static Map<String, Object> taskEntry(String ref, String name, Map<String, Object> continuation) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name", name);
        e.put("taskReferenceName", ref);
        e.put("type", "SIMPLE");
        Map<String, Object> md = new LinkedHashMap<>();
        md.put("lane", "main");
        if (continuation != null) {
            md.put("continuation", continuation);
        }
        e.put("metadata", md);
        return e;
    }

    private static Map<String, Object> switchEntry(String ref, String taken, Map<String, List<Map<String, Object>>> cases,
                                                   List<Map<String, Object>> otherwise) {
        Map<String, Object> sw = new LinkedHashMap<>();
        sw.put("name", ref);
        sw.put("taskReferenceName", ref);
        sw.put("type", "SWITCH");
        sw.put("inputParameters", Map.of("switchCaseValue", taken));
        sw.put("decisionCases", cases);
        sw.put("defaultCase", otherwise);
        return sw;
    }

    private static Map<String, Object> done(String ref, String type, int seq, Map<String, Object> output, Map<String, Object> continuation) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", "id-" + ref);
        t.put("referenceTaskName", ref);
        t.put("taskType", type);
        t.put("status", "COMPLETED");
        t.put("seq", seq);
        t.put("outputData", output);
        t.put("workflowTask", Map.of("type", type));
        if (continuation != null) {
            t.put("continuation", continuation);
        }
        return t;
    }

    private static Replayer.Outcome first(Class<?> type, Map<String, Object> input) {
        WorkflowSpec spec = spec(type);
        return new Replayer(spec, spec.newInstance(), execution(input, List.of(), List.of(), marker(1, "LEASED", 0)))
                .process("main", 1, null);
    }

    private static List<Map<String, Object>> fragment(Replayer.Outcome out) {
        return (List<Map<String, Object>>) out.output.get("fragment");
    }

    private static Map<String, Object> metadata(Map<String, Object> entry) {
        return (Map<String, Object>) entry.get("metadata");
    }

    @Test
    void theTakenArmsCallsGoInsideItsCaseAndTheOtherArmIsPreviewed() {
        List<Map<String, Object>> fragment = fragment(first(IfElse.class, Map.of("route", "a")));
        assertEquals(List.of("if_route"), refs(fragment), "the code paused inside the if");
        Map<String, Object> sw = fragment.get(0);
        assertEquals(Map.of("switchCaseValue", "true"), sw.get("inputParameters"));
        Map<String, Object> a = cases(sw).get("true").get(0);
        assertEquals("pb_a", a.get("taskReferenceName"));
        assertEquals(Map.of("lane", "main", "n", 2), metadata(a).get("continuation"));
        Map<String, Object> preview = defaultCase(sw).get(0);
        assertEquals("pb_b_if_false", preview.get("taskReferenceName"));
        assertEquals(true, metadata(preview).get("preview"));
        assertTrue(String.valueOf(metadata(sw).get("site")).startsWith("PlainBranchesTest$IfElse.run:"));
    }

    @Test
    void anIfPassedWithoutACallIsDrawnWithTheArmThatMustHaveBeenTaken() {
        List<Map<String, Object>> fragment = fragment(first(IfOnly.class, Map.of("route", "b")));
        assertEquals(List.of("if_route", "pb_done"), refs(fragment));
        Map<String, Object> sw = fragment.get(0);
        assertEquals(Map.of("switchCaseValue", "false"), sw.get("inputParameters"));
        assertEquals("pb_a_if_true", cases(sw).get("true").get(0).get("taskReferenceName"));
        assertTrue(defaultCase(sw).isEmpty());
    }

    @Test
    void aSwitchDefaultPassedWithoutACallIsDrawnToo() {
        List<Map<String, Object>> fragment = fragment(first(Kinds.class, Map.of("kind", "prime")));
        assertEquals(List.of("switch_kind", "pb_done"), refs(fragment));
        assertEquals(Map.of("switchCaseValue", "default"), fragment.get(0).get("inputParameters"));
    }

    @Test
    void anEnumCaseIsTakenByItsConstant() {
        List<Map<String, Object>> fragment = fragment(first(Speeds.class, Map.of("speed", "SLOW")));
        Map<String, Object> sw = fragment.get(0);
        assertEquals(Map.of("switchCaseValue", "SLOW"), sw.get("inputParameters"));
        assertEquals(List.of("pb_b"), refs(cases(sw).get("SLOW")));
        assertEquals(List.of("pb_a_if_FAST"), refs(cases(sw).get("FAST")));
    }

    /** The recording of IfElse with route a: the if took true and pb_a finished. */
    private static Execution ifElseAfterA(String recordedCase) {
        Map<String, Object> inCase = taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2));
        Map<String, List<Map<String, Object>>> cases = new LinkedHashMap<>();
        cases.put("true", "true".equals(recordedCase) ? List.of(inCase) : new ArrayList<>());
        return execution(Map.of("route", "a"),
                List.of(switchEntry("if_route", recordedCase, cases, "true".equals(recordedCase) ? List.of() : List.of(inCase))),
                List.of(done("if_route", "SWITCH", 1, Map.of("selectedCase", recordedCase), null),
                        done("pb_a", "pb_a", 2, Map.of("result", "x"), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
    }

    @Test
    void theCallAfterAnArmEndedInAnEarlierContinuationGoesAfterTheSwitch() {
        WorkflowSpec spec = spec(IfElse.class);
        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), ifElseAfterA("true")).process("main", 2, null);
        List<Map<String, Object>> fragment = fragment(out);
        assertEquals(List.of("pb_done"), refs(fragment));
        assertEquals("if_route", metadata(fragment.get(0)).get("after"));
    }

    @Test
    void anotherArmThanTheRecordedOneIsADivergence() {
        WorkflowSpec spec = spec(IfElse.class);
        NonDeterminismException e = assertThrows(NonDeterminismException.class,
                () -> new Replayer(spec, spec.newInstance(), ifElseAfterA("false")).process(null, 0, null));
        assertTrue(e.getMessage().contains("if_route took false in the recorded execution but the code now takes true"), e.getMessage());
    }

    @Test
    void anExecutionRecordedWithoutTheSwitchReplaysAndContinuesAsItWas() {
        Execution exec = execution(Map.of("route", "a"),
                List.of(taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2))),
                List.of(done("pb_a", "pb_a", 1, Map.of("result", "x"), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
        WorkflowSpec spec = spec(IfElse.class);
        Replayer.Outcome out = assertDoesNotThrow(() -> new Replayer(spec, spec.newInstance(), exec).process("main", 2, null));
        List<Map<String, Object>> fragment = fragment(out);
        assertEquals(List.of("pb_done"), refs(fragment));
        assertNull(metadata(fragment.get(0)).get("after"));
    }

    @Test
    void anIfPassedAfterTheLastCallIsDrawnWhenTheCodeReturns() {
        Execution exec = execution(Map.of("route", "y"),
                List.of(taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2))),
                List.of(done("pb_a", "pb_a", 1, Map.of("result", "x"), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
        WorkflowSpec spec = spec(Tail.class);
        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), exec).process("main", 2, null);
        assertEquals(true, out.output.get("done"));
        List<Map<String, Object>> fragment = fragment(out);
        assertEquals(List.of("if_route"), refs(fragment));
        assertEquals(Map.of("switchCaseValue", "false"), fragment.get(0).get("inputParameters"));
    }

    @Test
    void theSwitchIsInTheLoopsAroundTheIfNotThoseInsideItsArm() {
        Map<String, Object> sw = fragment(first(LoopInArm.class, Map.of("route", "a"))).get(0);
        assertEquals(List.of(), metadata(sw).get("loops"));
        Map<String, Object> a = cases(sw).get("true").get(0);
        assertEquals(1, ((List<?>) metadata(a).get("loops")).size(), "the call is inside the arm's loop");
    }

    @Test
    void anIfInALoopIsDrawnOncePerPass() {
        Map<String, Object> first = taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2));
        Map<String, List<Map<String, Object>>> cases = new LinkedHashMap<>();
        cases.put("true", List.of(first));
        Execution exec = execution(Map.of("routes", List.of("a", "a")),
                List.of(switchEntry("if_route", "true", cases, List.of())),
                List.of(done("if_route", "SWITCH", 1, Map.of("selectedCase", "true"), null),
                        done("pb_a", "pb_a", 2, Map.of("result", "a"), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
        WorkflowSpec spec = spec(Loops.class);
        List<Map<String, Object>> fragment = fragment(new Replayer(spec, spec.newInstance(), exec).process("main", 2, null));
        assertEquals(List.of("if_route_2"), refs(fragment));
        assertEquals("if_route", metadata(fragment.get(0)).get("after"));
        assertEquals(1, ((List<?>) metadata(fragment.get(0)).get("loops")).size(), "the if is inside the for loop");
        assertEquals(List.of("pb_a_2"), refs(cases(fragment.get(0)).get("true")));
    }

    // ------------------------------------------------------------------ in memory

    @Test
    void inMemoryAWorkflowTakingTaskClassesCallsTheRegisteredTaskObjects() {
        Tasks tasks = org.mockito.Mockito.mock(Tasks.class);
        org.mockito.Mockito.when(tasks.b("y")).thenReturn("from b");
        org.mockito.Mockito.when(tasks.done("z")).thenReturn("finished");
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(IfElse.class).tasks(tasks);
            TestRun<String> run = env.start(IfElse::run, "b");
            assertEquals("finished", run.result());
            assertEquals(List.of("pb_b", "pb_done"), run.ranTasks());
            org.mockito.Mockito.verify(tasks, org.mockito.Mockito.never()).a(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    void inMemoryAFactoryGivesTheWorkflowTaskStubsFromConductorStub() {
        Tasks tasks = org.mockito.Mockito.mock(Tasks.class);
        org.mockito.Mockito.when(tasks.done("z")).thenReturn("finished");
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflow(IfElse.class, () -> new IfElse(Conductor.stub(Tasks.class))).tasks(tasks);
            assertEquals("finished", env.start(IfElse::run, "a").result());
            org.mockito.Mockito.verify(tasks).a("x");
        }
    }

    // ------------------------------------------------------------------ Thread.sleep

    public static class Sleepy {
        private final Tasks tasks;

        public Sleepy(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_sleepy")
        public String run(@InputParam("x") String x) throws InterruptedException {
            Thread.sleep(10); // SLEEP_DIRECT
            nap();
            org.conductoross.conductor.sdk.Conductor.async(() -> {
                pause(); // SLEEP_LAMBDA_CALL
                return tasks.a(x);
            });
            return tasks.done(x);
        }

        private static void nap() throws InterruptedException {
            Thread.sleep(10); // SLEEP_HELPER
        }

        private static void pause() {
            try {
                Thread.sleep(10); // SLEEP_LAMBDA
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void everyThreadSleepTheWorkflowCanReachIsFoundWithItsLine() throws Exception {
        List<String> source = java.nio.file.Files.readAllLines(
                java.nio.file.Path.of("src/test/java/plainbranches/PlainBranchesTest.java"));
        java.util.function.Function<String, Integer> line = mark -> {
            for (int i = 0; i < source.size(); i++) {
                if (source.get(i).trim().endsWith("// " + mark)) {
                    return i + 1;
                }
            }
            throw new AssertionError(mark);
        };
        List<String> sites = org.conductoross.conductor.sdk.internal.CodeReader.threadSleeps(
                Sleepy.class.getMethod("run", String.class));
        assertEquals(List.of(
                        "PlainBranchesTest$Sleepy.run:" + line.apply("SLEEP_DIRECT"),
                        "PlainBranchesTest$Sleepy.nap:" + line.apply("SLEEP_HELPER"),
                        "PlainBranchesTest$Sleepy.pause:" + line.apply("SLEEP_LAMBDA")),
                sites);
        assertEquals(List.of(), org.conductoross.conductor.sdk.internal.CodeReader.threadSleeps(
                IfElse.class.getMethod("run", String.class)));
    }

    // ------------------------------------------------------------------ an exception, and what it skipped

    public static class Between {
        private final Tasks tasks;

        public Between(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_between")
        public String run(@InputParam("x") String x) {
            String raw = tasks.a(x);
            int n = Integer.parseInt(raw); // THROWS_HERE
            return tasks.b("n" + n);
        }
    }

    public static class Guard {
        private final Tasks tasks;

        public Guard(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_guard")
        public String run(@InputParam("x") String x) {
            String raw = tasks.a(x);
            if (raw.startsWith("s")) {
                Integer.parseInt(raw);
                tasks.b("parsed");
            } else {
                tasks.b("unparsed");
            }
            return tasks.done(raw);
        }
    }

    public static class Nested {
        private final Tasks tasks;

        public Nested(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "pb_nested")
        public String run(@InputParam("x") String x) {
            String raw = tasks.a(x);
            String checked = check(raw);
            return tasks.done(checked);
        }

        private String check(String raw) {
            Integer.parseInt(raw);
            return tasks.b(raw);
        }
    }

    /** The recording after task a returned {@code value}; continuation 2 resumes after it. */
    private static Execution afterA(String value) {
        return execution(Map.of("x", "in"),
                List.of(taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2))),
                List.of(done("pb_a", "pb_a", 1, Map.of("result", value), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
    }

    private static Replayer.Outcome second(Class<?> type, String value) {
        WorkflowSpec spec = spec(type);
        return new Replayer(spec, spec.newInstance(), afterA(value)).process("main", 2, null);
    }

    @Test
    void anExceptionBetweenTwoTasksMarksWhereItThrewAndPreviewsWhatWasSkipped() throws Exception {
        Replayer.Outcome out = second(Between.class, "seven");
        assertEquals(true, out.output.get("done"));
        Map<String, Object> error = (Map<String, Object>) out.output.get("error");
        assertEquals("throw", error.get("ref"));
        List<Map<String, Object>> fragment = fragment(out);
        assertEquals(List.of("throw", "pb_b_if_no_error"), refs(fragment));
        Map<String, Object> thrown = fragment.get(0);
        assertEquals("TERMINATE", thrown.get("type"));
        assertEquals("NumberFormatException", thrown.get("name"));
        int line = 1;
        List<String> source = java.nio.file.Files.readAllLines(java.nio.file.Path.of("src/test/java/plainbranches/PlainBranchesTest.java"));
        while (!source.get(line - 1).trim().endsWith("// THROWS_HERE")) {
            line++;
        }
        assertEquals("PlainBranchesTest$Between.run:" + line, metadata(thrown).get("site"));
        Map<String, Object> skipped = fragment.get(1);
        assertEquals("pb_b", skipped.get("name"));
        assertEquals(true, metadata(skipped).get("preview"));
        assertEquals("throw", metadata(skipped).get("previewAfter"));
    }

    @Test
    void anExceptionInAnArmPreviewsTheRestOfTheArmThenWhatFollowsTheIfNotTheOtherArm() {
        List<Map<String, Object>> fragment = fragment(second(Guard.class, "seven"));
        // the if the code threw inside is drawn, the mark and the rest of that arm in its case
        assertEquals(List.of("if_startsWith", "pb_done_if_no_error"), refs(fragment));
        Map<String, Object> sw = fragment.get(0);
        assertEquals(Map.of("switchCaseValue", "true"), sw.get("inputParameters"));
        assertEquals(List.of("throw", "pb_b_if_no_error"), refs(cases(sw).get("true")));
        assertEquals(List.of("pb_b_if_false"), refs(defaultCase(sw)), "the other arm is previewed as an untaken arm");
    }

    @Test
    void anExceptionInAHelperPreviewsTheRestOfTheHelperThenOfTheWorkflow() {
        List<Map<String, Object>> fragment = fragment(second(Nested.class, "x"));
        assertEquals(List.of("throw", "pb_b_if_no_error", "pb_done_if_no_error"), refs(fragment));
    }

    @Test
    void aReplayOfTheFailedExecutionFindsTheMarkAndItsPreviews() {
        Replayer.Outcome out = second(Between.class, "seven");
        List<Map<String, Object>> defs = new ArrayList<>();
        defs.add(taskEntry("pb_a", "pb_a", Map.of("lane", "main", "n", 2)));
        defs.addAll(fragment(out));
        Execution failed = execution(Map.of("x", "in"), defs,
                List.of(done("pb_a", "pb_a", 1, Map.of("result", "seven"), marker(2, "APPLIED", 2))),
                marker(1, "APPLIED", 1));
        WorkflowSpec spec = spec(Between.class);
        assertDoesNotThrow(() -> new Replayer(spec, spec.newInstance(), failed).process(null, 0, null));
    }
}
