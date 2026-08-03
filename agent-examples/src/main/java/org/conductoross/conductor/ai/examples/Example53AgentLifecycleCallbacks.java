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
import org.conductoross.conductor.ai.CallbackHandler;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 53 — Agent Lifecycle Callbacks (composable handler classes)
 *
 * <p>Demonstrates using {@link CallbackHandler} subclasses to hook into agent
 * and model lifecycle events. Multiple handlers chain in list order — each one
 * does a single concern (timing, logging, etc.).
 *
 * <p>Registered as Conductor workers:
 * <ul>
 *   <li>{@code lifecycle_agent_53_before_model} — fires before each LLM call</li>
 *   <li>{@code lifecycle_agent_53_after_model} — fires after each LLM call</li>
 * </ul>
 */
public class Example53AgentLifecycleCallbacks {

    // ── Handler 1: Timing (agent lifecycle) ──────────────────────────

    static class TimingHandler extends CallbackHandler {
        private long t0;

        @Override
        public Map<String, Object> onAgentStart(Map<String, Object> kwargs) {
            t0 = System.currentTimeMillis();
            System.out.println("  [timing] Agent started");
            return null;
        }

        @Override
        public Map<String, Object> onAgentEnd(Map<String, Object> kwargs) {
            long elapsed = System.currentTimeMillis() - t0;
            System.out.println("  [timing] Agent finished — " + elapsed + "ms");
            return null;
        }
    }

    // ── Handler 2: Logging (model + tool lifecycle) ───────────────────

    static class LoggingHandler extends CallbackHandler {

        @Override
        public Map<String, Object> onModelStart(Map<String, Object> kwargs) {
            Object messages = kwargs.get("messages");
            int count = messages instanceof List ? ((List<?>) messages).size() : 0;
            System.out.println("  [log] Sending " + count + " messages to LLM");
            return null;
        }

        @Override
        public Map<String, Object> onModelEnd(Map<String, Object> kwargs) {
            Object result = kwargs.get("llm_result");
            String snippet = result instanceof String
                ? ((String) result).substring(0, Math.min(80, ((String) result).length()))
                : "";
            System.out.println("  [log] LLM responded: " + snippet);
            return null;
        }

        @Override
        public Map<String, Object> onToolStart(Map<String, Object> kwargs) {
            System.out.println("  [log] Tool call started");
            return null;
        }

        @Override
        public Map<String, Object> onToolEnd(Map<String, Object> kwargs) {
            System.out.println("  [log] Tool call finished");
            return null;
        }
    }

    // ── Tool ──────────────────────────────────────────────────────────

    static class WeatherTools {
        @Tool(name = "lookup_weather_53", description = "Get the current weather for a city")
        public Map<String, Object> lookupWeather(String city) {
            return Map.of("city", city, "temperature", "22C", "condition", "sunny");
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> tools = ToolRegistry.fromInstance(new WeatherTools());

        Agent agent = Agent.builder()
            .name("lifecycle_agent_53")
            .model(Settings.LLM_MODEL)
            .instructions("You are a helpful assistant. Use lookup_weather_53 for weather queries.")
            .tools(tools)
            .callbacks(new TimingHandler(), new LoggingHandler())
            .build();

        AgentResult result = runtime.run(agent, "What's the weather like in Tokyo?");
        result.printResult();

        runtime.shutdown();
    }
}
