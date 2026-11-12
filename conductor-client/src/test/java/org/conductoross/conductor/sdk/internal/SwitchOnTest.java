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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Workflow;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.switchOn;
import static org.conductoross.conductor.sdk.Conductor.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** switchOn lowers to a SWITCH entry with every case; the taken case's calls land inside it. */
class SwitchOnTest {

    public static class Tasks {
        @WorkerTask("shipA")
        public String a(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("shipB")
        public String b(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("notify")
        public String send(@InputParam("x") String x) {
            return x;
        }
    }

    public static class Flow {
        @Workflow(name = "switch_flow")
        public String run(Map<String, Object> input) {
            String route = String.valueOf(input.get("route"));
            String r = switchOn("route", route)
                    .when("a", () -> task(Tasks::a, "x"))
                    .when("none", () -> "nothing")
                    .otherwise(() -> task(Tasks::b, "y"))
                    .get();
            return task(Tasks::send, r);
        }
    }

    public static class NoOtherwise {
        @Workflow(name = "no_otherwise")
        public String run(Map<String, Object> input) {
            switchOn("route", input.get("route")).when("a", () -> task(Tasks::a, "x")).run();
            return "done";
        }
    }

    public static class ChangedFirstCall {
        @Workflow(name = "switch_flow")
        public String run(Map<String, Object> input) {
            return task(Tasks::b, "x");
        }
    }

    private static WorkflowSpec spec(Class<?> type) {
        return WorkflowSpec.of(type).get(0);
    }

    /** An execution with the given definition entries, tasks and applied markers. */
    private static Execution execution(Map<String, Object> input, List<Map<String, Object>> defTasks,
                                       List<Map<String, Object>> tasks, Map<String, Object> bootstrap) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", input);
        json.put("workflowDefinition", Map.of("name", "switch_flow", "tasks", defTasks));
        json.put("tasks", tasks);
        json.put("nativeState", Map.of("bootstrap", bootstrap));
        return new Execution(json);
    }

