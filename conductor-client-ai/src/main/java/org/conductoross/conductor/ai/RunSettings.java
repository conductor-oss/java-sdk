/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-invocation settings applied when an {@link Agent} is executed.
 *
 * <p>Pass to {@code run}/{@code start}/{@code stream} (and their async
 * variants). LLM fields override the serialized <b>root</b> agent config before
 * compile+register+start, while execution metadata such as {@link
 * #idempotencyKey(String)} is sent at the top level of the start request.
 * Unset fields leave the agent and request unchanged; sub-agents keep their own
 * settings.
 *
 * <p>Example:
 * <pre>{@code
 * runtime.run(agent, "Summarize this", new RunSettings()
 *         .model("openai/gpt-4o")
 *         .temperature(0.2)
 *         .maxTokens(2048));
 * }</pre>
 */
public class RunSettings {

    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String reasoningEffort;
    private Integer thinkingBudgetTokens;
    private String idempotencyKey;

    /** Provider/model id (e.g. {@code "openai/gpt-4o"}). */
    public RunSettings model(String model) {
        this.model = model;
        return this;
    }

    /** Sampling temperature ({@code 0.0} is honored — the gate is non-null, not truthiness). */
    public RunSettings temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    /** Maximum tokens for the completion. */
    public RunSettings maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    /** Reasoning effort for reasoning models (e.g. {@code "high"}). */
    public RunSettings reasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }

    /** Extended-thinking token budget — setting it enables thinking. */
    public RunSettings thinkingBudgetTokens(Integer thinkingBudgetTokens) {
        this.thinkingBudgetTokens = thinkingBudgetTokens;
        return this;
    }

    /**
     * Stable key used by the server to deduplicate starts of the same logical
     * execution. The runtime omits {@code null}, empty, and whitespace-only
     * values; it never generates a key automatically.
     */
    public RunSettings idempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public Integer getThinkingBudgetTokens() {
        return thinkingBudgetTokens;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Map only the LLM override fields to {@code agentConfig} wire keys —
     * mirrors the Python SDK's {@code RunSettings.to_config_overrides} so
     * wire-key names match across SDKs. Execution metadata such as the
     * idempotency key is deliberately excluded. Uses {@code != null} so
     * {@code temperature(0.0)} and {@code maxTokens(0)} are honored.
     */
    public Map<String, Object> toConfigOverrides() {
        Map<String, Object> overrides = new LinkedHashMap<>();
        if (model != null) {
            overrides.put("model", model);
        }
        if (temperature != null) {
            overrides.put("temperature", temperature);
        }
        if (maxTokens != null) {
            overrides.put("maxTokens", maxTokens);
        }
        if (reasoningEffort != null) {
            overrides.put("reasoningEffort", reasoningEffort);
        }
        if (thinkingBudgetTokens != null) {
            Map<String, Object> thinking = new LinkedHashMap<>();
            thinking.put("enabled", true);
            thinking.put("budgetTokens", thinkingBudgetTokens);
            overrides.put("thinkingConfig", thinking);
        }
        return overrides;
    }

    @Override
    public String toString() {
        return "RunSettings" + toConfigOverrides();
    }
}
