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
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
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

        // HTTP tool — uses the regular REST endpoint on the same test server, not /mcp.
        ToolDef httpWeather = HttpTool.builder()
            .name("get_current_weather_http")
            .description("Get the current weather for a city through the test server's HTTP API")
            .url("http://localhost:3001/api/weather")
            .method("GET")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of("city", Map.of("type", "string")),
                "required", List.of("city")
            ))
            .build();

        // MCP tool — Conductor discovers and calls the weather tools through the MCP protocol.
        ToolDef mcpWeather = McpTool.builder()
            .name("weather_mcp")
            .description("Weather tools for retrieving the current weather by city")
            .serverUrl("http://localhost:3001/mcp")
            .build();

        Agent agent = Agent.builder()
            .name("api_assistant")
            .model(Settings.LLM_MODEL)
            .instructions("Use weather_mcp for weather requests.")
            .tools(mcpWeather)
            .maxTokens(102040)
            .build();

        AgentResult result = runtime.run(agent,
            "Get the weather in London");
        result.printResult();

        runtime.shutdown();
    }
}
