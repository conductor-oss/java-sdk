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
import lombok.Data;

/**
 * Details of the HITL task that is currently paused, embedded in {@link AgentStatusResponse}.
 *
 * <p>Present only when {@link AgentStatusResponse#isWaiting()} is {@code true}.
 * Pass {@link #getTaskRefName()} back to the server via
 * {@link io.orkes.conductor.client.AgentClient#respond(String, RespondBody)} to resume execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public final class PendingTool {

    private String taskRefName;

    @JsonProperty("tool_name")
    private String toolName;

    private Map<String, Object> parameters;

    @JsonProperty("response_schema")
    private Object responseSchema;

    @JsonProperty("response_ui_schema")
    private Object responseUiSchema;

}
