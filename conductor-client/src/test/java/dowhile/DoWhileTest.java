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
package dowhile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * doWhile is ordinary code in a loop the code names: every step its body makes is tagged with the
 * loop, as the steps of a plain Java loop are with the loop read from the compiled code.
 */
class DoWhileTest {

    /** The task stub the workflows below call: its calls schedule the tasks. */
    static final Tasks TASKS = org.conductoross.conductor.sdk.Conductor.stub(Tasks.class);

    public static class Tasks {
        @WorkerTask("dw_step")
        public String step(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("dw_extra")
        public String extra(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("dw_done")
        public String done(@InputParam("x") String x) {
            return x;
        }
    }

    /** Two passes of one task, then a task after the loop. */
    public static class Twice {
        @Workflow(name = "dw_twice")
        public String run(Map<String, Object> input) {
            int[] pass = {0};
            doWhile("poll", () -> {
                TASKS.step("p" + pass[0]);
                pass[0]++;
            }, () -> pass[0] < 2);
            return TASKS.done("z");
        }
    }

    /** An if in the body, and a Java loop around the doWhile. */
    public static class Nested {
        @Workflow(name = "dw_nested")
        public String run(@InputParam("rounds") List<String> rounds) {
            for (String round : rounds) {
                int[] pass = {0};
                doWhile("inner", () -> {
                    if (round.equals("a")) {
                        TASKS.extra(round);
                    }
                    pass[0]++;
                }, () -> pass[0] < 1);
            }
            return TASKS.done("z");
        }
    }

    /** The body throws on its first pass. */
    public static class Throws {
        @Workflow(name = "dw_throws")
        public String run(Map<String, Object> input) {
            doWhile("poll", () -> {
                if (input.get("fail") != null) {
                    throw new IllegalStateException("no");
                }
                TASKS.step("p");
            }, () -> false);
            return TASKS.done("z");
        }
    }

    private static WorkflowSpec spec(Class<?> type) {
        return WorkflowSpec.of(type).get(0);
    }

    private static Execution execution(String name, Map<String, Object> input, List<Map<String, Object>> defTasks,
                                       List<Map<String, Object>> tasks, Map<String, Object> bootstrap) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", input);
        json.put("workflowDefinition", Map.of("name", name, "tasks", defTasks));
        json.put("tasks", tasks);
        json.put("nativeState", Map.of("bootstrap", bootstrap));
        return new Execution(json);
    }

    /** A marker of continuation {@code n} of the main lane; applied at {@code step} when APPLIED. */
    private static Map<String, Object> marker(int n, String state, long step) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lane", "main");
        m.put("n", n);
        if ("APPLIED".equals(state)) {
            m.put("endTime", step);
        }
        m.put("leased", "LEASED".equals(state));
        m.put("resumedWith", Map.of());
        m.put("recorded", Map.of());
        return m;
    }

