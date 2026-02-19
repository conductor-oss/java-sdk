/*
 * Copyright 2022 Conductor Authors.
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
package com.netflix.conductor.sdk.workflow.def;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import com.netflix.conductor.sdk.workflow.utils.MapBuilder;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowBuilderTests {

    static {
        WorkflowExecutor.initTaskImplementations();
    }

    @Test
    public void testBuildMinimalWorkflow() {
        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        ConductorWorkflow<Object> workflow = builder
                .name("minimal_wf")
                .version(1)
                .add(new SimpleTask("task_def", "task_ref"))
                .build();

        assertEquals("minimal_wf", workflow.getName());
        assertEquals(1, workflow.getVersion());
        assertTrue(workflow.isRestartable());

        WorkflowDef def = workflow.toWorkflowDef();
        assertEquals(1, def.getTasks().size());
        assertEquals("task_ref", def.getTasks().get(0).getTaskReferenceName());
    }

    @Test
    public void testBuildFullWorkflow() {
        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        ConductorWorkflow<Object> workflow = builder
                .name("full_wf")
                .version(3)
                .description("A full workflow")
                .failureWorkflow("failure_handler")
                .ownerEmail("test@example.com")
                .timeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY, 3600L)
                .restartable(false)
                .add(new SimpleTask("task1_def", "task1_ref"),
                     new SimpleTask("task2_def", "task2_ref"))
                .output(new MapBuilder()
                        .add("key1", "value1")
                        .add("key2", 42))
                .build();

        assertEquals("full_wf", workflow.getName());
        assertEquals(3, workflow.getVersion());
        assertEquals("A full workflow", workflow.getDescription());
        assertEquals("failure_handler", workflow.getFailureWorkflow());
        assertEquals("test@example.com", workflow.getOwnerEmail());
        assertEquals(WorkflowDef.TimeoutPolicy.ALERT_ONLY, workflow.getTimeoutPolicy());
        assertEquals(3600L, workflow.getTimeoutSeconds());
        assertFalse(workflow.isRestartable());

        Map<String, Object> output = workflow.getWorkflowOutput();
        assertEquals("value1", output.get("key1"));
        assertEquals(42, output.get("key2"));
    }

    @Test
    public void testToWorkflowDef() {
        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        ConductorWorkflow<Object> workflow = builder
                .name("def_wf")
                .version(2)
                .description("workflow for def test")
                .ownerEmail("owner@example.com")
                .failureWorkflow("on_failure")
                .timeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY, 600L)
                .restartable(false)
                .add(new SimpleTask("t1", "t1_ref"),
                     new SimpleTask("t2", "t2_ref"))
                .build();

        WorkflowDef def = workflow.toWorkflowDef();

        assertEquals("def_wf", def.getName());
        assertEquals(2, def.getVersion());
        assertEquals("workflow for def test", def.getDescription());
        assertEquals("owner@example.com", def.getOwnerEmail());
        assertEquals("on_failure", def.getFailureWorkflow());
        assertEquals(WorkflowDef.TimeoutPolicy.ALERT_ONLY, def.getTimeoutPolicy());
        assertEquals(600L, def.getTimeoutSeconds());
        assertFalse(def.isRestartable());

        List<WorkflowTask> tasks = def.getTasks();
        assertEquals(2, tasks.size());
        assertEquals("t1_ref", tasks.get(0).getTaskReferenceName());
        assertEquals("t1", tasks.get(0).getName());
        assertEquals("t2_ref", tasks.get(1).getTaskReferenceName());
        assertEquals("t2", tasks.get(1).getName());
    }

    @Test
    public void testBuildDuplicateTaskRefThrows() {
        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        builder.name("dup_wf")
                .version(1)
                .add(new SimpleTask("task_a", "same_ref"),
                     new SimpleTask("task_b", "same_ref"));

        ValidationError error = assertThrows(ValidationError.class, builder::build);
        assertTrue(error.getMessage().contains("same_ref"));
    }

    @Test
    public void testValidationError() {
        ValidationError error = new ValidationError("something went wrong");

        assertEquals("something went wrong", error.getMessage());
        assertInstanceOf(RuntimeException.class, error);
    }

    @Test
    public void testOutputOverloads() {
        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        ConductorWorkflow<Object> workflow = builder
                .name("output_wf")
                .version(1)
                .add(new SimpleTask("t", "t_ref"))
                .output("strKey", "strVal")
                .output("numKey", 99)
                .output("boolKey", true)
                .output(new MapBuilder().add("mapKey", "mapVal"))
                .build();

        Map<String, Object> output = workflow.getWorkflowOutput();
        assertEquals("strVal", output.get("strKey"));
        assertEquals(99, output.get("numKey"));
        assertEquals(true, output.get("boolKey"));
        assertEquals("mapVal", output.get("mapKey"));
    }

    @Test
    public void testVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("counter", 0);
        vars.put("flag", true);
        vars.put("label", "test");

        WorkflowBuilder<Object> builder = new WorkflowBuilder<>(null);
        ConductorWorkflow<Object> workflow = builder
                .name("vars_wf")
                .version(1)
                .add(new SimpleTask("t", "t_ref"))
                .variables(vars)
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) workflow.getVariables();
        assertNotNull(result);
        assertEquals(0, result.get("counter"));
        assertEquals(true, result.get("flag"));
        assertEquals("test", result.get("label"));
    }
}
