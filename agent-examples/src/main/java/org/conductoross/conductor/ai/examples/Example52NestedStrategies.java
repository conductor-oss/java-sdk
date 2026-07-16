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
 * Example 52 — Nested Strategies (parallel research → sequential summarizer)
 *
 * <p>Demonstrates composing strategies: a PARALLEL phase runs multiple research
 * agents concurrently, then a SEQUENTIAL pipeline feeds the combined output
 * to a summarizer.
 *
 * <pre>
 * pipeline (SEQUENTIAL)
 * └── research_phase (PARALLEL)
 *     ├── market_analyst  ─── concurrent
 *     └── risk_analyst    ─── concurrent
 * └── summarizer          ─── receives combined research output
 * </pre>
 */
public class Example52NestedStrategies {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // ── Parallel research phase ──────────────────────────────────────

        Agent marketAnalyst = Agent.builder()
            .name("market_analyst_52")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a market analyst. Analyze the market size, growth rate, "
                + "and key players for the given topic. Be concise (3-4 bullet points).")
            .build();

        Agent riskAnalyst = Agent.builder()
            .name("risk_analyst_52")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a risk analyst. Identify the top 3 risks: regulatory, "
                + "technical, and competitive. Be concise.")
            .build();

        // Both analysts run concurrently
        Agent researchPhase = Agent.builder()
            .name("research_phase_52")
            .model(Settings.LLM_MODEL)
            .agents(marketAnalyst, riskAnalyst)
            .strategy(Strategy.PARALLEL)
            .build();

        // ── Sequential summarizer ────────────────────────────────────────

        Agent summarizer = Agent.builder()
            .name("summarizer_52")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are an executive briefing writer. Synthesize the market analysis "
                + "and risk assessment into a concise executive summary (1 paragraph).")
            .build();

        // ── Pipeline: parallel research → sequential summarizer ──────────

        Agent pipeline = Agent.builder()
            .name("research_pipeline_52")
            .model(Settings.LLM_MODEL)
            .agents(researchPhase, summarizer)
            .strategy(Strategy.SEQUENTIAL)
            .build();

        AgentResult result = runtime.run(pipeline,
            "Launching an AI-powered healthcare diagnostics tool in the US");
        result.printResult();

        runtime.shutdown();
    }
}
