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

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.AgentDef;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 70 — Annotated Agent
 *
 * <p>Demonstrates defining an agent declaratively with the {@code @AgentDef} method
 * annotation (the Java counterpart of the Python SDK's {@code @agent} decorator).
 * The method body returns the agent's instructions; {@code @Tool} methods on the
 * same class are attached automatically.
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o</li>
 * </ul>
 */
public class Example70AnnotatedAgent {

    @Tool(name = "get_weather", description = "Get the current weather for a city")
    public String getWeather(String city) {
        return "Sunny, 72F in " + city;
    }

    @AgentDef(model = "openai/gpt-4o")
    public String weatherbot() {
        return "You are a weather assistant. Use the get_weather tool to answer questions.";
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = Agent.fromInstance(new Example70AnnotatedAgent(), "weatherbot");

        AgentResult result = runtime.run(agent, "What's the weather in Paris?");
        result.printResult();

        runtime.shutdown();
    }
}