    private static Map<String, Object> marker(String lane, int n, String state, long step) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lane", lane);
        m.put("n", n);
        m.put("state", state);
        m.put("step", step);
        m.put("resumedWith", Map.of());
        m.put("recorded", Map.of());
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> switchEntry(String taken, List<Map<String, Object>> inCase) {
        Map<String, Object> sw = new LinkedHashMap<>();
        sw.put("name", "route");
        sw.put("taskReferenceName", "route");
        sw.put("type", "SWITCH");
        sw.put("inputParameters", Map.of("switchCaseValue", taken));
        Map<String, List<Map<String, Object>>> cases = new LinkedHashMap<>();
        cases.put("a", "a".equals(taken) ? inCase : new ArrayList<>());
        cases.put("none", new ArrayList<>());
        sw.put("decisionCases", cases);
        sw.put("defaultCase", "a".equals(taken) ? new ArrayList<>() : inCase);
        return sw;
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

    private static Map<String, Object> doneTask(String ref, String type, int seq, Map<String, Object> output, Map<String, Object> continuation) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", "id-" + ref);
        t.put("referenceTaskName", ref);
        t.put("taskType", type);
        t.put("status", "COMPLETED");
        t.put("seq", seq);
        t.put("outputData", output);
        t.put("workflowTask", Map.of("type", "SIMPLE".equals(type) ? "SIMPLE" : type));
        if (continuation != null) {
            t.put("continuation", continuation);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    @Test
    void theFirstContinuationEmitsTheSwitchWithEveryCaseAndTheTakenBodyInsideIt() {
        Execution exec = execution(Map.of("route", "a"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Replayer.Outcome out = new Replayer(spec(Flow.class), new Flow(), exec).process("main", 1, null);

        List<Map<String, Object>> fragment = (List<Map<String, Object>>) out.output.get("fragment");
        assertEquals(1, fragment.size(), "only the switch is at the top level: the code paused inside it");
        Map<String, Object> sw = fragment.get(0);
        assertEquals("SWITCH", sw.get("type"));
        assertEquals("value-param", sw.get("evaluatorType"));
        assertEquals(Map.of("switchCaseValue", "a"), sw.get("inputParameters"));
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
        assertEquals(List.of("a", "none"), new ArrayList<>(cases.keySet()), "every declared case is present");
        assertEquals(1, cases.get("a").size());
        assertEquals("shipA", cases.get("a").get(0).get("name"));
        assertEquals(Map.of("lane", "main", "n", 2), ((Map<String, Object>) cases.get("a").get(0).get("metadata")).get("continuation"));
        assertTrue(cases.get("none").isEmpty(), "an untaken case with no calls is an empty branch");
        // the untaken default case shows the task it would have called, as a preview
        List<Map<String, Object>> defaultCase = (List<Map<String, Object>>) sw.get("defaultCase");
        assertEquals("shipB_if_default", defaultCase.get(0).get("taskReferenceName"));
        assertEquals(true, ((Map<String, Object>) defaultCase.get(0).get("metadata")).get("preview"));
        assertEquals(false, out.output.get("done"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void anEmptyCaseContinuesInlineAfterTheSwitch() {
        Execution exec = execution(Map.of("route", "none"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Replayer.Outcome out = new Replayer(spec(Flow.class), new Flow(), exec).process("main", 1, null);

        List<Map<String, Object>> fragment = (List<Map<String, Object>>) out.output.get("fragment");
        assertEquals(List.of("route", "notify"), fragment.stream().map(e -> e.get("taskReferenceName")).toList());
        Map<String, Object> sw = fragment.get(0);
        assertTrue(((Map<String, List<?>>) sw.get("decisionCases")).get("none").isEmpty());
        assertNull(((Map<String, Object>) fragment.get(1).get("metadata")).get("after"), "same fragment: no anchor needed");
    }

    @SuppressWarnings("unchecked")
    @Test
    void anUnhandledValueTakesTheDefaultCase() {
        Execution exec = execution(Map.of("route", "zzz"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Replayer.Outcome out = new Replayer(spec(Flow.class), new Flow(), exec).process("main", 1, null);

        Map<String, Object> sw = ((List<Map<String, Object>>) out.output.get("fragment")).get(0);
        assertEquals(Map.of("switchCaseValue", "zzz"), sw.get("inputParameters"));
        List<Map<String, Object>> defaultCase = (List<Map<String, Object>>) sw.get("defaultCase");
        assertEquals("shipB", defaultCase.get(0).get("name"));
        assertEquals("shipB", defaultCase.get(0).get("taskReferenceName"), "the taken body's call is real");
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
        assertEquals("shipA_if_a", cases.get("a").get(0).get("taskReferenceName"), "untaken case a previews its call");
        assertTrue(cases.get("none").isEmpty());
    }

    @Test
    void anUnhandledValueWithoutOtherwiseFailsTheWorkflow() {
        Execution exec = execution(Map.of("route", "zzz"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Replayer.Outcome out = new Replayer(spec(NoOtherwise.class), new NoOtherwise(), exec).process("main", 1, null);
        assertEquals(true, out.output.get("done"));
        assertTrue(String.valueOf(((Map<?, ?>) out.output.get("error")).get("message")).contains("no case for zzz"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resumingInsideACaseAnchorsTheNextCallAfterTheSwitch() {
        // the recording: the switch took case a, whose task finished; this continuation resumes
        // after it and the code leaves the case
        Map<String, Object> aEntry = taskEntry("shipA", "shipA", Map.of("lane", "main", "n", 2));
        Execution exec = execution(Map.of("route", "a"),
                List.of(switchEntry("a", List.of(aEntry))),
                List.of(doneTask("route", "SWITCH", 1, Map.of("selectedCase", "a"), null),
                        doneTask("shipA", "shipA", 2, Map.of("result", "x"), marker("main", 2, "LEASED", 0))),
                marker("main", 1, "APPLIED", 1));
        Replayer.Outcome out = new Replayer(spec(Flow.class), new Flow(), exec).process("main", 2, null);

        List<Map<String, Object>> fragment = (List<Map<String, Object>>) out.output.get("fragment");
        assertEquals(1, fragment.size());
        assertEquals("notify", fragment.get(0).get("taskReferenceName"));
        assertEquals("route", ((Map<String, Object>) fragment.get(0).get("metadata")).get("after"),
                "the call after the case goes after the switch, not inside the case");
    }

    public static class DroppedCall {
        @Workflow(name = "dropped_call")
        public String run(Map<String, Object> input) {
            return task(Tasks::a, "x");
        }
    }

    @Test
    void aRecordedCallTheCodeNoLongerMakesIsADivergence() {
        Execution exec = execution(Map.of(),
                List.of(taskEntry("shipA", "shipA", Map.of("lane", "main", "n", 2)), taskEntry("notify", "notify", null)),
                List.of(doneTask("shipA", "shipA", 1, Map.of("result", "x"), marker("main", 2, "APPLIED", 2)),
                        doneTask("notify", "notify", 2, Map.of("result", "y"), null)),
                marker("main", 1, "APPLIED", 1));
        org.junit.jupiter.api.Assertions.assertThrows(org.conductoross.conductor.sdk.NonDeterminismException.class,
                () -> new Replayer(spec(DroppedCall.class), new DroppedCall(), exec).process(null, 0, null));
    }

    public static class Previewed {
        @org.conductoross.conductor.sdk.Variable
        public List<String> notes = new ArrayList<>();

        @Workflow(name = "previewed")
        public String run(Map<String, Object> input) {
            switchOn("route", input.get("route"))
                    .when("a", () -> task(Tasks::a, "x"))
                    .when("b", () -> {
                        // a body that keeps going past its first call, forks, and mutates a variable
                        String first = task(Tasks::b, "y");
                        notes.add("previewed " + first);
                        org.conductoross.conductor.sdk.Promise<String> p1 = org.conductoross.conductor.sdk.Conductor.async(() -> task(Tasks::a, "left"));
                        org.conductoross.conductor.sdk.Promise<String> p2 = org.conductoross.conductor.sdk.Conductor.async(() -> task(Tasks::b, "right"));
                        org.conductoross.conductor.sdk.Conductor.join(p1, p2);
                        return task(Tasks::send, first.toUpperCase());
                    })
                    .when("c", () -> {
                        switchOn("inner", "?").when("x", () -> task(Tasks::a, "ix")).otherwise(() -> task(Tasks::b, "iy")).run();
                        return "c";
                    })
                    .otherwise(() -> task(Tasks::send, "fallback"))
                    .get();
            return "done";
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void aValueNoCaseHandlesIsDrawnBeforeTheWorkflowFails() {
        Execution exec = execution(Map.of("route", "nowhere"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Map<String, Object> out = new Replayer(WorkflowSpec.of(NoOtherwise.class).get(0), new NoOtherwise(), exec).process("main", 1, null).output;
        Map<String, Object> sw = ((List<Map<String, Object>>) out.get("fragment")).get(0);
        assertEquals("SWITCH", sw.get("type"));
        assertTrue(((Map<String, List<?>>) sw.get("decisionCases")).values().stream().anyMatch(c -> !c.isEmpty()), sw.toString());
        assertTrue(((Map<String, Object>) out.get("error")).get("message").toString().contains("no case"), out.toString());
    }

    public static class SameNameInALoop {
        @Workflow(name = "same_name")
        public String run(Map<String, Object> input) {
            for (int i = 0; i < 2; i++) {
                switchOn("route", String.valueOf(i)).when("x", () -> "none").otherwise(() -> "none").get();
            }
            return task(Tasks::send, "after");
        }
    }

    public static class DollarValue {
        @Workflow(name = "dollar_value")
        public String run(Map<String, Object> input) {
            return switchOn("route", "${workflow.input}").when("${workflow.input}", () -> task(Tasks::a, "x")).get();
        }
    }

    public static class TaskInFinally {
        @Workflow(name = "task_in_finally")
        public String run(Map<String, Object> input) {
            try {
                return task(Tasks::a, "x");
            } finally {
                task(Tasks::b, "y");
            }
        }
    }

    public static class Unjoined {
        @Workflow(name = "unjoined")
        public String run(Map<String, Object> input) {
            org.conductoross.conductor.sdk.Conductor.async(() -> task(Tasks::a, "x"));
            return "done";
        }
    }

    public static class CallBeforeJoin {
        @Workflow(name = "call_before_join")
        public String run(Map<String, Object> input) {
            org.conductoross.conductor.sdk.Conductor.async(() -> task(Tasks::a, "x"));
            return task(Tasks::b, "y");
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fragmentOf(Class<?> flow, Object instance) {
        Execution exec = execution(Map.of(), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        return (List<Map<String, Object>>) new Replayer(WorkflowSpec.of(flow).get(0), instance, exec).process("main", 1, null).output.get("fragment");
    }

    @Test
    void theSameSwitchNameInALoopGetsANumberedReference() {
        List<Map<String, Object>> fragment = fragmentOf(SameNameInALoop.class, new SameNameInALoop());
        assertEquals(List.of("route", "route_2", "notify"), fragment.stream().map(e -> e.get("taskReferenceName")).toList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void aSwitchValueIsNeverEvaluatedAsAnExpression() {
        Map<String, Object> sw = fragmentOf(DollarValue.class, new DollarValue()).get(0);
        assertEquals(Map.of("switchCaseValue", "$${workflow.input}"), sw.get("inputParameters"));
        assertTrue(((Map<String, Object>) sw.get("decisionCases")).containsKey("${workflow.input}"));
    }

    @Test
    void aTaskInAFinallyBlockDoesNotRunWhenTheLanePauses() {
        List<Map<String, Object>> fragment = fragmentOf(TaskInFinally.class, new TaskInFinally());
        assertEquals(List.of("shipA"), fragment.stream().map(e -> e.get("taskReferenceName")).toList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void lanesThatAreNeverJoinedAreForkedWithAJoinThatDoesNotWait() {
        Execution exec = execution(Map.of(), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Map<String, Object> out = new Replayer(WorkflowSpec.of(Unjoined.class).get(0), new Unjoined(), exec).process("main", 1, null).output;
        assertEquals(true, out.get("done"));
        assertNull(out.get("error"));
        List<Map<String, Object>> fragment = (List<Map<String, Object>>) out.get("fragment");
        assertEquals(List.of("fork", "fork_join"), fragment.stream().map(e -> e.get("taskReferenceName")).toList());
        assertEquals(true, ((Map<String, Object>) fragment.get(1).get("metadata")).get("detached"));
    }

    @Test
    void aCallBeforeTheJoinRunsAlongsideTheLane() {
        List<Map<String, Object>> fragment = fragmentOf(CallBeforeJoin.class, new CallBeforeJoin());
        assertEquals(List.of("fork", "fork_join", "shipB"), fragment.stream().map(e -> e.get("taskReferenceName")).toList());
    }

    @Test
    void aValueThatIsNotAnObjectDecodesFromResult() {
        java.util.UUID id = java.util.UUID.randomUUID();
        assertEquals(id, Json.decodeOutput(Map.of("result", id.toString()), java.util.UUID.class));
    }

    public static class FannedOut {
        @Workflow(name = "fanned_out")
        public String run(Map<String, Object> input) {
            switchOn("route", input.get("route"))
                    .when("a", () -> task(Tasks::a, "x"))
                    .when("b", () -> {
                        List<String> items = new ArrayList<>(); // what a stand-in result looks like
                        org.conductoross.conductor.sdk.Conductor.fanOut("spread", items, i -> task(Tasks::b, i)).get();
                        return task(Tasks::send, "after");
                    })
                    .get();
            return "done";
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void anUntakenFanOutIsADynamicForkAndJoinEvenWithNoItemsToShow() {
        Execution exec = execution(Map.of("route", "a"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Replayer.Outcome out = new Replayer(WorkflowSpec.of(FannedOut.class).get(0), new FannedOut(), exec).process("main", 1, null);

        Map<String, Object> sw = ((List<Map<String, Object>>) out.output.get("fragment")).get(0);
        List<Map<String, Object>> b = ((Map<String, List<Map<String, Object>>>) sw.get("decisionCases")).get("b");
        assertEquals(List.of("spread_if_b", "spread_if_b_join", "notify_if_b"), b.stream().map(e -> e.get("taskReferenceName")).toList());
        assertEquals("FORK_JOIN_DYNAMIC", b.get(0).get("type"));
        assertEquals("dynamicTasks", b.get(0).get("dynamicForkTasksParam"));
        assertEquals("dynamicTasksInput", b.get(0).get("dynamicForkTasksInputParamName"));
        assertEquals("JOIN", b.get(1).get("type"));
        assertTrue(b.stream().allMatch(e -> Boolean.TRUE.equals(((Map<String, Object>) e.get("metadata")).get("preview"))));
    }

    @SuppressWarnings("unchecked")
    @Test
    void untakenCasesShowTheTasksTheyWouldHaveCalled() {
        Execution exec = execution(Map.of("route", "a"), List.of(), List.of(), marker("main", 1, "LEASED", 0));
        Previewed instance = new Previewed();
        Replayer.Outcome out = new Replayer(WorkflowSpec.of(Previewed.class).get(0), instance, exec).process("main", 1, null);

        Map<String, Object> sw = ((List<Map<String, Object>>) out.output.get("fragment")).get(0);
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
        // the taken case holds the real call, paused on
        assertEquals(List.of("shipA"), cases.get("a").stream().map(e -> e.get("taskReferenceName")).toList());
        // case b: its first call, the fork of its two async bodies, and the call after the join
        List<String> b = cases.get("b").stream().map(e -> (String) e.get("taskReferenceName")).toList();
        assertEquals(List.of("shipB_if_b", "fork_if_b", "fork_if_b_join", "notify_if_b"), b, b.toString());
        assertTrue(cases.get("b").stream().allMatch(e -> Boolean.TRUE.equals(((Map<String, Object>) e.get("metadata")).get("preview"))));
        assertTrue(cases.get("b").stream().noneMatch(e -> ((Map<String, Object>) e.get("metadata")).containsKey("continuation")));
        List<List<Map<String, Object>>> branches = (List<List<Map<String, Object>>>) cases.get("b").get(1).get("forkTasks");
        assertEquals(2, branches.size());
        assertEquals("shipA_if_b", branches.get(0).get(0).get("taskReferenceName"));
        assertEquals("shipB_if_b_2", branches.get(1).get(0).get("taskReferenceName"));
        // case c: a nested switch with both of its cases previewed
        Map<String, Object> inner = cases.get("c").get(0);
        assertEquals("SWITCH", inner.get("type"));
        assertEquals("inner_if_c", inner.get("taskReferenceName"));
        assertEquals("shipA_if_c_x", ((Map<String, List<Map<String, Object>>>) inner.get("decisionCases")).get("x").get(0).get("taskReferenceName"));
        assertEquals("shipB_if_c_default", ((List<Map<String, Object>>) inner.get("defaultCase")).get(0).get("taskReferenceName"));
        // the default case too
        assertEquals("notify_if_default", ((List<Map<String, Object>>) sw.get("defaultCase")).get(0).get("taskReferenceName"));
        // the preview left no trace on the instance
        assertTrue(instance.notes.isEmpty(), instance.notes.toString());
        assertEquals(Map.of("notes", List.of()), out.output.get("variables"));
    }

    @Test
    void aSwitchThatTakesAnotherCaseOnReplayIsADivergence() {
        Map<String, Object> aEntry = taskEntry("shipA", "shipA", Map.of("lane", "main", "n", 2));
        Execution exec = execution(Map.of("route", "none"), // the code now takes "none" ...
                List.of(switchEntry("a", List.of(aEntry))), // ... but the recording took "a"
                List.of(doneTask("route", "SWITCH", 1, Map.of("selectedCase", "a"), null),
                        doneTask("shipA", "shipA", 2, Map.of("result", "x"), marker("main", 2, "LEASED", 0))),
                marker("main", 1, "APPLIED", 1));
        NonDeterminismException e = assertThrows(NonDeterminismException.class,
                () -> new Replayer(spec(Flow.class), new Flow(), exec).process("main", 2, null));
        assertTrue(e.getMessage().contains("took case a"), e.getMessage());
    }

    @Test
    void aDivergenceNamesWhatTheCodeCallsAndWhatWasRecordedThere() {
        Execution exec = execution(Map.of(),
                List.of(taskEntry("shipA", "shipA", Map.of("lane", "main", "n", 2))),
                List.of(doneTask("shipA", "shipA", 1, Map.of("result", "x"), marker("main", 2, "LEASED", 0))),
                marker("main", 1, "APPLIED", 1));
        NonDeterminismException e = assertThrows(NonDeterminismException.class,
                () -> new Replayer(spec(ChangedFirstCall.class), new ChangedFirstCall(), exec).process("main", 2, null));
        assertTrue(e.getMessage().startsWith("The running code differs from what this execution recorded: at "),
                e.getMessage());
        assertTrue(e.getMessage().endsWith(
                " it calls shipB, but the recording has shipA there. Deploy a compatible build to continue."),
                e.getMessage());
    }
}
