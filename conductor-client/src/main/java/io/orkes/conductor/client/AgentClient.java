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

import java.util.Map;

import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.exceptions.AgentNotFoundException;
import io.orkes.conductor.client.exceptions.SSEUnavailableException;
import io.orkes.conductor.client.model.agent.AgentRequest;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;
import io.orkes.conductor.client.model.agent.CompileResponse;
import io.orkes.conductor.client.model.agent.RespondBody;
import io.orkes.conductor.client.model.agent.StartResponse;

/**
 * Client for the agent control-plane ({@code /api/agent/*}).
 *
 * <p>Follows the SDK's interface + Orkes-implementation convention
 * ({@code SchedulerClient} / {@code http.OrkesSchedulerClient}); obtain an
 * instance from {@link OrkesClients#getAgentClient()}. Standard Conductor
 * endpoints ({@code /api/workflow/*}, {@code /api/tasks}, etc.) are handled by
 * the SDK's own typed clients ({@code WorkflowClient}, {@code TaskClient},
 * {@code MetadataClient}).
 *
 * <p>Server errors surface as the agent SDK's typed exceptions:
 * {@link AgentNotFoundException} for HTTP 404, {@link AgentAPIException}
 * otherwise.
 */
public interface AgentClient extends AutoCloseable {

    /** {@code POST /api/agent/compile} — compile agent config to a workflow def. */
    CompileResponse compileAgent(AgentRequest request);

    /** {@code POST /api/agent/deploy} — compile + register, no execution. */
    StartResponse deployAgent(AgentRequest request);

    /** {@code POST /api/agent/start} — compile + register + start an execution. */
    StartResponse startAgent(AgentRequest request);

    /** {@code GET /api/agent/{executionId}/status} — fetch execution status. */
    AgentStatusResponse getAgentStatus(String executionId);

    /**
     * {@code GET /api/agent/execution/{executionId}} — fetch the full execution
     * tree. Returned as the server's raw JSON shape (no client-side DTO).
     */
    Map<String, Object> getExecution(String executionId);

    /**
     * {@code GET /api/agent/executions} — search executions. {@code params} are
     * passed through as query parameters ({@code null} for none). Returned as
     * the server's raw JSON shape.
     */
    Map<String, Object> listExecutions(Map<String, Object> params);

    /** {@code POST /api/agent/{executionId}/respond} — respond to a waiting HITL task. */
    void respond(String executionId, RespondBody body);

    /** {@code POST /api/agent/{executionId}/stop} — graceful deterministic stop. */
    void stopAgent(String executionId);

    /** {@code POST /api/agent/{executionId}/signal} — inject persistent context. */
    void signalAgent(String executionId, String message);

    /**
     * {@code GET /api/agent/stream/{executionId}} — open the SSE event stream.
     *
     * <p>Returns a connected {@link SseClient}; consume via
     * {@link SseClient#nextEvent()}. On mid-stream drops the client reconnects
     * with a {@code Last-Event-ID} header. Throws
     * {@link SSEUnavailableException} when the server rejects streaming
     * outright — callers should degrade to status polling.
     *
     * @param executionId the execution to stream
     * @param lastEventId resume point for reconnects ({@code null} to start fresh)
     */
    SseClient streamSse(String executionId, String lastEventId);

    /**
     * Release any transport resources held by this client. The shared
     * {@code ConductorClient} (and its HTTP pool) is owned by the caller and is
     * not closed here.
     */
    @Override
    void close();
}
