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

import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.model.AgentResult;

import com.google.adk.agents.LlmAgent;

/**
 * Example Adk 00 — Hello World using the native Google ADK Java SDK.
 *
 * <p>Defines a real {@link LlmAgent} with {@code com.google.adk.agents.LlmAgent.builder()},
 * and hands it directly to {@link org.conductoross.conductor.ai.AgentRuntime#run(Object, String)}
 * for execution on the durable Conductor runtime.
 *
 * <p>Requirements:
 * <ul>
 *   <li>{@code CONDUCTOR_SERVER_URL=http://localhost:8080/api}</li>
 *   <li>OpenAI/Gemini key configured in server credentials</li>
 * </ul>
 */
public class Example00HelloWorld {
    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent greeter = LlmAgent.builder()
                .name("greeter")
                .description("A friendly greeter that says hello and shares a fun fact.")
                .model(Settings.LLM_MODEL)
                .instruction("You are a friendly greeter. Reply with a warm hello and one fun fact.")
                .build();

        AgentResult result = runtime.run(greeter, "Say hello!");
        result.printResult();

        runtime.shutdown();
    }
}
