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
package org.conductoross.conductor.ai.examples;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;
import org.conductoross.conductor.ai.tools.HttpTool;
import org.conductoross.conductor.ai.tools.McpTool;

/**
 * Example 04 — HTTP and MCP Tools (server-side tools, no workers needed)
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>http_tool: HTTP endpoints as tools (Conductor HttpTask)</li>
 *   <li>mcp_tool: MCP server tools (Conductor CallMcpTool)</li>
 *   <li>Mixing local @Tool workers with server-side tools</li>
 * </ul>
 *
 * <p>These tools execute entirely server-side — no local worker process needed.
 *
 * <p>MCP Weather Server Setup:
 * <pre>
 *   npx -y @philschmid/weather-mcp   # runs on port 3001
 * </pre>
 *
 * <p>Requirements:
 * <ul>
 *   <li>Conductor server with LLM support</li>
 *   <li>MCP weather server on http://localhost:3001/mcp</li>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:6767</li>
 * </ul>
 */
public class Example04HttpAndMcpTools {

    static class ReportTools {
        @Tool(name = "format_report", description = "Format raw data into a readable report")
        public String formatReport(Map<String, Object> data) {
            return "Report: " + data;
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // Local worker tool
        List<ToolDef> localTools = ToolRegistry.fromInstance(new ReportTools());

        // HTTP tool — executes server-side via Conductor HttpTask
        ToolDef weatherApi = HttpTool.builder()
            .name("get_current_weather")
            .description("Get current weather for a city from the weather API")
            .url("http://localhost:3001/mcp")
            .method("POST")
            .accept("text/event-stream", "application/json")
            .contentType("application/json")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                    "jsonrpc", Map.of("type", "string", "const", "2.0"),
                    "id",      Map.of("const", 1),
                    "method",  Map.of("type", "string", "const", "tools/call"),
                    "params",  Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of(
                            "name",      Map.of("type", "string", "const", "get_current_weather"),
                            "arguments", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of("city", Map.of("type", "string")),
                                "required", List.of("city")
                            )
                        ),
                        "required", List.of("name", "arguments")
                    )
                ),
                "required", List.of("jsonrpc", "id", "method", "params")
            ))
            .build();

        // MCP tool — discovered from MCP server at runtime
        ToolDef mcpWeather = McpTool.builder()
            .name("github")
            .description("GitHub operations via MCP")
            .serverUrl("http://localhost:3001/mcp")
            .build();

        Agent agent = Agent.builder()
            .name("api_assistant")
            .model(Settings.LLM_MODEL)
            .instructions("You have access to weather data, GitHub, and report formatting.")
            .tools(localTools)
            .tools(weatherApi)
            .maxTokens(102040)
            .build();

        AgentResult result = runtime.run(agent,
            "Get the weather in London and format it as a report.");
        result.printResult();

        runtime.shutdown();
    }
}
