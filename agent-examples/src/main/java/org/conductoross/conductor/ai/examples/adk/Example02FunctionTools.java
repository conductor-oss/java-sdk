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
package org.conductoross.conductor.ai.examples.adk;

import java.util.Map;

import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.model.AgentResult;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

/**
 * Example Adk 02 — Native ADK {@link FunctionTool}s wired through runtime.
 *
 * <p>Tools are static methods annotated with {@code @Schema} — the idiomatic
 * ADK pattern — and packaged via {@code FunctionTool.create(Class, "methodName")}.
 * No Conductor-specific annotations.
 */
public class Example02FunctionTools {

    @Schema(description = "Get the current weather for a city")
    public static Map<String, Object> getWeather(
            @Schema(name = "city", description = "Name of the city") String city) {
        Map<String, Map<String, Object>> data = Map.of(
                "tokyo",  Map.of("temp_c", 22, "condition", "Clear",         "humidity", 65),
                "paris",  Map.of("temp_c", 18, "condition", "Partly Cloudy", "humidity", 72),
                "sydney", Map.of("temp_c", 25, "condition", "Sunny",         "humidity", 58),
                "mumbai", Map.of("temp_c", 32, "condition", "Humid",         "humidity", 85)
        );
        Map<String, Object> row = data.getOrDefault(city.toLowerCase(),
                Map.of("temp_c", 20, "condition", "Unknown", "humidity", 50));
        return Map.of("city", city, "temp_c", row.get("temp_c"),
                "condition", row.get("condition"), "humidity", row.get("humidity"));
    }

    @Schema(description = "Convert temperature between Celsius and Fahrenheit")
    public static Map<String, Object> convertTemperature(
            @Schema(name = "temp_celsius", description = "Temperature in Celsius") double tempCelsius,
            @Schema(name = "to_unit",      description = "Target unit (fahrenheit or kelvin)") String toUnit) {
        if ("fahrenheit".equalsIgnoreCase(toUnit)) {
            double f = tempCelsius * 9 / 5 + 32;
            return Map.of("celsius", tempCelsius, "fahrenheit", Math.round(f * 10.0) / 10.0);
        }
        if ("kelvin".equalsIgnoreCase(toUnit)) {
            double k = tempCelsius + 273.15;
            return Map.of("celsius", tempCelsius, "kelvin", Math.round(k * 10.0) / 10.0);
        }
        return Map.of("error", "Unknown unit: " + toUnit);
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent calculator = LlmAgent.builder()
                .name("travel_assistant")
                .description("Answers weather and temperature-conversion questions for travelers.")
                .model(Settings.LLM_MODEL)
                .instruction("You are a travel assistant. Help users with weather and temperature conversions. "
                        + "Be concise and accurate.")
                .tools(
                        FunctionTool.create(Example02FunctionTools.class, "getWeather"),
                        FunctionTool.create(Example02FunctionTools.class, "convertTemperature")
                )
                .build();

        AgentResult result = runtime.run(calculator,
                "What's the weather in Tokyo? Convert the temperature to Fahrenheit.");
        result.printResult();

        runtime.shutdown();
    }
}
