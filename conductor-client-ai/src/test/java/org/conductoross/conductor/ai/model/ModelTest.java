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
package org.conductoross.conductor.ai.model;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.enums.AgentStatus;
import org.conductoross.conductor.ai.enums.EventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the model POJOs/value types (no server). */
class ModelTest {

    @Test
    void agentResultGettersAndNullDefaults() {
        TokenUsage usage = new TokenUsage(10, 20, 30);
        AgentResult r = new AgentResult(
                "out", "exec-1", AgentStatus.COMPLETED, List.of(Map.of("name", "t")), List.of(), usage, null);
        assertEquals("out", r.getOutput());
        assertEquals("exec-1", r.getExecutionId());
        assertEquals(AgentStatus.COMPLETED, r.getStatus());
        assertEquals(1, r.getToolCalls().size());
        assertSame(usage, r.getTokenUsage());

        // null status/lists → safe defaults
        AgentResult d = new AgentResult(null, "e", null, null, null, null, null);
        assertEquals(AgentStatus.COMPLETED, d.getStatus());
        assertNotNull(d.getToolCalls());
        assertTrue(d.getToolCalls().isEmpty());
        assertNotNull(d.getEvents());
    }

    @Test
    void agentEventDirectConstructor() {
        AgentEvent e =
                new AgentEvent(EventType.MESSAGE, "hello", "calc", Map.of("x", 1), "res", "outp", "exec-9", null, null);
        assertEquals(EventType.MESSAGE, e.getType());
        assertEquals("hello", e.getContent());
        assertEquals("calc", e.getToolName());
        assertEquals(1, e.getArgs().get("x"));
        assertEquals("exec-9", e.getExecutionId());
    }

    @Test
    void agentEventFromMapParsesContent() {
        AgentEvent e = AgentEvent.fromMap(Map.of("content", "hi there"));
        assertEquals("hi there", e.getContent());
    }

    @Test
    void tokenUsage() {
        TokenUsage u = new TokenUsage(7, 11, 18);
        assertEquals(7, u.getPromptTokens());
        assertEquals(11, u.getCompletionTokens());
        assertEquals(18, u.getTotalTokens());
    }

    @Test
    void deploymentInfo() {
        DeploymentInfo d = new DeploymentInfo("agent-1_wf", "agent-1");
        assertEquals("agent-1_wf", d.getRegisteredName());
        assertEquals("agent-1", d.getAgentName());
    }

    @Test
    void promptTemplateOverloads() {
        assertEquals("p", new PromptTemplate("p").getName());
        PromptTemplate withVars = new PromptTemplate("p", Map.of("k", "v"));
        assertEquals("v", withVars.getVariables().get("k"));
        PromptTemplate versioned = new PromptTemplate("p", Map.of(), 3);
        assertEquals(3, versioned.getVersion());
    }

    @Test
    void guardrailResultFactories() {
        assertTrue(GuardrailResult.pass().isPassed());
        GuardrailResult failed = GuardrailResult.fail("bad");
        assertFalse(failed.isPassed());
        assertEquals("bad", failed.getMessage());
        assertEquals("clean", GuardrailResult.fix("clean").getFixedOutput());
    }

    @Test
    void prefillToolCall() {
        PrefillToolCall p = PrefillToolCall.of("git_status", Map.of("dir", "/tmp"));
        assertEquals("git_status", p.getToolName());
        assertEquals("/tmp", p.getArguments().get("dir"));
    }

    @Test
    void toolContextStateIsMutable() {
        ToolContext ctx = new ToolContext("s", "e", "t");
        assertEquals("e", ctx.getExecutionId());
        ctx.getState().put("repo", "x/y");
        assertEquals("x/y", ctx.getState().get("repo"));
    }
}
