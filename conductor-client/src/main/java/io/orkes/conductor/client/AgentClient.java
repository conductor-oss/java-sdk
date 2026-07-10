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
package io.orkes.conductor.client;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.exceptions.AgentNotFoundException;
import io.orkes.conductor.client.model.agent.AgentRequest;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;
import io.orkes.conductor.client.model.agent.CompileResponse;
import io.orkes.conductor.client.model.agent.RespondBody;
import io.orkes.conductor.client.model.agent.StartResponse;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for the agent control-plane ({@code /api/agent/*}).
 *
 * <p>Strictly scoped to five endpoints — compile, deploy, start, status, respond.
 * Standard Conductor endpoints ({@code /api/workflow/*}, {@code /api/tasks}, etc.)
 * are handled by the SDK's own typed clients ({@code WorkflowClient},
 * {@code TaskClient}, {@code MetadataClient}).
 *
 * <p>Every request goes through the shared {@link ConductorClient}'s native HTTP +
 * auth + serialization layer ({@link ConductorClientRequest} →
 * {@link ConductorClient#execute}). No hand-rolled HTTP. Conductor's
 * {@link ConductorClientException} is mapped to the agent SDK's typed
 * {@link AgentAPIException}/{@link AgentNotFoundException}.
 *
 * <p>Paths are relative to the client's base path (the server's {@code /api}
 * root), so {@code "/agent/start"} resolves to {@code /api/agent/start}.
 */
public class AgentClient {

    private static final TypeReference<CompileResponse> COMPILE_TYPE = new TypeReference<CompileResponse>() {};
    private static final TypeReference<StartResponse> START_TYPE = new TypeReference<StartResponse>() {};
    private static final TypeReference<AgentStatusResponse> STATUS_TYPE = new TypeReference<AgentStatusResponse>() {};

    protected final ConductorClient client;

    public AgentClient(ConductorClient client) {
        this.client = client;
    }

    /** {@code POST /api/agent/compile} — compile agent config to a workflow def. */
    public CompileResponse compileAgent(AgentRequest request) {
        return post("/agent/compile", request, COMPILE_TYPE);
    }

    /** {@code POST /api/agent/deploy} — compile + register, no execution. */
    public StartResponse deployAgent(AgentRequest request) {
        return post("/agent/deploy", request, START_TYPE);
    }

    /** {@code POST /api/agent/start} — compile + register + start an execution. */
    public StartResponse startAgent(AgentRequest request) {
        return post("/agent/start", request, START_TYPE);
    }

    /** {@code GET /api/agent/{executionId}/status} — fetch execution status. */
    public AgentStatusResponse getAgentStatus(String executionId) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/agent/{executionId}/status")
                .addPathParam("executionId", executionId)
                .build();
        return executeFor(req, STATUS_TYPE);
    }

    /** {@code POST /api/agent/{executionId}/respond} — respond to a waiting HITL task. */
    public void respond(String executionId, RespondBody body) {
        ConductorClientRequest req = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/agent/{executionId}/respond")
                .addPathParam("executionId", executionId)
                .body(body)
                .build();
        try {
            client.execute(req);
        } catch (ConductorClientException e) {
            throw mapException(e);
        }
    }

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
