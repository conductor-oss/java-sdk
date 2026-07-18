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
package io.orkes.conductor.client.model.agent;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code GET /api/agent/{executionId}/status}.
 *
 * <p>Polled by {@code AgentHandle} until the
 * execution reaches a terminal status. Used internally — callers receive an
 * {@code AgentResult} after completion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AgentStatusResponse {

    private String executionId;

    private String status;

    @JsonProperty("isComplete")
    private boolean complete;

    @JsonProperty("isRunning")
    private boolean running;

    @JsonProperty("isWaiting")
    private boolean waiting;

    private Map<String, Object> output;

    private String reasonForIncompletion;

    private PendingTool pendingTool;

    public AgentStatusResponse() {}

    public String getExecutionId() {
        return executionId;
    }

    /**
     * Conductor workflow status string: {@code RUNNING}, {@code COMPLETED},
     * {@code FAILED}, {@code TERMINATED}, {@code TIMED_OUT}, {@code PAUSED}.
     */
    public String getStatus() {
        return status;
    }

    /** {@code true} when status is terminal (COMPLETED, FAILED, TERMINATED, TIMED_OUT). */
    public boolean isComplete() {
        return complete;
    }

    public boolean isRunning() {
        return running;
    }

    /** {@code true} when a HITL task is paused waiting for human input. */
    public boolean isWaiting() {
        return waiting;
    }

    /**
     * Final workflow output. Only present when {@link #isComplete()} is {@code true}.
     */
    public Map<String, Object> getOutput() {
        return output;
    }

    /**
     * Failure or termination reason. Only present for non-COMPLETED terminal runs.
     */
    public String getReasonForIncompletion() {
        return reasonForIncompletion;
    }

    /**
     * Details of the paused HITL task. Only present when {@link #isWaiting()} is {@code true}.
     */
    public PendingTool getPendingTool() {
        return pendingTool;
    }

    @Override
    public String toString() {
        return "AgentStatusResponse{executionId=" + executionId + ", status=" + status + ", complete=" + complete
                + ", waiting=" + waiting + "}";
    }
}
