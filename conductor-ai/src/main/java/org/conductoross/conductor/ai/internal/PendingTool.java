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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Details of the HITL task that is currently paused, embedded in {@link AgentStatusResponse}.
 *
 * <p>Present only when {@link AgentStatusResponse#isWaiting()} is {@code true}.
 * Pass {@link #getTaskRefName()} back to the server via
 * {@link AgentClient#respond(String, java.util.Map)} to resume execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PendingTool {

    @JsonProperty("taskRefName")
    private String taskRefName;

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("response_schema")
    private Object responseSchema;

    @JsonProperty("response_ui_schema")
    private Object responseUiSchema;

    public PendingTool() {}

    /** Conductor task reference name — echoed back in the respond body when needed. */
    public String getTaskRefName() {
        return taskRefName;
    }

    /** Logical tool name shown to the human reviewer. */
    public String getToolName() {
        return toolName;
    }

    /** Arguments the agent passed to the tool (what the human is being asked to approve). */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /** JSON Schema the response body must conform to, or {@code null} if unconstrained. */
    public Object getResponseSchema() {
        return responseSchema;
    }

    /** UI rendering hints for approval form rendering, or {@code null} if absent. */
    public Object getResponseUiSchema() {
        return responseUiSchema;
    }

    @Override
    public String toString() {
        return "PendingTool{toolName=" + toolName + ", taskRefName=" + taskRefName + "}";
    }
}
