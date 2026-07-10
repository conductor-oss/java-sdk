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
package org.conductoross.conductor.ai.internal;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/agent/deploy} and {@code POST /api/agent/start}.
 *
 * <p>For deploy, {@link #getExecutionId()} is {@code null} — no execution was started.
 * For start, {@link #getExecutionId()} is the Conductor workflow ID to pass to
 * {@link AgentClient#getAgentStatus(String)} and {@link AgentClient#respond(String, java.util.Map)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StartResponse {

    /** Current canonical field name. {@code @JsonAlias} handles older server versions. */
    @JsonProperty("executionId")
    @JsonAlias({"workflowId", "id", "correlationId"})
    private String executionId;

    @JsonProperty("agentName")
    private String agentName;

    @JsonProperty("requiredWorkers")
    private List<String> requiredWorkers;

    public StartResponse() {}

    /** Conductor workflow ID for this execution, or {@code null} for deploy-only calls. */
    public String getExecutionId() {
        return executionId;
    }

    /** The registered workflow name on the server. */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Task type names the SDK must have workers polling before the agent can progress.
     * Handled automatically by the runtime.
     */
    public List<String> getRequiredWorkers() {
        return requiredWorkers != null ? requiredWorkers : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "StartResponse{executionId=" + executionId + ", agentName=" + agentName + "}";
    }
}
