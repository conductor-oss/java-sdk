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
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 01 — Basic Agent
 *
 * <p>Demonstrates the simplest possible agent: a single LLM with no tools.
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:6767/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o</li>
 * </ul>
 */
public class Example01BasicAgent {
    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = Agent.builder()
            .name("basic_assistant")
            .model(Settings.LLM_MODEL)
            .instructions("You are a helpful assistant.")
            .build();

        AgentResult result = runtime.run(agent, "What is the capital of France?");
        result.printResult();

        runtime.shutdown();
    }
}
