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

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 49 — Include Contents (control context passed to sub-agents)
 *
 * <p>When {@code includeContents("none")} is set, a sub-agent starts with a
 * clean slate and does NOT see the parent agent's conversation history. This
 * is useful for sub-agents that should work independently.
 *
 * <p>Two sub-agents:
 * <ul>
 *   <li>independent_summarizer: {@code includeContents("none")} — clean slate</li>
 *   <li>context_aware_helper: default — sees full conversation history</li>
 * </ul>
 */
public class Example49IncludeContents {

    static class SummaryTools {
        @Tool(name = "summarize_text", description = "Summarize a piece of text to its first 20 words")
        public Map<String, Object> summarizeText(String text) {
            String[] words = text.split("\\s+");
            int count = Math.min(words.length, 20);
            String summary = String.join(" ", java.util.Arrays.copyOf(words, count));
            if (words.length > 20) summary += "...";
            return Map.of("summary", summary, "word_count", words.length);
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> summaryTools = ToolRegistry.fromInstance(new SummaryTools());

        // This sub-agent starts with a clean slate — no parent conversation history
        Agent independentSummarizer = Agent.builder()
            .name("independent_summarizer_49")
            .model(Settings.LLM_MODEL)
            .instructions("You are a summarizer. Summarize any text given to you concisely.")
            .tools(summaryTools)
            .includeContents("none")
            .build();

        // This sub-agent sees the parent's full conversation history (default)
        Agent contextAwareHelper = Agent.builder()
            .name("context_aware_helper_49")
            .model(Settings.LLM_MODEL)
            .instructions("You are a helpful assistant that builds on prior conversation context.")
            .build();

        Agent coordinator = Agent.builder()
            .name("coordinator_49")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You coordinate tasks. Route summarization requests to "
                + "independent_summarizer_49 and general questions to context_aware_helper_49.")
            .agents(independentSummarizer, contextAwareHelper)
            .strategy(Strategy.HANDOFF)
            .build();

        System.out.println("=== Scenario 1: Summarization (independent sub-agent, no parent context) ===");
        AgentResult r1 = runtime.run(coordinator,
            "Summarize this: Artificial intelligence is transforming industries by automating "
            + "repetitive tasks, improving decision-making, and enabling new capabilities. "
            + "Companies are investing heavily in AI to gain competitive advantages.");
        r1.printResult();

        System.out.println("=== Scenario 2: General question (context-aware sub-agent) ===");
        AgentResult r2 = runtime.run(coordinator,
            "What are the key benefits of AI in business?");
        r2.printResult();

        runtime.shutdown();
    }
}
