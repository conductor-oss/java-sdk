/*
 * Copyright 2025 Conductor Authors.
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
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import static org.junit.jupiter.api.Assertions.*;

public class ConductorWorkflowTests {

    static {
        WorkflowExecutor.initTaskImplementations();
    }

    @Test
    public void testGettersAndSetters() {
        ConductorWorkflow<Object> wf = new ConductorWorkflow<>(null);
        wf.setName("test_wf");
        wf.setVersion(5);
        wf.setDescription("A test workflow");
        wf.setFailureWorkflow("failure_wf");
        wf.setOwnerEmail("dev@example.com");
        wf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        wf.setTimeoutSeconds(7200L);
        wf.setRestartable(false);

        Map<String, Object> defaultInput = new HashMap<>();
        defaultInput.put("key", "val");
        wf.setDefaultInput(defaultInput);

        Map<String, Object> variables = new HashMap<>();
        variables.put("counter", 0);
        wf.setVariables(variables);

        Map<String, Object> output = new HashMap<>();
        output.put("result", "done");
        wf.setWorkflowOutput(output);

        assertEquals("test_wf", wf.getName());
        assertEquals(5, wf.getVersion());
        assertEquals("A test workflow", wf.getDescription());
        assertEquals("failure_wf", wf.getFailureWorkflow());
        assertEquals("dev@example.com", wf.getOwnerEmail());
        assertEquals(WorkflowDef.TimeoutPolicy.TIME_OUT_WF, wf.getTimeoutPolicy());
        assertEquals(7200L, wf.getTimeoutSeconds());
        assertFalse(wf.isRestartable());
        assertEquals(defaultInput, wf.getDefaultInput());
        assertEquals(variables, wf.getVariables());
        assertEquals(output, wf.getWorkflowOutput());
    }

    @Test
    public void testAddTask() {
        ConductorWorkflow<Object> wf = new ConductorWorkflow<>(null);
        wf.setName("add_test");
        wf.setVersion(1);
        wf.add(new SimpleTask("t1", "t1_ref"));
        wf.add(new SimpleTask("t2", "t2_ref"));

        WorkflowDef def = wf.toWorkflowDef();
        assertEquals(2, def.getTasks().size());
        assertEquals("t1_ref", def.getTasks().get(0).getTaskReferenceName());
        assertEquals("t2_ref", def.getTasks().get(1).getTaskReferenceName());
    }

    @Test
    public void testToWorkflowDefWithVariablesAndDefaultInput() {
        ConductorWorkflow<Map<String, Object>> wf = new ConductorWorkflow<>(null);
        wf.setName("full_def");
        wf.setVersion(2);
        wf.setDescription("desc");
        wf.setOwnerEmail("test@test.com");
        wf.setFailureWorkflow("fail_wf");
        wf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY);
        wf.setTimeoutSeconds(1800L);
        wf.setRestartable(false);

        Map<String, Object> input = new HashMap<>();
        input.put("param1", "default");
        wf.setDefaultInput(input);

        Map<String, Object> vars = new HashMap<>();
        vars.put("state", "init");
        wf.setVariables(vars);

        Map<String, Object> output = new HashMap<>();
        output.put("out", "${workflow.output.result}");
        wf.setWorkflowOutput(output);

        wf.add(new SimpleTask("task_a", "ref_a"));

        WorkflowDef def = wf.toWorkflowDef();
        assertEquals("full_def", def.getName());
        assertEquals(2, def.getVersion());
        assertEquals("desc", def.getDescription());
        assertEquals("test@test.com", def.getOwnerEmail());
        assertEquals("fail_wf", def.getFailureWorkflow());
        assertEquals(WorkflowDef.TimeoutPolicy.ALERT_ONLY, def.getTimeoutPolicy());
        assertEquals(1800L, def.getTimeoutSeconds());
        assertFalse(def.isRestartable());
        assertNotNull(def.getInputTemplate());
        assertEquals("default", def.getInputTemplate().get("param1"));
        assertEquals("init", def.getVariables().get("state"));
        assertEquals("${workflow.output.result}", def.getOutputParameters().get("out"));
        assertEquals(1, def.getTasks().size());
    }

    @Test
    public void testFromWorkflowDef() {
        WorkflowDef def = new WorkflowDef();
        def.setName("from_def_wf");
        def.setVersion(3);
        def.setDescription("From def test");
        def.setOwnerEmail("owner@company.com");
        def.setFailureWorkflow("on_fail");
        def.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY);
        def.setTimeoutSeconds(600L);
        def.setRestartable(false);
        Map<String, Object> vars = new HashMap<>();
        vars.put("v1", "x");
        def.setVariables(vars);
        Map<String, Object> outParams = new HashMap<>();
        outParams.put("outKey", "outVal");
        def.setOutputParameters(outParams);
        Map<String, Object> inputTemplate = new HashMap<>();
        inputTemplate.put("in1", "default1");
        def.setInputTemplate(inputTemplate);

        WorkflowTask wt = new WorkflowTask();
        wt.setName("simple_task");
        wt.setTaskReferenceName("simple_ref");
        wt.setType("SIMPLE");
        def.getTasks().add(wt);

        ConductorWorkflow<Object> wf = ConductorWorkflow.fromWorkflowDef(def);

        assertEquals("from_def_wf", wf.getName());
        assertEquals(3, wf.getVersion());
        assertEquals("From def test", wf.getDescription());
        assertEquals("owner@company.com", wf.getOwnerEmail());
        assertEquals("on_fail", wf.getFailureWorkflow());
        assertEquals(WorkflowDef.TimeoutPolicy.ALERT_ONLY, wf.getTimeoutPolicy());
        assertEquals(600L, wf.getTimeoutSeconds());
        assertFalse(wf.isRestartable());
        assertNotNull(wf.getVariables());
        assertNotNull(wf.getWorkflowOutput());
        assertEquals("outVal", wf.getWorkflowOutput().get("outKey"));
        assertNotNull(wf.getDefaultInput());
    }

    @Test
    public void testEqualsAndHashCode() {
        ConductorWorkflow<Object> wf1 = new ConductorWorkflow<>(null);
        wf1.setName("same_name");
        wf1.setVersion(1);

        ConductorWorkflow<Object> wf2 = new ConductorWorkflow<>(null);
        wf2.setName("same_name");
        wf2.setVersion(1);

        ConductorWorkflow<Object> wf3 = new ConductorWorkflow<>(null);
        wf3.setName("same_name");
        wf3.setVersion(2);

        ConductorWorkflow<Object> wf4 = new ConductorWorkflow<>(null);
        wf4.setName("different_name");
        wf4.setVersion(1);

        // Same name and version are equal
        assertEquals(wf1, wf2);
        assertEquals(wf1.hashCode(), wf2.hashCode());

        // Different version
        assertNotEquals(wf1, wf3);

        // Different name
        assertNotEquals(wf1, wf4);

        // Self equality
        assertEquals(wf1, wf1);

        // Null and other type
        assertNotEquals(wf1, null);
        assertNotEquals(wf1, "not a workflow");
    }

    @Test
    public void testToString() {
        ConductorWorkflow<Object> wf = new ConductorWorkflow<>(null);
        wf.setName("str_wf");
        wf.setVersion(1);
        wf.add(new SimpleTask("t", "t_ref"));

        String json = wf.toString();
        assertNotNull(json);
        assertTrue(json.contains("str_wf"));
        assertTrue(json.contains("t_ref"));
    }

    @Test
    public void testStaticInputOutputFields() {
        assertNotNull(ConductorWorkflow.input);
        assertNotNull(ConductorWorkflow.output);

        // Verify they produce correct expressions
        assertEquals("${workflow.input.key}", ConductorWorkflow.input.get("key"));
        assertEquals("${workflow.output.result}", ConductorWorkflow.output.get("result"));
        assertEquals("${workflow.input}", ConductorWorkflow.input.getParent());
        assertEquals("${workflow.output}", ConductorWorkflow.output.getParent());
    }

    @Test
    public void testDefaultRestartable() {
        ConductorWorkflow<Object> wf = new ConductorWorkflow<>(null);
        assertTrue(wf.isRestartable(), "New workflow should be restartable by default");
    }
}
