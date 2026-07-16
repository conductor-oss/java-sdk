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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for {@code POST /api/agent/compile}, {@code /deploy}, and {@code /start}.
 *
 * <p>All three endpoints share the same server-side {@code StartRequest} DTO. The agent
 * definition arrives pre-serialized as a JSON-ready map — domain serialization is owned by
 * the agent SDK ({@code conductor-client-ai}), keeping this transport DTO free of agent types.
 * Native agents carry it under {@code "agentConfig"}; framework-backed agents under
 * {@code "framework"} + {@code "rawConfig"}.
 *
 * <p>Build via {@link #nativeAgent(Object)} or {@link #frameworkAgent(String, Object)},
 * then chain builder methods for execution-specific fields. Unset ({@code null}) fields
 * are omitted from the JSON body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AgentRequest {

    // ── Agent definition (mutually exclusive shapes) ─────────────────────
    /** Serialized agent definition for native agents; {@code null} on the framework path. */
    @JsonProperty("agentConfig")
    private final Object agentConfig;

    /** Framework wire name (e.g. {@code "openai"}); {@code null} on the native path. */
    @JsonProperty("framework")
    private final String framework;

    /** Serialized agent definition for framework-backed agents; {@code null} on the native path. */
    @JsonProperty("rawConfig")
    private final Object rawConfig;

    // ── Execution fields (only meaningful for /start) ────────────────────
    @JsonProperty("prompt")
    private final String prompt;

    @JsonProperty("sessionId")
    private final String sessionId;

    @JsonProperty("runId")
    private final String runId;

    @JsonProperty("static_plan")
    private final Object staticPlan;

    // ── Optional fields ──────────────────────────────────────────────────
    @JsonProperty("media")
    private final List<String> media;

    @JsonProperty("context")
    private final Map<String, Object> context;

    @JsonProperty("idempotencyKey")
    private final String idempotencyKey;

    @JsonProperty("credentials")
    private final List<String> credentials;

    @JsonProperty("timeoutSeconds")
    private final Integer timeoutSeconds;

    private AgentRequest(Builder b) {
        this.agentConfig = b.agentConfig;
        this.framework = b.framework;
        this.rawConfig = b.rawConfig;
        this.prompt = b.prompt;
        this.sessionId = b.sessionId;
        this.runId = b.runId;
        this.staticPlan = b.staticPlan;
        this.media = b.media;
        this.context = b.context;
        this.idempotencyKey = b.idempotencyKey;
        this.credentials = b.credentials;
        this.timeoutSeconds = b.timeoutSeconds;
    }

    /** Build a request for a native (non-framework) agent from its serialized definition. */
    public static Builder nativeAgent(Object agentConfig) {
        return new Builder(agentConfig, null, null);
    }

    /** Build a request for a framework-backed agent (OpenAI, ADK, Skill) from its serialized definition. */
    public static Builder frameworkAgent(String framework, Object rawConfig) {
        return new Builder(null, framework, rawConfig);
    }

    // ── Builder ──────────────────────────────────────────────────────────

    public static final class Builder {
        private final Object agentConfig;
        private final String framework;
        private final Object rawConfig;
        private String prompt;
        private String sessionId;
        private String runId;
        private Object staticPlan;
        private List<String> media;
        private Map<String, Object> context;
        private String idempotencyKey;
        private List<String> credentials;
        private Integer timeoutSeconds;

        private Builder(Object agentConfig, String framework, Object rawConfig) {
            this.agentConfig = agentConfig;
            this.framework = framework;
            this.rawConfig = rawConfig;
        }

        public Builder prompt(String v) {
            this.prompt = v;
            return this;
        }

        public Builder sessionId(String v) {
            this.sessionId = v;
            return this;
        }

        public Builder runId(String v) {
            this.runId = v;
            return this;
        }

        /** Pre-serialized static plan (JSON-ready map), written as {@code "static_plan"}. */
        public Builder staticPlan(Object v) {
            this.staticPlan = v;
            return this;
        }

        public Builder media(List<String> v) {
            this.media = v;
            return this;
        }

        public Builder context(Map<String, Object> v) {
            this.context = v;
            return this;
        }

        public Builder idempotencyKey(String v) {
            this.idempotencyKey = v;
            return this;
        }

        public Builder credentials(List<String> v) {
            this.credentials = v;
            return this;
        }

        public Builder timeoutSeconds(Integer v) {
            this.timeoutSeconds = v;
            return this;
        }

        public AgentRequest build() {
            return new AgentRequest(this);
        }
    }
}
