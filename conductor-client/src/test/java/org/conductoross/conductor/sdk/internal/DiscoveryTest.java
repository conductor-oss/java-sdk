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

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Workflow;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.switchOn;
import static org.conductoross.conductor.sdk.Conductor.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The definition registered for a workflow class is discovered from its code. */
class DiscoveryTest {

    public static class Tasks {
        @WorkerTask("pick")
        public String pick(@InputParam("item") String item) {
            return item;
        }

        @WorkerTask("pack")
        public String pack(@InputParam("item") String item) {
            return item;
        }

        @WorkerTask("ship")
        public String ship(@InputParam("item") String item) {
            return item;
        }
    }

    public record Order(String item, String region, int quantity) {}

    public static class Fulfilment {
        boolean cancelled;

        @Signal
        public void cancel() {
            cancelled = true;
        }

        @Workflow(name = "fulfilment", description = "Picks, packs and ships an order")
        public String run(Order order) {
            String picked = task(Tasks::pick, order.item());
            Promise<String> a = async(() -> task(Tasks::pack, picked + "-a"));
            Promise<String> b = async(() -> task(Tasks::pack, picked + "-b"));
            join(a, b);
            sleep(Duration.ofSeconds(order.quantity() + 1));
            switchOn("region", order.region())
                    .when("eu", () -> task(Tasks::ship, "eu"))
                    .when("us", () -> task(Tasks::ship, "us"))
                    .otherwise(() -> "hold")
                    .run();
            return picked;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void theDefinitionHoldsTheGraphTheCodeDescribes() {
        Map<String, Object> def = Conductor.definitions(Fulfilment.class).get(0);

        assertEquals("fulfilment", def.get("name"));
        assertEquals("Picks, packs and ships an order", def.get("description"));
        assertEquals(List.of("item", "region", "quantity"), def.get("inputParameters"));
        Map<String, Object> nativeMeta = (Map<String, Object>) ((Map<String, Object>) def.get("metadata")).get("native");
        assertEquals(true, nativeMeta.get("discovered"));
        assertEquals(List.of("cancel"), nativeMeta.get("signals"));

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) def.get("tasks");
        assertEquals(List.of("pick", "fork", "fork_join", "sleep", "region"),
                tasks.stream().map(t -> (String) t.get("taskReferenceName")).toList());
        // plain names, no preview marks, an inline task definition for every SIMPLE task
        assertEquals("pick", tasks.get(0).get("name"));
        assertFalse(((Map<String, Object>) tasks.get(0).get("metadata")).containsKey("preview"));
        assertEquals("pick", ((Map<String, Object>) tasks.get(0).get("taskDefinition")).get("name"));
        // the async bodies as the fork's branches
        List<List<Map<String, Object>>> branches = (List<List<Map<String, Object>>>) tasks.get(1).get("forkTasks");
        assertEquals(2, branches.size());
        assertEquals("pack", branches.get(0).get(0).get("taskReferenceName"));
        assertEquals("pack_2", branches.get(1).get(0).get("taskReferenceName"));
        // the switch with every case, each holding what it would call
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) tasks.get(4).get("decisionCases");
        assertEquals("ship", cases.get("eu").get(0).get("taskReferenceName"));
        assertEquals("ship_2", cases.get("us").get(0).get("taskReferenceName"));
        assertTrue(((List<?>) tasks.get(4).get("defaultCase")).isEmpty());
    }

    @Test
    void discoveryReadsCodeThatWouldNeedARealValue() {
        WorkflowSpec spec = WorkflowSpec.of(Fragile.class).get(0);
        List<Map<String, Object>> tasks = Replayer.discover(spec);
        assertEquals(List.of("pick", "if_length"), tasks.stream().map(t -> t.get("taskReferenceName")).toList());
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) tasks.get(1).get("decisionCases");
        assertEquals("ship", cases.get("true").get(0).get("taskReferenceName"));
    }

    static final java.util.concurrent.atomic.AtomicInteger SIDE_EFFECTS = new java.util.concurrent.atomic.AtomicInteger();
    static volatile boolean releaseSpin;

    public static class Spins {
        @Workflow(name = "spins")
        public String run(Order order) {
            task(Tasks::pick, order.item());
            while (!releaseSpin) {
                Thread.onSpinWait(); // plain code that never calls a task
            }
            return "done";
        }
    }

    public static class Once {
        @Workflow(name = "once")
        public String run(Order order) {
            String id = Conductor.once("id", () -> "id-" + SIDE_EFFECTS.incrementAndGet());
            return task(Tasks::pick, String.valueOf(id));
        }
    }

    @Test
    void codeThatLoopsForeverIsReadWithoutRunningIt() {
        WorkflowSpec spec = WorkflowSpec.of(Spins.class).get(0);
        Map<String, Object> def = Registration_skeleton(spec);
        Map<?, ?> nativeMeta = (Map<?, ?>) ((Map<?, ?>) def.get("metadata")).get("native");
        assertEquals(true, nativeMeta.get("discovered"));
        assertEquals(1, ((List<?>) def.get("tasks")).size());
    }

    private static Map<String, Object> Registration_skeleton(WorkflowSpec spec) {
        return Conductor.definitions(spec.type).get(0);
    }

    @Test
    void discoveryDoesNotRunOnceSuppliers() {
        int before = SIDE_EFFECTS.get();
        List<Map<String, Object>> tasks = Replayer.discover(WorkflowSpec.of(Once.class).get(0));
        assertEquals(List.of("pick"), tasks.stream().map(t -> t.get("taskReferenceName")).toList());
        assertEquals(before, SIDE_EFFECTS.get());
    }

    public static class Fragile {
        @Workflow(name = "fragile")
        public String run(Order order) {
            String picked = task(Tasks::pick, order.item());
            if (picked.length() > 3) {
                return task(Tasks::ship, picked);
            }
            throw new IllegalStateException("too short");
        }
    }
}
