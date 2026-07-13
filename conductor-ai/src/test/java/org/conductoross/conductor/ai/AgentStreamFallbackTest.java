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
import java.util.concurrent.atomic.AtomicInteger;

import org.conductoross.conductor.ai.enums.AgentStatus;
import org.conductoross.conductor.ai.model.AgentEvent;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.AgentStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Streaming degradation (spec R4 {@code streamingEnabled} + the
 * {@code SSEUnavailableException} contract): when streaming is disabled by
 * config or the server rejects the SSE connection, {@code stream()} returns a
 * polling-mode {@link AgentStream} whose {@code getResult()} reaches the real
 * terminal status — never a silently-empty COMPLETED.
 */
@Timeout(30)
class AgentStreamFallbackTest {

    private MockWebServer server;
    private final AtomicInteger sseHits = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() != null ? request.getPath() : "";
                if (path.startsWith("/api/agent/start")) {
                    return json("{\"executionId\":\"exec-1\"}");
                }
                if (path.startsWith("/api/agent/stream/")) {
                    sseHits.incrementAndGet();
                    return new MockResponse().setResponseCode(503);
                }
                if (path.equals("/api/agent/exec-1/status")) {
                    return json("{\"executionId\":\"exec-1\",\"status\":\"COMPLETED\",\"isComplete\":true,"
                            + "\"isRunning\":false,\"output\":{\"result\":\"done!\"}}");
                }
                return json("[]");
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private AgentRuntime runtime(AgentConfig config) {
        return new AgentRuntime(AgentRuntime.client(server.url("/").toString()), config.autoStartWorkers(false));
    }

    private static Agent agent() {
        return Agent.builder()
                .name("test_agent")
                .model("openai/gpt-4o")
                .instructions("You are a test agent.")
                .build();
    }

    @Test
    void streamingDisabledSkipsSseAndPolls() {
        AgentRuntime runtime = runtime(new AgentConfig().streamingEnabled(false));
        try (AgentStream stream = runtime.stream(agent(), "hi")) {
            int events = 0;
            for (AgentEvent event : stream) {
                events++;
            }
            assertEquals(0, events, "polling mode has no event transport");

            AgentResult result = stream.getResult();
            assertEquals(AgentStatus.COMPLETED, result.getStatus(), "getResult must poll the real status");
            assertEquals(0, sseHits.get(), "streamingEnabled=false must not touch the SSE endpoint");
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void sseRejectionFallsBackToPolling() {
        AgentRuntime runtime = runtime(new AgentConfig());
        try (AgentStream stream = runtime.stream(agent(), "hi")) {
            AgentResult result = stream.getResult();

            assertEquals(
                    AgentStatus.COMPLETED,
                    result.getStatus(),
                    "COUNTERFACTUAL: pre-fix, a rejected SSE connect surfaced as a silently-empty stream");
            assertTrue(sseHits.get() >= 1, "the SSE endpoint must have been attempted");
        } finally {
            runtime.shutdown();
        }
    }
}
