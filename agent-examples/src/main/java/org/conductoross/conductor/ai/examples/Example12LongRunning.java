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
import org.conductoross.conductor.ai.model.AgentHandle;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 12 — Long-Running Agent (fire-and-forget with polling)
 *
 * <p>Demonstrates starting an agent asynchronously and polling for its result.
 * The agent runs as a Conductor workflow and can be monitored independently.
 * In production, the workflow ID can be saved and checked from any process.
 */
public class Example12LongRunning {

    public static void main(String[] args) throws InterruptedException {
        Agent analyst = Agent.builder()
            .name("saas_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a data analyst. Provide a concise analysis "
                + "when asked about data topics.")
            .build();

        try (AgentRuntime runtime = new AgentRuntime()) {
            runtime.prepareWorkers(analyst);

            // Fire-and-forget: start returns immediately with a handle
            AgentHandle handle = runtime.start(analyst,
                "What are the key metrics to track for a SaaS product?");

            System.out.println("Agent started: " + handle.getExecutionId());
            System.out.println("Polling for result...");

            // Simulate doing other work while the agent runs
            System.out.println("[doing other work...]");
            Thread.sleep(1000);

            // Poll for result with a 2-minute timeout, checking every 2s
            AgentResult result = handle.waitForResult(120_000, 2000);
            result.printResult();
        }
    }
}
