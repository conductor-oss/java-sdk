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
package io.orkes.conductor.client.model.agent;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentModelJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void serializesAgentRequestWithDefaultNamesAndExplicitWireExceptions() throws Exception {
        AgentRequest request = AgentRequest.frameworkAgent("openai", Map.of("model", "gpt-4o"))
                .prompt("hello")
                .sessionId("session-1")
                .staticPlan(Map.of("step", 1))
                .timeoutSeconds(30)
                .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("openai", json.get("framework").asText());
        assertEquals("gpt-4o", json.get("rawConfig").get("model").asText());
        assertEquals("hello", json.get("prompt").asText());
        assertEquals("session-1", json.get("sessionId").asText());
        assertEquals(1, json.get("static_plan").get("step").asInt());
        assertEquals(30, json.get("timeoutSeconds").asInt());
        assertFalse(json.has("staticPlan"));
        assertFalse(json.has("agentConfig"));
    }

    @Test
    public void deserializesResponsesWithoutRedundantPropertyAnnotations() throws Exception {
        StartResponse start = objectMapper.readValue(
                "{\"executionId\":\"execution-1\",\"agentName\":\"researcher\","
                        + "\"requiredWorkers\":[\"search\"]}",
                StartResponse.class);
        CompileResponse compile = objectMapper.readValue(
                "{\"workflowDef\":{\"name\":\"researcher\"},"
                        + "\"requiredWorkers\":[\"search\"]}",
                CompileResponse.class);

        assertEquals("execution-1", start.getExecutionId());
        assertEquals("researcher", start.getAgentName());
        assertEquals(List.of("search"), start.getRequiredWorkers());
        assertEquals("researcher", compile.getWorkflowDef().get("name"));
        assertEquals(List.of("search"), compile.getRequiredWorkers());
    }

    @Test
    public void retainsAnnotationsWhereJavaAndWireNamesDiffer() throws Exception {
        AgentStatusResponse status = objectMapper.readValue(
                "{\"executionId\":\"execution-1\",\"status\":\"PAUSED\","
                        + "\"isComplete\":false,\"isRunning\":true,\"isWaiting\":true,"
                        + "\"pendingTool\":{\"taskRefName\":\"approve_ref\","
                        + "\"tool_name\":\"approve\",\"parameters\":{\"amount\":42},"
                        + "\"response_schema\":{\"type\":\"object\"},"
                        + "\"response_ui_schema\":{\"widget\":\"approval\"}}}",
                AgentStatusResponse.class);

        assertEquals("execution-1", status.getExecutionId());
        assertTrue(status.isRunning());
        assertTrue(status.isWaiting());
        assertFalse(status.isComplete());
        assertEquals("approve_ref", status.getPendingTool().getTaskRefName());
        assertEquals("approve", status.getPendingTool().getToolName());
        assertEquals(42, status.getPendingTool().getParameters().get("amount"));
    }

    @Test
    public void retainsExecutionIdAliasesWithoutJsonProperty() throws Exception {
        StartResponse response = objectMapper.readValue(
                "{\"workflowId\":\"legacy-execution\",\"agentName\":\"researcher\"}",
                StartResponse.class);

        assertEquals("legacy-execution", response.getExecutionId());
        assertEquals("researcher", response.getAgentName());
    }
}
