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

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Javascript;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.def.tasks.TaskRegistry;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import static org.junit.jupiter.api.Assertions.*;

class DecisionSwitchTest {
    @Test
    void preservesExplicitDecisionEvaluatorAndQuestions() {
        Switch decision = new Switch("route_request", "route").evaluatorType(Switch.DECISION_NAME)
                .input("model", "jev-1.13").input("state", "${workflow.input.request}")
                .input("questions", Map.of("route", Map.of("type", "choice")))
                .switchCase("billing", new Javascript("billing", "'invoice'"));
        WorkflowTask task = decision.getWorkflowDefTasks().get(0);
        assertEquals("SWITCH", task.getType());
        assertEquals("decision", task.getEvaluatorType());
        assertEquals("route", task.getExpression());
        assertFalse(task.getInputParameters().containsKey("switchCaseValue"));
        assertEquals("jev-1.13", task.getInputParameters().get("model"));
        assertEquals(1, task.getDecisionCases().get("billing").size());
        assertTrue(task.getDefaultCase().isEmpty());
    }

    @Test
    void roundTripsDecisionAndLegacySwitchesThroughTaskRegistry() {
        WorkflowExecutor.initTaskImplementations();
        for (Switch original : java.util.List.of(
                new Switch("decision", "route").evaluatorType("decision")
                        .input("model", "jev-1.13").switchCase("billing", new Javascript("handle", "'invoice'")),
                new Switch("value", "${workflow.input.route}"),
                new Switch("javascript", "$.route", true))) {
            WorkflowTask before = original.getWorkflowDefTasks().get(0);
            WorkflowTask after = TaskRegistry.getTask(before).getWorkflowDefTasks().get(0);
            assertEquals(before.getEvaluatorType(), after.getEvaluatorType());
            assertEquals(before.getExpression(), after.getExpression());
            assertEquals(before.getInputParameters(), after.getInputParameters());
            assertEquals(before.getDecisionCases(), after.getDecisionCases());
        }
    }

    @Test
    void preservesLegacyValueAndJavascriptSwitches() {
        WorkflowTask value = new Switch("route", "${workflow.input.route}").getWorkflowDefTasks().get(0);
        assertEquals("value-param", value.getEvaluatorType());
        assertEquals("switchCaseValue", value.getExpression());
        assertEquals("${workflow.input.route}", value.getInputParameters().get("switchCaseValue"));
        WorkflowTask javascript = new Switch("route", "$.route", true).getWorkflowDefTasks().get(0);
        assertEquals("javascript", javascript.getEvaluatorType());
        assertEquals("$.route", javascript.getExpression());
        WorkflowTask explicit = new Switch("route", "route", true).evaluatorType("decision")
                .getWorkflowDefTasks().get(0);
        assertEquals("decision", explicit.getEvaluatorType());
    }
}
