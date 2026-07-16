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
package org.conductoross.conductor.ai.model;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.netflix.conductor.client.http.WorkflowClient;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.http.OrkesAgentClient;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Handle lifecycle verbs (Python parity): {@code send} delivers the message on
 * the wire (the pre-fix placeholder silently discarded it), {@code stop} is the
 * graceful control-plane stop with a best-effort unblock, and pause/resume/
 * cancel delegate to the standard workflow endpoints.
 */
@Timeout(30)
class AgentHandleVerbsTest {

    private MockWebServer server;
    private AgentHandle handle;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        ApiClient apiClient = ApiClient.builder()
                .basePath(server.url("/api").toString())
                .build();
        handle = new AgentHandle("exec-1", new OrkesAgentClient(apiClient), new WorkflowClient(apiClient));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "expected a request to reach the stub server");
        return request;
    }

    @Test
    void sendDeliversTheMessage() throws InterruptedException {
        server.enqueue(json("{}"));

        handle.send("hello agent");

        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/agent/exec-1/respond", request.getPath());
        assertEquals(
                "{\"message\":\"hello agent\"}",
                request.getBody().readUtf8(),
                "COUNTERFACTUAL: the pre-fix placeholder sent an empty approve and discarded the message");
    }

    @Test
    void stopPostsStopThenBestEffortSignal() throws InterruptedException {
        server.enqueue(json("{}"));
        server.enqueue(json("{}"));

        handle.stop();

        assertEquals("/api/agent/exec-1/stop", takeRequest().getPath());
        assertEquals("/api/agent/exec-1/signal", takeRequest().getPath());
    }

    @Test
    void stopSwallowsSignalFailure() {
        server.enqueue(json("{}"));
        server.enqueue(json("{\"message\":\"no waiting task\"}").setResponseCode(500));

        assertDoesNotThrow(handle::stop, "the unblock signal is best-effort — the agent may not be waiting");
    }

    @Test
    void pauseResumeCancelDelegateToWorkflowEndpoints() throws InterruptedException {
        server.enqueue(json("{}"));
        server.enqueue(json("{}"));
        server.enqueue(json("{}"));

        handle.pause();
        handle.resume();
        handle.cancel("no longer needed");

        assertEquals("/api/workflow/exec-1/pause", takeRequest().getPath());
        assertEquals("/api/workflow/exec-1/resume", takeRequest().getPath());
        RecordedRequest cancel = takeRequest();
        assertEquals("DELETE", cancel.getMethod());
        assertTrue(
                cancel.getPath().startsWith("/api/workflow/exec-1"),
                "cancel must terminate the workflow: " + cancel.getPath());
        assertTrue(cancel.getPath().contains("reason="), "termination reason must ride the request");
    }

    @Test
    void getStatusReturnsTheServerSnapshot() {
        server.enqueue(json("{\"executionId\":\"exec-1\",\"status\":\"RUNNING\",\"isComplete\":false,"
                + "\"isRunning\":true}"));

        AgentStatusResponse status = handle.getStatus();

        assertEquals("RUNNING", status.getStatus());
    }
}
