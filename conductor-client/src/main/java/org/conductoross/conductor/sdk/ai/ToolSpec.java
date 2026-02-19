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
package org.conductoross.conductor.sdk.ai;

import java.util.Map;


/**
 * Specifies a tool (function) available to an LLM during chat completion.
 * Corresponds to server-side model {@code ToolSpec}.
 *
 * <p>Example:
 * <pre>
 * ToolSpec tool = new ToolSpec()
 *     .name("get_weather")
 *     .type("SIMPLE")
 *     .description("Returns current weather for a city")
 *     .inputSchema(Map.of(
 *         "type", "object",
 *         "properties", Map.of(
 *             "city", Map.of("type", "string", "description", "City name")
 *         ),
 *         "required", List.of("city")
 *     ));
 * </pre>
 */
public class ToolSpec {

    private String name;
    private String type;
    private String description;
    private Map<String, Object> configParams;
    private Map<String, String> integrationNames;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;

    public ToolSpec name(String name) { this.name = name; return this; }
    public ToolSpec type(String type) { this.type = type; return this; }
    public ToolSpec description(String description) { this.description = description; return this; }
    public ToolSpec configParams(Map<String, Object> configParams) { this.configParams = configParams; return this; }
    public ToolSpec integrationNames(Map<String, String> integrationNames) { this.integrationNames = integrationNames; return this; }
    public ToolSpec inputSchema(Map<String, Object> inputSchema) { this.inputSchema = inputSchema; return this; }
    public ToolSpec outputSchema(Map<String, Object> outputSchema) { this.outputSchema = outputSchema; return this; }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Map<String, Object> getConfigParams() { return configParams; }
    public Map<String, String> getIntegrationNames() { return integrationNames; }
    public Map<String, Object> getInputSchema() { return inputSchema; }
    public Map<String, Object> getOutputSchema() { return outputSchema; }
}
