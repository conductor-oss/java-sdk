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

import java.util.Map;

/**
 * Reference to a named prompt template stored on the Conductor server.
 *
 * <p>Pass an instance as the {@code instructionsTemplate} on an Agent to use a
 * server-managed template instead of a hardcoded string. Variables replace
 * {@code ${var}} placeholders at execution time.
 *
 * <pre>{@code
 * Agent agent = Agent.builder()
 *     .name("support")
 *     .model("anthropic/claude-sonnet-4-6")
 *     .instructionsTemplate(new PromptTemplate("customer-support",
 *         Map.of("company", "Acme", "tone", "friendly")))
 *     .build();
 * }</pre>
 */
public class PromptTemplate {
    private final String name;
    private final Map<String, Object> variables;
    private final Integer version;

    /**
     * Reference a template by name, using the latest version with no variables.
     */
    public PromptTemplate(String name) {
        this(name, null, null);
    }

    /**
     * Reference a template by name with variable substitution.
     */
    public PromptTemplate(String name, Map<String, Object> variables) {
        this(name, variables, null);
    }

    /**
     * Reference a specific version of a template with variable substitution.
     */
    public PromptTemplate(String name, Map<String, Object> variables, Integer version) {
        this.name = name;
        this.variables = variables;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Integer getVersion() {
        return version;
    }
}
