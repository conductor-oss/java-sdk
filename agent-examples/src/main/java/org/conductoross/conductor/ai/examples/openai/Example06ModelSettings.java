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
package org.conductoross.conductor.ai.examples.openai;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.frameworks.OpenAIAgent;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example OpenAi 06 — Model Settings
 *
 * <p>Java port of <code>sdk/python/examples/openai/06_model_settings.py</code>.
 *
 * <p>Demonstrates: two agents with identical wiring but different stylistic
 * intent — a high-temperature creative writer and a low-temperature precise
 * code reviewer.
 *
 * <p>Python parity gap: the current {@link OpenAIAgent} builder does not
 * expose {@code model_settings} (temperature, max_tokens). The intended
 * settings from the Python original are documented here so a future
 * OpenAIAgent builder extension can surface them:
 * <ul>
 *   <li>{@code creative_writer}: {@code temperature=0.9, max_tokens=500}.</li>
 *   <li>{@code code_reviewer}: {@code temperature=0.1, max_tokens=300}.</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini</li>
 * </ul>
 */
public class Example06ModelSettings {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // Creative agent — high-temperature intent (0.9) per Python original.
        Agent creativeAgent = OpenAIAgent.builder()
                .name("creative_writer")
                .instructions(
                        "You are a creative writing assistant. Write with vivid imagery "
                                + "and unexpected metaphors. Be bold and imaginative.")
                .model(Settings.LLM_MODEL)
                .build();

        // Precise agent — low-temperature intent (0.1) per Python original.
        Agent preciseAgent = OpenAIAgent.builder()
                .name("code_reviewer")
                .instructions(
                        "You are a precise code reviewer. Analyze code snippets for bugs, "
                                + "security issues, and best practices. Be concise and specific.")
                .model(Settings.LLM_MODEL)
                .build();

        System.out.println("=== Creative Agent (temp=0.9) ===");
        AgentResult creative = runtime.run(
                creativeAgent,
                "Write a two-sentence story about a robot learning to paint.");
        creative.printResult();

        System.out.println("\n=== Precise Agent (temp=0.1) ===");
        AgentResult precise = runtime.run(
                preciseAgent,
                "Review this Python code: `data = eval(user_input)`");
        precise.printResult();

        runtime.shutdown();
    }
}
