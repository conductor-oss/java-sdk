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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

/**
 * Response from {@code POST /api/agent/deploy} and {@code POST /api/agent/start}.
 *
 * <p>For deploy, the execution ID is {@code null} — no execution was started. For start, the
 * execution ID is the Conductor workflow ID to pass to
 * {@link io.orkes.conductor.client.AgentClient#getAgentStatus(String)} and {@link io.orkes.conductor.client.AgentClient#respond(String, RespondBody)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public final class StartResponse {

    /** Current canonical field name. {@code @JsonAlias} handles older server versions. */
    @JsonAlias({"workflowId", "id", "correlationId"})
    private String executionId;

    private String agentName;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> requiredWorkers = Collections.emptyList();
}
