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
import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 07 — Parallel Agents
 *
 * <p>Demonstrates running multiple agents in parallel. All sub-agents receive
 * the same prompt and run concurrently. Results are aggregated.
 */
public class Example07ParallelAgents {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // Analysts running in parallel
        Agent technicalAnalyst = Agent.builder()
            .name("technical_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a technical analyst. Analyze the topic from a technical perspective. "
                + "Focus on implementation details, technical challenges, and engineering aspects. "
                + "Be specific and technical.")
            .build();

        Agent businessAnalyst = Agent.builder()
            .name("business_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a business analyst. Analyze the topic from a business perspective. "
                + "Focus on market opportunities, ROI, competitive landscape, and business impact. "
                + "Use business terminology.")
            .build();

        Agent riskAnalyst = Agent.builder()
            .name("risk_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a risk analyst. Analyze the topic from a risk management perspective. "
                + "Identify potential risks, mitigation strategies, and regulatory considerations. "
                + "Use a risk framework.")
            .build();

        // Run all analysts in parallel
        Agent analysisTeam = Agent.builder()
            .name("analysis_team")
            .model(Settings.LLM_MODEL)
            .agents(technicalAnalyst, businessAnalyst, riskAnalyst)
            .strategy(Strategy.PARALLEL)
            .build();

        AgentResult result = runtime.run(analysisTeam,
            "Analyze the adoption of AI in healthcare for patient diagnosis");
        result.printResult();

        runtime.shutdown();
    }
}
