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
package org.conductoross.conductor.ai;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Spec R8 (T12/T13) at the wire level: {@link RunSettings} overrides mutate the
 * serialized {@code agentConfig} in the {@code POST /agent/start} payload —
 * full, partial, zero-value, absent, through the async variant, and pulled out
 * of the drop-in varargs (where they would otherwise be swallowed as a tool).
 */
@Timeout(30)
class AgentRuntimeRunSettingsWireTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer server;
    private AgentRuntime runtime;
    private Agent agent;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().startsWith("/api/agent/start")) {
                    return json("{\"executionId\":\"exec-1\"}");
                }
                return json("[]");
            }
        });
        server.start();
        runtime = new AgentRuntime(
                TestClients.forUrl(server.url("/").toString()),
                new AgentConfig().autoStartWorkers(false));
        agent = Agent.builder()
                .name("test_agent")
                .model("openai/gpt-4o")
                .instructions("You are a test agent.")
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        runtime.shutdown();
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> takeStartAgentBody() throws Exception {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "expected the start request to reach the stub server");
        assertEquals("/api/agent/start", request.getPath());
        return MAPPER.readValue(request.getBody().readUtf8(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> takeStartAgentConfig() throws Exception {
        Map<String, Object> body = takeStartAgentBody();
        Map<String, Object> agentConfig = (Map<String, Object>) body.get("agentConfig");
        assertNotNull(agentConfig, "start payload must carry the serialized agentConfig");
        return agentConfig;
    }

    @Test
    void overridesApplyIncludingZeroValues() throws Exception {
        runtime.start(agent, "hi", new RunSettings()
                .model("override/model")
                .temperature(0.0)
                .maxTokens(0));

        Map<String, Object> agentConfig = takeStartAgentConfig();
        assertEquals("override/model", agentConfig.get("model"));
        assertEquals(0.0, agentConfig.get("temperature"), "temperature=0.0 must apply — the gate is != null");
        assertEquals(0, agentConfig.get("maxTokens"), "maxTokens=0 must apply — the gate is != null");
    }

    @Test
    void partialOverridesKeepAgentValues() throws Exception {
        runtime.start(agent, "hi", new RunSettings()
                .reasoningEffort("low")
                .thinkingBudgetTokens(256));

        Map<String, Object> agentConfig = takeStartAgentConfig();
        assertEquals("openai/gpt-4o", agentConfig.get("model"), "unset fields must not override the agent");
        assertEquals("low", agentConfig.get("reasoningEffort"));
        assertEquals(Map.of("enabled", true, "budgetTokens", 256), agentConfig.get("thinkingConfig"));
    }

    @Test
    void noSettingsLeavesConfigUntouched() throws Exception {
        runtime.start(agent, "hi");

        Map<String, Object> body = takeStartAgentBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> agentConfig = (Map<String, Object>) body.get("agentConfig");
        assertEquals("openai/gpt-4o", agentConfig.get("model"));
        assertFalse(agentConfig.containsKey("thinkingConfig"));
        assertFalse(agentConfig.containsKey("reasoningEffort"));
        assertFalse(body.containsKey("idempotencyKey"));
    }

    @Test
    void idempotencyKeyIsTopLevelExecutionMetadata() throws Exception {
        runtime.start(agent, "hi", new RunSettings().idempotencyKey("logical-run-123"));

        Map<String, Object> body = takeStartAgentBody();
        assertEquals("logical-run-123", body.get("idempotencyKey"));
        @SuppressWarnings("unchecked")
        Map<String, Object> agentConfig = (Map<String, Object>) body.get("agentConfig");
        assertFalse(agentConfig.containsKey("idempotencyKey"));
    }

    @Test
    void blankIdempotencyKeyIsOmitted() throws Exception {
        runtime.start(agent, "hi", new RunSettings().idempotencyKey("   "));

        assertFalse(takeStartAgentBody().containsKey("idempotencyKey"));
    }

    @Test
    void asyncVariantForwardsSettings() throws Exception {
        runtime.startAsync(agent, "hi", new RunSettings().model("async/model")).join();

        Map<String, Object> agentConfig = takeStartAgentConfig();
        assertEquals("async/model", agentConfig.get("model"));
    }

    @Test
    void dropInVarargsExtractRunSettings() throws Exception {
        // Through the Object drop-in, RunSettings arrives in the tools varargs —
        // it must be applied as overrides, not coerced (and dropped) as a tool.
        runtime.start((Object) agent, "hi", new RunSettings()
                .model("varargs/model")
                .idempotencyKey("varargs-run-123"));

        Map<String, Object> body = takeStartAgentBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> agentConfig = (Map<String, Object>) body.get("agentConfig");
        assertEquals("varargs/model", agentConfig.get("model"));
        assertEquals("varargs-run-123", body.get("idempotencyKey"));
    }
}
