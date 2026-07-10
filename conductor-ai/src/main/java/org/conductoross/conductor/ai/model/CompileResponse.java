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
package org.conductoross.conductor.ai.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from {@code POST /api/agent/compile}.
 *
 * <p>Returned by {@link org.conductoross.conductor.ai.AgentRuntime#plan(org.conductoross.conductor.ai.Agent)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CompileResponse {

    @JsonProperty("workflowDef")
    private Map<String, Object> workflowDef;

    @JsonProperty("requiredWorkers")
    private List<String> requiredWorkers;

    public CompileResponse() {}

    /** The compiled Conductor workflow definition. */
    public Map<String, Object> getWorkflowDef() {
        return workflowDef != null ? workflowDef : Collections.emptyMap();
    }

    /**
     * Task type names the SDK must register local workers for before the agent
     * can make progress. The SDK handles this automatically inside
     * {@link org.conductoross.conductor.ai.AgentRuntime#run}.
     */
    public List<String> getRequiredWorkers() {
        return requiredWorkers != null ? requiredWorkers : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "CompileResponse{requiredWorkers=" + getRequiredWorkers() + "}";
    }
}
