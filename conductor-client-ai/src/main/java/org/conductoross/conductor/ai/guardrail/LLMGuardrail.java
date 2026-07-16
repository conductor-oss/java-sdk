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
package org.conductoross.conductor.ai.guardrail;

import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.enums.Position;
import org.conductoross.conductor.ai.model.GuardrailDef;

/**
 * A guardrail that uses an LLM to evaluate content against a policy.
 *
 * <p>Serialized as {@code guardrailType: "llm"}. The Conductor server calls the specified model
 * with the policy and content and expects a {@code {"passed": true/false, "reason": "..."}} response.
 * No worker process is needed.
 *
 * <pre>{@code
 * GuardrailDef safety = LLMGuardrail.builder()
 *     .name("safety_check")
 *     .model("anthropic/claude-sonnet-4-6")
 *     .policy("Reject any content that contains harmful, violent, or discriminatory language.")
 *     .build();
 * }</pre>
 */
public class LLMGuardrail {

    private LLMGuardrail() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "llm_guardrail";
        private String model;
        private String policy;
        private Position position = Position.OUTPUT;
        private OnFail onFail = OnFail.RAISE;
        private int maxRetries = 3;
        private Integer maxTokens;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** LLM model in {@code "provider/model"} format (e.g. {@code "anthropic/claude-sonnet-4-6"}). */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** Description of what the guardrail should check for. */
        public Builder policy(String policy) {
            this.policy = policy;
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder onFail(OnFail onFail) {
            this.onFail = onFail;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public GuardrailDef build() {
            if (model == null || model.isEmpty()) {
                throw new IllegalArgumentException("LLMGuardrail requires a model");
            }
            if (policy == null || policy.isEmpty()) {
                throw new IllegalArgumentException("LLMGuardrail requires a policy");
            }
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("model", model);
            config.put("policy", policy);
            if (maxTokens != null) config.put("maxTokens", maxTokens);
            return GuardrailDef.builder()
                    .name(name)
                    .position(position)
                    .onFail(onFail)
                    .maxRetries(maxRetries)
                    .guardrailType("llm")
                    .config(config)
                    .build();
        }
    }
}