    private static Map<String, Object> entry(String ref, String name, int continuation, List<String> loops) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("name", name);
        e.put("taskReferenceName", ref);
        e.put("type", "SIMPLE");
        e.put("metadata", Map.of("lane", "main", "continuation", continuation, "loops", loops));
        return e;
    }

    private static Map<String, Object> doneTask(String ref, String type, int seq, Map<String, Object> output,
                                                Map<String, Object> continuation) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", "id-" + ref);
        t.put("referenceTaskName", ref);
        t.put("taskType", type);
        t.put("status", "COMPLETED");
        t.put("seq", seq);
        t.put("outputData", output);
        t.put("workflowTask", Map.of("type", type, "metadata",
                Map.of("lane", "main", "continuation", continuation.get("n"))));
        t.put("continuation", continuation);
        return t;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fragment(Replayer.Outcome out) {
        return (List<Map<String, Object>>) out.output.get("fragment");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadata(Map<String, Object> entry) {
        return (Map<String, Object>) entry.get("metadata");
    }

    private static List<Object> refs(List<Map<String, Object>> entries) {
        return entries.stream().map(e -> e.get("taskReferenceName")).toList();
    }

    @Test
    void eachPassOfTheBodyIsInTheNamedLoopAndWhatFollowsIsNot() {
        WorkflowSpec spec = spec(Twice.class);
        Execution first = execution("dw_twice", Map.of(), List.of(), List.of(), marker(1, "LEASED", 0));
        List<Map<String, Object>> one = fragment(new Replayer(spec, spec.newInstance(), first).process("main", 1));
        assertEquals(List.of("dw_step"), refs(one));
        assertEquals(List.of("poll"), metadata(one.get(0)).get("loops"));

        Execution second = execution("dw_twice", Map.of(),
                List.of(entry("dw_step", "dw_step", 2, List.of("poll"))),
                List.of(doneTask("dw_step", "dw_step", 1, Map.of("result", "p0"), marker(2, "LEASED", 0))),
                marker(1, "APPLIED", 1));
        List<Map<String, Object>> two = fragment(new Replayer(spec, spec.newInstance(), second).process("main", 2));
        assertEquals(List.of("dw_step_2"), refs(two));
        assertEquals(List.of("poll"), metadata(two.get(0)).get("loops"));

        Execution third = execution("dw_twice", Map.of(),
                List.of(entry("dw_step", "dw_step", 2, List.of("poll")), entry("dw_step_2", "dw_step", 3, List.of("poll"))),
                List.of(doneTask("dw_step", "dw_step", 1, Map.of("result", "p0"), marker(2, "APPLIED", 2)),
                        doneTask("dw_step_2", "dw_step", 2, Map.of("result", "p1"), marker(3, "LEASED", 0))),
                marker(1, "APPLIED", 1));
        List<Map<String, Object>> three = fragment(new Replayer(spec, spec.newInstance(), third).process("main", 3));
        assertEquals(List.of("dw_done"), refs(three));
        assertNull(metadata(three.get(0)).get("loops"), "the loop ended: the next call is outside it");
    }

    @SuppressWarnings("unchecked")
    @Test
    void anIfInTheBodyIsInTheLoopInsideTheJavaLoopAroundIt() {
        WorkflowSpec spec = spec(Nested.class);
        Execution first = execution("dw_nested", Map.of("rounds", List.of("a")), List.of(), List.of(), marker(1, "LEASED", 0));
        List<Map<String, Object>> fragment = fragment(new Replayer(spec, spec.newInstance(), first).process("main", 1));
        assertEquals(1, fragment.size());
        Map<String, Object> sw = fragment.get(0);
        assertEquals("SWITCH", sw.get("type"));
        List<String> loops = (List<String>) metadata(sw).get("loops");
        assertEquals(2, loops.size(), "the Java for loop, then the doWhile: " + loops);
        assertTrue(loops.get(0).startsWith("Nested_loop_"), loops.get(0));
        assertEquals("inner", loops.get(1));
        Map<String, Object> extra = ((Map<String, List<Map<String, Object>>>) sw.get("decisionCases")).get("true").get(0);
        assertEquals(loops, metadata(extra).get("loops"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aThrowInTheBodyIsMarkedInTheLoop() {
        WorkflowSpec spec = spec(Throws.class);
        Execution first = execution("dw_throws", Map.of("fail", true), List.of(), List.of(), marker(1, "LEASED", 0));
        List<Map<String, Object>> fragment = fragment(new Replayer(spec, spec.newInstance(), first).process("main", 1));
        // the code threw inside the if in the body: the mark is in that if's case
        Map<String, Object> sw = fragment.get(0);
        assertEquals("SWITCH", sw.get("type"), String.valueOf(fragment));
        assertEquals(List.of("poll"), metadata(sw).get("loops"));
        Map<String, Object> thrown = ((Map<String, List<Map<String, Object>>>) sw.get("decisionCases")).get("true").stream()
                .filter(e -> "TERMINATE".equals(e.get("type"))).findFirst().orElse(null);
        assertNotNull(thrown, "the mark of where the code threw: " + sw);
        assertEquals(List.of("poll"), metadata(thrown).get("loops"));
    }

    @Test
    void theDefinitionReadFromTheCodeHasTheBodyInTheNamedLoop() {
        List<Map<String, Object>> entries = Replayer.discover(spec(Twice.class));
        assertEquals(List.of("dw_step", "dw_done"), refs(entries));
        assertEquals(List.of("poll"), metadata(entries.get(0)).get("loops"));
        assertNull(metadata(entries.get(1)).get("loops"));

        List<Map<String, Object>> nested = Replayer.discover(spec(Nested.class));
        List<Object> all = new ArrayList<>();
        nested.forEach(e -> all.add(metadata(e).get("loops")));
        assertEquals(2, ((List<?>) all.get(0)).size(), "the for loop, then the doWhile: " + all);
        assertEquals("inner", ((List<?>) all.get(0)).get(1));
    }
}
