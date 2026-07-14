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
package io.orkes.conductor.client.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.exceptions.AgentNotFoundException;
import io.orkes.conductor.client.exceptions.SSEUnavailableException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-shape and error-contract tests for {@link OrkesAgentClient} against a
 * stub server. Proves the spec R1 client surface: every {@code /agent/*}
 * control-plane operation rides the shared {@link ConductorClient} transport,
 * with 404 mapped to {@link AgentNotFoundException} and other errors to
 * {@link AgentAPIException}.
 */
public class OrkesAgentClientTest {

    private MockWebServer server;
    private OrkesAgentClient agentClient;

    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        ApiClient apiClient = ApiClient.builder()
                .basePath(server.url("/api").toString())
                .build();
        agentClient = new OrkesAgentClient(apiClient);
    }

    @AfterEach
    public void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "expected a request to reach the stub server");
        return request;
    }

    @Test
    public void getAgentClientReturnsInterfaceImplementation() {
        OrkesClients clients = new OrkesClients(new ConductorClient(server.url("/api").toString()));
        AgentClient fromFactory = clients.getAgentClient();
        assertInstanceOf(OrkesAgentClient.class, fromFactory);
    }

    @Test
    public void getExecutionHitsExecutionPathAndReturnsRawMap() throws InterruptedException {
        server.enqueue(json("{\"workflowId\":\"e1\",\"status\":\"RUNNING\"}"));

        Map<String, Object> execution = agentClient.getExecution("e1");

        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/agent/execution/e1", request.getPath());
        assertEquals("e1", execution.get("workflowId"));
        assertEquals("RUNNING", execution.get("status"));
    }

    @Test
    public void listExecutionsPassesQueryParams() throws InterruptedException {
        server.enqueue(json("{\"results\":[],\"totalHits\":0}"));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", "RUNNING");
        params.put("size", 10);
        Map<String, Object> page = agentClient.listExecutions(params);

        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        String path = request.getPath();
        assertTrue(path.startsWith("/api/agent/executions?"), "unexpected path: " + path);
        assertTrue(path.contains("status=RUNNING"), "missing status param: " + path);
        assertTrue(path.contains("size=10"), "missing size param: " + path);
        assertEquals(0, page.get("totalHits"));
    }

    @Test
    public void listExecutionsWithoutParamsHitsBarePath() throws InterruptedException {
        server.enqueue(json("{\"results\":[]}"));

        agentClient.listExecutions(null);

        RecordedRequest request = takeRequest();
        assertEquals("/api/agent/executions", request.getPath());
    }

    @Test
    public void stopAgentPostsWithNoBody() throws InterruptedException {
        server.enqueue(json("{}"));

        agentClient.stopAgent("e1");

        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/agent/e1/stop", request.getPath());
        assertEquals("", request.getBody().readUtf8());
    }

    @Test
    public void signalAgentPostsMessageBody() throws InterruptedException {
        server.enqueue(json("{}"));

        agentClient.signalAgent("e1", "focus on security");

        RecordedRequest request = takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/agent/e1/signal", request.getPath());
        assertEquals("{\"message\":\"focus on security\"}", request.getBody().readUtf8());
    }

    @Test
    public void notFoundMapsToAgentNotFoundException() {
        server.enqueue(json("{\"message\":\"no such execution\"}").setResponseCode(404));

        assertThrows(AgentNotFoundException.class, () -> agentClient.getExecution("missing"));
    }

    @Test
    public void serverErrorMapsToAgentAPIException() {
        server.enqueue(json("{\"message\":\"boom\"}").setResponseCode(500));

        AgentAPIException ex = assertThrows(AgentAPIException.class, () -> agentClient.stopAgent("e1"));
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    public void streamSseRequiresApiClientTransport() {
        OrkesAgentClient plainTransport =
                new OrkesAgentClient(new ConductorClient(server.url("/api").toString()));

        assertThrows(SSEUnavailableException.class, () -> plainTransport.streamSse("e1", null));
    }

    @Test
    public void closeIsNoOpAndClientRemainsUsable() throws InterruptedException {
        agentClient.close();

        server.enqueue(json("{\"workflowId\":\"e2\"}"));
        Map<String, Object> execution = agentClient.getExecution("e2");

        takeRequest();
        assertEquals("e2", execution.get("workflowId"));
    }
}
