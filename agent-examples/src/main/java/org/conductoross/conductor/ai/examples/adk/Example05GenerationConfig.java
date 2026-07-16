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
import com.google.genai.types.GenerateContentConfig;

/**
 * Example Adk 05 — Generation Config
 *
 * <p>Java port of <code>sdk/python/examples/adk/05_generation_config.py</code>.
 *
 * <p>Demonstrates: temperature and output control via native ADK's
 * {@code generateContentConfig(...)}.
 */
public class Example05GenerationConfig {
    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // Precise agent — low temperature for factual responses
        LlmAgent factualAgent = LlmAgent.builder()
            .name("fact_checker")
            .description("A low-temperature fact-checker that gives precise, well-sourced answers.")
            .model(Settings.LLM_MODEL)
            .instruction(
                "You are a precise fact-checker. Provide accurate, well-sourced "
                + "answers. Be concise and avoid speculation.")
            .generateContentConfig(GenerateContentConfig.builder()
                .temperature(0.1f)
                .build())
            .build();

        // Creative agent — high temperature for creative writing
        LlmAgent creativeAgent = LlmAgent.builder()
            .name("storyteller")
            .description("A high-temperature storyteller that produces vivid, imaginative narratives.")
            .model(Settings.LLM_MODEL)
            .instruction(
                "You are an imaginative storyteller. Create vivid, engaging "
                + "narratives with rich descriptions and unexpected twists.")
            .generateContentConfig(GenerateContentConfig.builder()
                .temperature(0.9f)
                .build())
            .build();

        System.out.println("=== Factual Agent (temp=0.1) ===");
        AgentResult result = runtime.run(factualAgent,
            "What is the speed of light in a vacuum?");
        result.printResult();

        System.out.println("\n=== Creative Agent (temp=0.9) ===");
        result = runtime.run(creativeAgent,
            "Write a two-sentence story about a cat who discovered a hidden library.");
        result.printResult();

        runtime.shutdown();
    }
}
