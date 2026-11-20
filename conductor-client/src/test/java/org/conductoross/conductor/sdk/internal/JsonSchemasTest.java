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

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkflowInstanceIdInputParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonSchemasTest {

    enum Tier { CRITICAL, STANDARD }

    record Host(String name, Tier tier, List<String> tags, int port) {}

    record Region(String name, List<Host> hosts, Map<String, Double> load, Instant since, LocalDate day) {}

    static final class Node {
        public String id;
        public Node next; // holds itself
    }

    @SuppressWarnings("unused")
    static final class Tasks {
        public Region inventory(@InputParam("fleet") String fleet, @InputParam("max") int max) {
            return null;
        }

        public String probe(@WorkflowInstanceIdInputParam String workflowId, @InputParam("host") Host host) {
            return null;
        }

        public @OutputParam("hosts") List<Host> hosts(@InputParam("region") Region region) {
            return null;
        }

        public void noOutput(@InputParam("x") String x) {}

        public TaskResult raw(@InputParam("x") String x) {
            return null;
        }

        public Node node(@InputParam("x") String x) {
            return null;
        }

        public String workflow(Region region) {
            return null;
        }

        public List<String> twoInputs(@InputParam("a") String a, @InputParam("b") boolean b) {
            return null;
        }
    }

    private static Method m(String name) {
        for (Method method : Tasks.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> props(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @Test
    void valuesAreTypedAsJacksonSeesThem() {
        Map<String, Object> region = JsonSchemas.of(Region.class);
        assertEquals("object", region.get("type"));
        Map<String, Object> p = props(region);
        assertEquals(Map.of("type", "string"), p.get("name"));
        assertEquals(Map.of("type", "object", "additionalProperties", Map.of("type", "number")), p.get("load"));
        assertEquals(Map.of("type", "string", "format", "date-time"), p.get("since"));
        assertEquals(Map.of("type", "string", "format", "date"), p.get("day"));

        Map<String, Object> hosts = (Map<String, Object>) p.get("hosts");
        assertEquals("array", hosts.get("type"));
        Map<String, Object> host = props((Map<String, Object>) hosts.get("items"));
        assertEquals(Map.of("type", "string", "enum", List.of("CRITICAL", "STANDARD")), host.get("tier"));
        assertEquals(Map.of("type", "array", "items", Map.of("type", "string")), host.get("tags"));
        assertEquals(Map.of("type", "integer"), host.get("port"));
    }

    @Test
    void aTaskInputHasOnePropertyPerParameterAsCallsNameIt() {
        Map<String, Object> input = JsonSchemas.taskInput(m("inventory"));
        assertEquals("http://json-schema.org/draft-07/schema#", input.get("$schema"));
        assertEquals(Map.of("fleet", Map.of("type", "string"), "max", Map.of("type", "integer")), props(input));
        // the workflow id parameter is filled from the task, not from the input
        assertEquals(List.of("host"), List.copyOf(props(JsonSchemas.taskInput(m("probe"))).keySet()));
    }

    @Test
    void anOutputObjectSpreadsAndAnyOtherValueIsResult() {
        assertEquals(List.of("name", "hosts", "load", "since", "day"),
                List.copyOf(props(JsonSchemas.taskOutput(m("inventory"))).keySet()));
        assertEquals(Map.of("result", Map.of("type", "string")), props(JsonSchemas.taskOutput(m("probe"))));
        assertEquals(List.of("hosts"), List.copyOf(props(JsonSchemas.taskOutput(m("hosts"))).keySet()));
        assertEquals(Map.of("result", Map.of("type", "array", "items", Map.of("type", "string"))),
                props(JsonSchemas.taskOutput(m("twoInputs"))));
    }

    @Test
    void nothingIsSaidOfNoOutputOrARawResult() {
        assertNull(JsonSchemas.taskOutput(m("noOutput")));
        assertNull(JsonSchemas.taskOutput(m("raw")));
    }

    @Test
    void aTypeThatHoldsItselfIsNotUnrolledAgain() {
        Map<String, Object> node = props(JsonSchemas.taskOutput(m("node")));
        assertEquals(Map.of("type", "object"), node.get("next"));
    }

    @Test
    void aWorkflowTakesASingleObjectsPropertiesElseOnePerParameter() {
        assertEquals(List.of("name", "hosts", "load", "since", "day"),
                List.copyOf(props(JsonSchemas.workflowInput(m("workflow"))).keySet()));
        assertEquals(List.of("a", "b"), List.copyOf(props(JsonSchemas.workflowInput(m("twoInputs"))).keySet()));
        assertEquals(Map.of("result", Map.of("type", "string")), props(JsonSchemas.workflowOutput(m("workflow"))));
    }
}
