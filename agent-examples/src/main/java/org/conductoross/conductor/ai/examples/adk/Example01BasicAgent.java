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
 * Example Adk 01 — Basic Agent
 *
 * <p>Java port of <code>sdk/python/examples/adk/01_basic_agent.py</code>.
 *
 * <p>Demonstrates: the simplest Google ADK agent — defined via the
 * native {@link LlmAgent} builder and bridged to the Agentspan durable
 * runtime via {@link org.conductoross.conductor.ai.Agentspan#run(Object, String)}.
 */
public class Example01BasicAgent {
    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent researcher = LlmAgent.builder()
            .name("greeter")
            .description("A friendly assistant that gives concise, helpful answers.")
            .model(Settings.LLM_MODEL)
            .instruction("You are a friendly assistant. Keep your responses concise and helpful.")
            .build();

        AgentResult result = runtime.run(researcher,
            "Say hello and tell me a fun fact about machine learning.");
        System.out.println("researcher completed with status: " + result.getStatus());
        result.printResult();

        runtime.shutdown();
    }
}
