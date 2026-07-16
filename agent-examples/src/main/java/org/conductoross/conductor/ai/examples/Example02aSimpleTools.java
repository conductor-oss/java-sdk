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

/**
 * Example 02a — Simple Tool Calling
 *
 * <p>Two tools — weather and stock price. Based on the user's question,
 * the LLM decides which tool to call.
 *
 * <p>In the Conductor UI each tool call appears as a separate DynamicTask
 * with its inputs and outputs clearly visible.
 */
public class Example02aSimpleTools {

    static class AssistantTools {
        @Tool(name = "get_weather", description = "Get the current weather for a city")
        public Map<String, Object> getWeather(String city) {
            return Map.of("city", city, "temp_f", 72, "condition", "Sunny");
        }

        @Tool(name = "get_stock_price", description = "Get the current stock price for a ticker symbol")
        public Map<String, Object> getStockPrice(String symbol) {
            return Map.of("symbol", symbol, "price", 182.50, "change", "+1.2%");
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> tools = ToolRegistry.fromInstance(new AssistantTools());

        Agent agent = Agent.builder()
            .name("weather_stock_agent")
            .model(Settings.LLM_MODEL)
            .tools(tools)
            .instructions("You are a helpful assistant. Use tools to answer questions.")
            .build();

        // The LLM will call get_weather (not get_stock_price)
        AgentResult result = runtime.run(agent, "What's the weather like in San Francisco?");
        result.printResult();

        runtime.shutdown();
    }
}
