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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Workflow;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A workflow takes its task classes as constructor parameters and calls them directly. */
class TaskStubsTest {

    public record Order(String item, int quantity) {}

    public static class Inventory {}

    /** A task class whose constructor needs a dependency: the stub never runs it. */
    public static class Tasks {
        private final Inventory inventory;

        public Tasks(Inventory inventory) {
            this.inventory = java.util.Objects.requireNonNull(inventory);
        }

        @WorkerTask("stub_reserve")
        public String reserve(@InputParam("order") Order order) {
            return "real " + inventory;
        }

        @WorkerTask("stub_ship")
        public void ship(@InputParam("id") String id) {}

        @WorkerTask("stub_ship2")
        public String ship2(@InputParam("id") String id) {
            return id;
        }

        public String helper() {
            return "not a task";
        }
    }

    public static class Direct {
        private final Tasks tasks;

        public Direct(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "stub_direct")
        public String run(Order order) {
            String id = tasks.reserve(order);
            tasks.ship(id);
            return id;
        }
    }

    public static class EachByStub {
        private final Tasks tasks;

        public EachByStub(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "stub_each")
        public List<String> run(Order order) {
            return org.conductoross.conductor.sdk.Conductor.forEach("ship_each", List.of("a", "b"), tasks::ship2);
        }
    }

    public static class EachByReference {
        @Workflow(name = "stub_each")
        public List<String> run(Order order) {
            return org.conductoross.conductor.sdk.Conductor.forEach("ship_each", List.of("a", "b"), Tasks::ship2);
        }
    }

    public static class ByReference {
        @Workflow(name = "stub_direct")
        public String run(Order order) {
            String id = task(Tasks::reserve, order);
            task(Tasks::ship, id);
            return id;
        }
    }

    public static class CreatesItsTasks {
        private final Tasks tasks = new Tasks(new Inventory());

        @Workflow(name = "stub_field")
        public String run(Order order) {
            return tasks.reserve(order);
        }
    }

    public static class CreatesTasksInItsCode {
        @Workflow(name = "stub_new")
        public String run(Order order) {
            return new Tasks(new Inventory()).reserve(order);
        }
    }

    public static class NeedsSomethingElse {
        public NeedsSomethingElse(Tasks tasks, Inventory inventory) {}

        @Workflow(name = "stub_other")
        public String run(Order order) {
            return "";
        }
    }

    public static final class FinalTasks {
        @WorkerTask("stub_final")
        public String run(@InputParam("x") String x) {
            return x;
        }
    }

    @Test
    void aWorkflowGetsAStubForEachTaskClassItsConstructorTakes() {
        Object instance = WorkflowSpec.of(Direct.class).get(0).newInstance();
        Tasks tasks = ((Direct) instance).tasks;
        assertInstanceOf(TaskStubs.Stub.class, tasks);
        assertInstanceOf(Tasks.class, tasks);
        assertNotSame(instance, WorkflowSpec.of(Direct.class).get(0).newInstance(), "a new instance per replay");
    }

    @SuppressWarnings("unchecked")
    @Test
    void aCallOnTheStubSchedulesTheTaskAsAMethodReferenceDoes() {
        List<Map<String, Object>> direct = firstFragment(Direct.class);
        List<Map<String, Object>> byReference = firstFragment(ByReference.class);
        assertEquals("stub_reserve", direct.get(0).get("name"));
        assertEquals(Map.of("order", Map.of("item", "book", "quantity", 2)), direct.get(0).get("inputParameters"));
        assertEquals(byReference.get(0).get("name"), direct.get(0).get("name"));
        assertEquals(byReference.get(0).get("inputParameters"), direct.get(0).get("inputParameters"));
    }

    @Test
    void forEachThroughAStubIsTheSameLoopAsByMethodReference() throws Exception {
        List<Map<String, Object>> byStub = firstFragment(EachByStub.class);
        assertEquals(firstFragment(EachByReference.class), byStub);
        assertTrue(byStub.toString().contains("stub_ship2"), byStub.toString());
        CodeReader.Loop loop = (CodeReader.Loop) CodeReader.read(EachByStub.class.getMethod("run", Order.class)).get(0);
        assertEquals("ship_each", loop.name());
        assertEquals("stub_ship2", ((CodeReader.Call) loop.body().get(0)).command().name);
    }

    @Test
    void conductorStubGivesAFactoryTheSameStub() {
        Tasks stub = org.conductoross.conductor.sdk.Conductor.stub(Tasks.class);
        assertInstanceOf(TaskStubs.Stub.class, stub);
        assertInstanceOf(Tasks.class, stub);
    }

    @Test
    void aMethodThatIsNotATaskCannotBeCalledOnAStub() {
        Tasks stub = TaskStubs.stub(Tasks.class);
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, stub::helper);
        assertTrue(e.getMessage().contains("Tasks.helper is not a @WorkerTask method"), e.getMessage());
    }

    @Test
    void aStubIsOnlyUsableInsideAWorkflow() {
        Tasks stub = TaskStubs.stub(Tasks.class);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> stub.reserve(new Order("book", 1)));
        assertTrue(e.getMessage().contains("only valid inside a @Workflow method"), e.getMessage());
    }

    @Test
    void aWorkflowThatCreatesItsOwnTaskObjectIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> WorkflowSpec.of(CreatesItsTasks.class).get(0).newInstance());
        assertTrue(e.getMessage().contains("field tasks of"), e.getMessage());
        assertTrue(e.getMessage().contains("take it as a constructor parameter"), e.getMessage());
    }

    @Test
    void readingTheCodeRejectsATaskObjectCreatedInTheWorkflowMethod() throws Exception {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CodeReader.read(CreatesTasksInItsCode.class.getMethod("run", Order.class)));
        assertTrue(e.getMessage().contains("creates a Tasks"), e.getMessage());
    }

    @Test
    void readingTheCodeSeesStubCallsAsTasks() throws Exception {
        List<CodeReader.Step> steps = CodeReader.read(Direct.class.getMethod("run", Order.class));
        assertEquals(List.of("stub_reserve", "stub_ship"),
                steps.stream().map(s -> ((CodeReader.Call) s).command().name).toList());
    }

    @Test
    void outsideSpringOnlyTaskClassesAreSupplied() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> WorkflowSpec.of(NeedsSomethingElse.class).get(0).newInstance());
        assertTrue(e.getMessage().contains("takes a Inventory"), e.getMessage());
        assertTrue(e.getMessage().contains("host().workflow(NeedsSomethingElse.class, factory)"), e.getMessage());
    }

    @Test
    void aFinalTaskClassCannotBeStubbed() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> TaskStubs.stub(FinalTasks.class));
        assertTrue(e.getMessage().contains("is final"), e.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> firstFragment(Class<?> type) {
        WorkflowSpec spec = WorkflowSpec.of(type).get(0);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", Map.of("item", "book", "quantity", 2));
        json.put("workflowDefinition", Map.of("name", "stub_direct", "tasks", List.of()));
        json.put("tasks", List.of());
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("lane", "main");
        marker.put("n", 1);
        marker.put("state", "LEASED");
        marker.put("step", 0L);
        marker.put("resumedWith", Map.of());
        marker.put("recorded", Map.of());
        json.put("nativeState", Map.of("bootstrap", marker));
        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), new Execution(json)).process("main", 1, null);
        return (List<Map<String, Object>>) out.output.get("fragment");
    }
}
