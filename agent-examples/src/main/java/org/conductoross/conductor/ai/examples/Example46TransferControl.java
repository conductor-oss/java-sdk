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
 * Example 46 — Transfer Control (restrict which agents can hand off to which)
 *
 * <p>Uses {@code allowedTransitions} to constrain handoff paths between sub-agents.
 * Prevents unwanted transfers (e.g., the data collector can only go to the analyst).
 *
 * <p>Transition constraints:
 * <ul>
 *   <li>data_collector_46 → analyst_46 only</li>
 *   <li>analyst_46 → summarizer_46 or coordinator_46</li>
 *   <li>summarizer_46 → coordinator_46 only</li>
 * </ul>
 */
public class Example46TransferControl {

    static class CollectorTools {
        @Tool(name = "collect_data", description = "Collect data from a source")
        public Map<String, Object> collectData(String source) {
            return Map.of("source", source, "records", 42, "status", "collected");
        }
    }

    static class AnalystTools {
        @Tool(name = "analyze_data", description = "Analyze collected data")
        public Map<String, Object> analyzeData(String dataSummary) {
            return Map.of("analysis", "Trend is upward", "confidence", 0.87);
        }
    }

    static class SummarizerTools {
        @Tool(name = "write_summary", description = "Write a summary report")
        public Map<String, Object> writeSummary(String findings) {
            return Map.of(
                "summary", "Report: " + (findings.length() > 100 ? findings.substring(0, 100) : findings),
                "word_count", 150
            );
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> collectorTools = ToolRegistry.fromInstance(new CollectorTools());
        List<ToolDef> analystTools = ToolRegistry.fromInstance(new AnalystTools());
        List<ToolDef> summarizerTools = ToolRegistry.fromInstance(new SummarizerTools());

        Agent dataCollector = Agent.builder()
            .name("data_collector_46")
            .model(Settings.LLM_MODEL)
            .instructions("Collect data using collect_data. Then transfer to the analyst.")
            .tools(collectorTools)
            .build();

        Agent analyst = Agent.builder()
            .name("analyst_46")
            .model(Settings.LLM_MODEL)
            .instructions("Analyze data using analyze_data. Transfer to summarizer when done.")
            .tools(analystTools)
            .build();

        Agent summarizer = Agent.builder()
            .name("summarizer_46")
            .model(Settings.LLM_MODEL)
            .instructions("Write a summary using write_summary.")
            .tools(summarizerTools)
            .build();

        // Coordinator with constrained transitions
        Agent coordinator = Agent.builder()
            .name("coordinator_46")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You coordinate a data pipeline. Route to data_collector_46 first, "
                + "then analyst_46, then summarizer_46.")
            .agents(dataCollector, analyst, summarizer)
            .strategy(Strategy.HANDOFF)
            .allowedTransitions(Map.of(
                "data_collector_46", List.of("analyst_46"),
                "analyst_46", List.of("summarizer_46", "coordinator_46"),
                "summarizer_46", List.of("coordinator_46")
            ))
            .build();

        AgentResult result = runtime.run(coordinator,
            "Collect data from the sales database, analyze trends, and write a summary.");
        result.printResult();

        runtime.shutdown();
    }
}
