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
package io.orkes.conductor.client.http;

import java.util.Collections;
import java.util.Map;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.SseClient;
import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.exceptions.AgentNotFoundException;
import io.orkes.conductor.client.exceptions.SSEUnavailableException;
import io.orkes.conductor.client.model.agent.AgentRequest;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;
import io.orkes.conductor.client.model.agent.CompileResponse;
import io.orkes.conductor.client.model.agent.RespondBody;
import io.orkes.conductor.client.model.agent.StartResponse;

import tools.jackson.core.type.TypeReference;

/**
 * Orkes implementation of {@link AgentClient} for the agent control-plane
 * ({@code /api/agent/*}).
 *
 * <p>Every request goes through the shared {@link ConductorClient}'s native
 * HTTP + auth + serialization layer ({@link ConductorClientRequest} →
 * {@link ConductorClient#execute}). No hand-rolled HTTP. Conductor's
 * {@link ConductorClientException} is mapped to the agent SDK's typed
 * {@link AgentAPIException}/{@link AgentNotFoundException}.
 *
 * <p>Paths are relative to the client's base path (the server's {@code /api}
 * root), so {@code "/agent/start"} resolves to {@code /api/agent/start}.
 */
public class OrkesAgentClient implements AgentClient {

    private static final TypeReference<CompileResponse> COMPILE_TYPE = new TypeReference<CompileResponse>() {};
    private static final TypeReference<StartResponse> START_TYPE = new TypeReference<StartResponse>() {};
    private static final TypeReference<AgentStatusResponse> STATUS_TYPE = new TypeReference<AgentStatusResponse>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    protected final ConductorClient client;

    public OrkesAgentClient(ConductorClient client) {
        this.client = client;
    }

    @Override
    public CompileResponse compileAgent(AgentRequest request) {
        return post("/agent/compile", request, COMPILE_TYPE);
    }

    @Override
    public StartResponse deployAgent(AgentRequest request) {
        return post("/agent/deploy", request, START_TYPE);
    }

    @Override
    public StartResponse startAgent(AgentRequest request) {
        return post("/agent/start", request, START_TYPE);
    }

    @Override
    public AgentStatusResponse getAgentStatus(String executionId) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/agent/{executionId}/status")
                .addPathParam("executionId", executionId)
                .build();
        return executeFor(req, STATUS_TYPE);
    }

    @Override
    public Map<String, Object> getExecution(String executionId) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/agent/execution/{executionId}")
                .addPathParam("executionId", executionId)
                .build();
        Map<String, Object> result = executeFor(req, MAP_TYPE);
        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> listExecutions(Map<String, Object> params) {
        ConductorClientRequest.Builder builder = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/agent/executions");
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    builder.addQueryParam(entry.getKey(), value.toString());
                }
            }
        }
        Map<String, Object> result = executeFor(builder.build(), MAP_TYPE);
        return result != null ? result : Collections.emptyMap();
    }

    @Override
    public void respond(String executionId, RespondBody body) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/agent/{executionId}/respond")
                .addPathParam("executionId", executionId)
                .body(body)
                .build();
        execute(req);
    }

    @Override
    public void stopAgent(String executionId) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/agent/{executionId}/stop")
                .addPathParam("executionId", executionId)
                .build();
        execute(req);
    }

    @Override
    public void cancelAgent(String executionId, String reason) {
        ConductorClientRequest.Builder builder = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/agent/{executionId}/cancel")
                .addPathParam("executionId", executionId);
        if (reason != null && !reason.isBlank()) {
            builder.addQueryParam("reason", reason);
        }
        execute(builder.build());
    }

    @Override
    public void signalAgent(String executionId, String message) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/agent/{executionId}/signal")
                .addPathParam("executionId", executionId)
                .body(Collections.singletonMap("message", message))
                .build();
        execute(req);
    }

    @Override
    public SseClient streamSse(String executionId, String lastEventId) {
        if (!(client instanceof ApiClient)) {
            throw new SSEUnavailableException(
                    "SSE streaming requires the Orkes ApiClient transport; got "
                            + client.getClass().getName());
        }
        SseClient sseClient = new SseClient((ApiClient) client, executionId, lastEventId);
        sseClient.connect();
        return sseClient;
    }

    /** No-op: the shared {@link ConductorClient} owns the HTTP transports. */
    @Override
    public void close() {}

    // ── internals ──────────────────────────────────────────────────────────

    private <T> T post(String path, Object payload, TypeReference<T> type) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.POST)
                .path(path)
                .body(payload)
                .build();
        return executeFor(req, type);
    }

    private <T> T executeFor(ConductorClientRequest req, TypeReference<T> type) {
        try {
            ConductorClientResponse<T> resp = client.execute(req, type);
            return resp.getData();
        } catch (ConductorClientException e) {
            throw mapException(e);
        }
    }

    private void execute(ConductorClientRequest req) {
        try {
            client.execute(req);
        } catch (ConductorClientException e) {
            throw mapException(e);
        }
    }

    /** Preserve the agent SDK's typed error contract over Conductor's exception. */
    private static RuntimeException mapException(ConductorClientException e) {
        int status = e.getStatus();
        String body = e.getMessage();
        if (status == 404) {
            return new AgentNotFoundException(status, body);
        }
        return new AgentAPIException(status, body);
    }
}
