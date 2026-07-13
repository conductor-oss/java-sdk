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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Spec R9 (T14): {@code serve} = deploy + serve. Each served agent is deployed
 * (one {@code POST /agent/deploy} per agent) before workers start, and
 * {@code blocking=false} returns to the caller once workers are polling.
 */
@Timeout(30)
class AgentRuntimeServeTest {

    private MockWebServer server;
    private AgentRuntime runtime;
    private final List<String> agentApiCalls = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() != null ? request.getPath() : "";
                if (path.startsWith("/api/agent/")) {
                    agentApiCalls.add(request.getMethod() + " " + path);
                }
                if (path.equals("/api/agent/deploy")) {
                    return json("{\"agentName\":\"registered_agent\"}");
                }
                return json("[]");
            }
        });
        server.start();
        runtime = new AgentRuntime(AgentRuntime.client(server.url("/").toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        runtime.shutdown();
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private static Agent agent(String name) {
        return Agent.builder()
                .name(name)
                .model("openai/gpt-4o")
                .instructions("You are a test agent.")
                .build();
    }

    private long deployCount() {
        return agentApiCalls.stream()
                .filter("POST /api/agent/deploy"::equals)
                .count();
    }

    @Test
    void nonBlockingServeDeploysThenReturns() {
        // COUNTERFACTUAL (pre-fix): serve never deployed, so a bare
        // serve(agent) required a separate deploy() call to be startable —
        // and there was no non-blocking form at all (the call joined forever).
        runtime.serve(false, agent("serve_agent"));

        assertEquals(1, deployCount(), "serve must deploy the agent (serve = deploy + serve, spec R9)");
    }

    @Test
    void serveDeploysEveryAgentOnce() {
        runtime.serve(false, agent("agent_one"), agent("agent_two"));

        assertEquals(2, deployCount(), "one deploy per served agent");
    }

    @Test
    void serveWithoutAgentsStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> runtime.serve(false));
    }
}
