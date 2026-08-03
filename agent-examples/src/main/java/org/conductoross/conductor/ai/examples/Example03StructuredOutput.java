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
 * Example 03 — Structured Output
 *
 * <p>Demonstrates using outputType to get typed structured output from an agent.
 */
public class Example03StructuredOutput {

    /** Structured output type for weather data. */
    public static class WeatherReport {
        public String city;
        public double temperature;
        public String condition;
        public String recommendation;

        @Override
        public String toString() {
            return String.format(
                "WeatherReport{city=%s, temp=%.1f, condition=%s, rec=%s}",
                city, temperature, condition, recommendation);
        }
    }

    static class WeatherTools {
        @Tool(name = "get_weather", description = "Get current weather data for a city")
        public Map<String, Object> getWeather(String city) {
            return Map.of("city", city, "temp_f", 72, "condition", "Sunny", "humidity", 45);
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        WeatherTools weatherTools = new WeatherTools();
        List<ToolDef> tools = ToolRegistry.fromInstance(weatherTools);

        Agent agent = Agent.builder()
            .name("weather_reporter")
            .model(Settings.LLM_MODEL)
            .instructions("You are a weather reporter. Get the weather and provide a recommendation.")
            .tools(tools)
            .outputType(WeatherReport.class)
            .build();

        AgentResult result = runtime.run(agent, "What's the weather in NYC?");
        result.printResult();

        // Get the typed output
        if (result.isSuccess()) {
            WeatherReport report = result.getOutput(WeatherReport.class);
            if (report != null) {
                System.out.println("\nTyped output:");
                System.out.println("  City: " + report.city);
                System.out.println("  Temperature: " + report.temperature);
                System.out.println("  Condition: " + report.condition);
                System.out.println("  Recommendation: " + report.recommendation);
            }
        }

        runtime.shutdown();
    }
}
