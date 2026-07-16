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
package org.conductoross.conductor.ai;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.enums.AgentStatus;
import org.conductoross.conductor.ai.enums.EventType;
import org.conductoross.conductor.ai.model.AgentEvent;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify that all public model classes expose {@code getExecutionId()} (not the
 * legacy {@code getWorkflowId()}) — issue #254.
 */
class ModelExecutionIdTest {

    @Test
    void agentResult_getExecutionId() {
        AgentResult result = new AgentResult(
                Map.of("result", "ok"), "exec-123", AgentStatus.COMPLETED, List.of(), List.of(), null, null);
        assertEquals("exec-123", result.getExecutionId());
    }

    @Test
    void agentResult_executionId_defaults_to_empty() {
        AgentResult result = new AgentResult(null, null, null, null, null, null, null);
        assertEquals("", result.getExecutionId());
    }

    @Test
    void agentEvent_getExecutionId() {
        AgentEvent event =
                new AgentEvent(EventType.TOOL_CALL, null, "my_tool", Map.of(), null, null, "exec-456", null, null);
        assertEquals("exec-456", event.getExecutionId());
    }

    @Test
    void agentEvent_fromMap_reads_executionId_key() {
        Map<String, Object> data = Map.of(
                "type", "tool_call",
                "toolName", "fetch",
                "executionId", "exec-789");
        AgentEvent event = AgentEvent.fromMap(data);
        assertEquals("exec-789", event.getExecutionId());
    }

    @Test
    void toolContext_getExecutionId() {
        ToolContext ctx = new ToolContext("session-1", "exec-abc", "task-1");
        assertEquals("exec-abc", ctx.getExecutionId());
    }

    @Test
    void toolContext_state_is_mutable() {
        ToolContext ctx = new ToolContext("s", "e", "t");
        ctx.getState().put("key", "value");
        assertEquals("value", ctx.getState().get("key"));
    }
}
