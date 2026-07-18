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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentModelJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void serializesDeployedAgentNameAndVersionAtTopLevel() {
        AgentRequest request = AgentRequest.deployedAgent("researcher", 7).build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals(
                objectMapper.createObjectNode().put("name", "researcher").put("version", 7),
                json);
        assertEquals("researcher", request.getName());
        assertEquals(7, request.getVersion());
        assertNull(request.getAgentConfig());
        assertNull(request.getFramework());
        assertNull(request.getRawConfig());

        JsonNode unversioned =
                objectMapper.valueToTree(AgentRequest.deployedAgent("researcher", null).build());
        assertEquals(objectMapper.createObjectNode().put("name", "researcher"), unversioned);
    }

    @Test
    public void serializesModelAndSkillRefWithExactPropertyNames() {
        AgentRequest request =
                AgentRequest.frameworkAgent("skill", null)
                        .model("claude-sonnet")
                        .skillRef(Map.of("name", "code-review", "version", 2))
                        .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("skill", json.get("framework").asText());
        assertEquals("claude-sonnet", json.get("model").asText());
        assertEquals("code-review", json.get("skillRef").get("name").asText());
        assertEquals(2, json.get("skillRef").get("version").asInt());
        assertFalse(json.has("rawConfig"));
        assertFalse(json.has("skill_ref"));
    }

    @Test
    public void nativeAndFrameworkAgentShapesRemainUnchangedAndNullsAreOmitted() {
        JsonNode nativeJson = objectMapper.valueToTree(
                AgentRequest.nativeAgent(Map.of("goal", "review")).build());
        JsonNode frameworkJson = objectMapper.valueToTree(
                AgentRequest.frameworkAgent("openai", Map.of("model", "gpt-4o")).build());

        assertEquals(
                objectMapper.createObjectNode()
                        .set("agentConfig", objectMapper.createObjectNode().put("goal", "review")),
                nativeJson);
        assertEquals(
                objectMapper.createObjectNode()
                        .put("framework", "openai")
                        .set("rawConfig", objectMapper.createObjectNode().put("model", "gpt-4o")),
                frameworkJson);
        for (String field : List.of("name", "version", "model", "skillRef")) {
            assertFalse(nativeJson.has(field));
            assertFalse(frameworkJson.has(field));
        }
    }

    @Test
    public void serializesAgentRequestWithDefaultNamesAndExplicitWireExceptions() throws Exception {
        AgentRequest request = AgentRequest.frameworkAgent("openai", Map.of("model", "gpt-4o"))
                .prompt("hello")
                .sessionId("session-1")
                .staticPlan(Map.of("step", 1))
                .idempotencyKey("logical-run-123")
                .timeoutSeconds(30)
                .build();

        JsonNode json = objectMapper.valueToTree(request);

        assertEquals("openai", json.get("framework").asText());
        assertEquals("gpt-4o", json.get("rawConfig").get("model").asText());
        assertEquals("hello", json.get("prompt").asText());
        assertEquals("session-1", json.get("sessionId").asText());
        assertEquals(1, json.get("static_plan").get("step").asInt());
        assertEquals("logical-run-123", json.get("idempotencyKey").asText());
        assertEquals(30, json.get("timeoutSeconds").asInt());
        assertFalse(json.has("staticPlan"));
        assertFalse(json.has("agentConfig"));
        assertFalse(json.get("rawConfig").has("idempotencyKey"));
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
                        + "\"startTime\":1710000000000,\"endTime\":1710000005000,"
                        + "\"isComplete\":false,\"isRunning\":true,\"isWaiting\":true,"
                        + "\"pendingTool\":{\"taskRefName\":\"approve_ref\","
                        + "\"tool_name\":\"approve\",\"parameters\":{\"amount\":42},"
                        + "\"response_schema\":{\"type\":\"object\"},"
                        + "\"response_ui_schema\":{\"widget\":\"approval\"}}}",
                AgentStatusResponse.class);

        assertEquals("execution-1", status.getExecutionId());
        assertEquals(1710000000000L, status.getStartTime());
        assertEquals(1710000005000L, status.getEndTime());
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

    @Test
    public void serializesRespondBodyThroughLombokGeneratedAccessors() {
        JsonNode approval = objectMapper.valueToTree(RespondBody.approve("looks good"));
        JsonNode custom = objectMapper.valueToTree(RespondBody.of(Map.of("selected", "writer")));

        assertEquals(objectMapper.createObjectNode()
                        .put("approved", true)
                        .put("reason", "looks good"),
                approval);
        assertEquals(objectMapper.createObjectNode().put("selected", "writer"), custom);
        assertFalse(custom.has("extraFields"));
    }
}
