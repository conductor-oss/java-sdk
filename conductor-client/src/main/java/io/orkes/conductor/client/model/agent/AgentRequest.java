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
import lombok.Data;
import lombok.ToString;

/**
 * Request payload for {@code POST /api/agent/compile}, {@code /deploy}, and {@code /start}.
 *
 * <p>All three endpoints share the same server-side {@code StartRequest} DTO. The agent
 * definition arrives pre-serialized as a JSON-ready map — domain serialization is owned by
 * the agent SDK ({@code conductor-client-ai}), keeping this transport DTO free of agent types.
 * Deployed agents carry {@code "name"} + optional {@code "version"}; native agents carry their
 * definition under {@code "agentConfig"}; framework-backed agents use {@code "framework"} plus
 * {@code "rawConfig"} or {@code "skillRef"}.
 *
 * <p>Build via {@link #deployedAgent(String, Integer)}, {@link #nativeAgent(Object)}, or {@link
 * #frameworkAgent(String, Object)}, then chain builder methods for execution-specific fields.
 * Unset ({@code null}) fields are omitted from the JSON body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@ToString(onlyExplicitlyIncluded = true)
public final class AgentRequest {

    // ── Agent definition (mutually exclusive shapes) ─────────────────────
    /** Name of an already-deployed agent; {@code null} for inline definitions. */
    private final String name;

    /** Optional version of an already-deployed agent. */
    private final Integer version;

    /** Serialized agent definition for native agents; {@code null} on the framework path. */
    private final Object agentConfig;

    /** Framework wire name (e.g. {@code "openai"}); {@code null} on the native path. */
    private final String framework;

    /** Serialized agent definition for framework-backed agents; {@code null} on the native path. */
    private final Object rawConfig;

    /** Optional model override understood by the server's agent control plane. */
    private final String model;

    /** Skill reference for framework-backed agents that do not provide {@code rawConfig}. */
    private final Map<String, Object> skillRef;

    // ── Execution fields (only meaningful for /start) ────────────────────
    private final String prompt;

    private final String sessionId;

    private final String runId;

    @JsonProperty("static_plan")
    private final Object staticPlan;

    // ── Optional fields ──────────────────────────────────────────────────
    private final List<String> media;

    private final Map<String, Object> context;

    private final String idempotencyKey;

    private final List<String> credentials;

    private final Integer timeoutSeconds;

    private AgentRequest(Builder b) {
        this.name = b.name;
        this.version = b.version;
        this.agentConfig = b.agentConfig;
        this.framework = b.framework;
        this.rawConfig = b.rawConfig;
        this.model = b.model;
        this.skillRef = b.skillRef;
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

    /** Build a request that starts an already-deployed agent by name and optional version. */
    public static Builder deployedAgent(String name, Integer version) {
        return new Builder(name, version, null, null, null);
    }

    /** Build a request for a native (non-framework) agent from its serialized definition. */
    public static Builder nativeAgent(Object agentConfig) {
        return new Builder(null, null, agentConfig, null, null);
    }

    /** Build a request for a framework-backed agent (OpenAI, ADK, Skill) from its serialized definition. */
    public static Builder frameworkAgent(String framework, Object rawConfig) {
        return new Builder(null, null, null, framework, rawConfig);
    }

    // ── Builder ──────────────────────────────────────────────────────────

    public static final class Builder {
        private final String name;
        private final Integer version;
        private final Object agentConfig;
        private final String framework;
        private final Object rawConfig;
        private String model;
        private Map<String, Object> skillRef;
        private String prompt;
        private String sessionId;
        private String runId;
        private Object staticPlan;
        private List<String> media;
        private Map<String, Object> context;
        private String idempotencyKey;
        private List<String> credentials;
        private Integer timeoutSeconds;

        private Builder(
                String name,
                Integer version,
                Object agentConfig,
                String framework,
                Object rawConfig) {
            this.name = name;
            this.version = version;
            this.agentConfig = agentConfig;
            this.framework = framework;
            this.rawConfig = rawConfig;
        }

        public Builder model(String v) {
            this.model = v;
            return this;
        }

        public Builder skillRef(Map<String, Object> v) {
            this.skillRef = v;
            return this;
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
