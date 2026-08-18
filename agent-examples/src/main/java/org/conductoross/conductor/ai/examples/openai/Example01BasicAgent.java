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
 * Example OpenAi 01 — Basic Agent
 *
 * <p>Java port of <code>sdk/python/examples/openai/01_basic_agent.py</code>.
 *
 * <p>Demonstrates: the simplest possible OpenAI Agents SDK agent — no tools,
 * just a name + instructions + model — wired through the Conductor
 * {@link OpenAIAgent} factory so the server normalizes it into a Conductor
 * workflow.
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:6767/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini</li>
 * </ul>
 */
public class Example01BasicAgent {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = OpenAIAgent.builder()
                .name("openai_greeter_01")
                .instructions("You are a friendly assistant. Keep your responses concise and helpful.")
                .model(Settings.LLM_MODEL)
                .build();

        AgentResult result = runtime.run(
                agent,
                "Say hello and tell me a fun fact about the Python programming language.");
        result.printResult();

        runtime.shutdown();
    }
}
