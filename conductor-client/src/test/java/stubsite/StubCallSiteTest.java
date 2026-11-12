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
package stubsite;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Outside the SDK's packages, as application code is: the site is the workflow's line, not the stub's. */
class StubCallSiteTest {

    public static class Tasks {
        @WorkerTask("site_greet")
        public String greet(@InputParam("name") String name) {
            return name;
        }
    }

    public static class Flow {
        private final Tasks tasks;

        public Flow(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "site_flow")
        public String run(@InputParam("name") String name) {
            return tasks.greet(name); // CALL_LINE
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void theSiteOfAStubCallIsTheLineInTheWorkflow() throws Exception {
        WorkflowSpec spec = WorkflowSpec.of(Flow.class).get(0);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", Map.of("name", "World"));
        json.put("workflowDefinition", Map.of("name", "site_flow", "tasks", List.of()));
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

        List<Map<String, Object>> fragment = (List<Map<String, Object>>) out.output.get("fragment");
        assertEquals(1, fragment.size(), "the replay paused on the task: " + out.output);
        Map<String, Object> entry = fragment.get(0);
        List<String> source = java.nio.file.Files.readAllLines(
                java.nio.file.Path.of("src/test/java/stubsite/StubCallSiteTest.java"));
        int line = 1;
        while (!source.get(line - 1).contains("// CALL_" + "LINE")) {
            line++;
        }
        assertEquals("StubCallSiteTest$Flow.run:" + line, ((Map<String, Object>) entry.get("metadata")).get("site"));
    }
}
