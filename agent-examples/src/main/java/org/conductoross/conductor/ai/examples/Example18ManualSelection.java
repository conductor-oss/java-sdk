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
import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.model.AgentHandle;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 18 — Manual Agent Selection (programmatic simulation)
 *
 * <p>Demonstrates {@code Strategy.MANUAL} where an operator decides which
 * sub-agent responds on each turn. The workflow pauses at a HumanTask after
 * each turn, waiting for a {@code {"selected": "<agent_name>"}} response.
 *
 * <p>In this example the selections are driven programmatically to make the
 * example fully runnable end-to-end. In a real application a UI would present
 * the agent choices and a human would make the selection.
 *
 * <pre>
 * editorial_team (MANUAL, 3 turns)
 *   turn 1 → writer       (auto-selected)
 *   turn 2 → fact_checker (auto-selected)
 *   turn 3 → editor       (auto-selected)
 * </pre>
 */
public class Example18ManualSelection {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent writer = Agent.builder()
            .name("writer")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a creative writer. Draft compelling, vivid prose. "
                + "Prioritise narrative flow and reader engagement.")
            .build();

        Agent editor = Agent.builder()
            .name("editor")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a strict editor. Review the content for grammar, "
                + "clarity, and structure. Be direct and precise.")
            .build();

        Agent factChecker = Agent.builder()
            .name("fact_checker")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a meticulous fact-checker. Verify the accuracy of "
                + "claims in the content and flag anything unsubstantiated.")
            .build();

        Agent editorialTeam = Agent.builder()
            .name("editorial_team")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You coordinate an editorial team. A human operator selects "
                + "which team member responds on each turn.")
            .agents(writer, editor, factChecker)
            .strategy(Strategy.MANUAL)
            .maxTurns(3)
            .build();

        String prompt =
            "Draft a short paragraph about the discovery of penicillin, "
            + "then have it reviewed for accuracy and style.";

        AgentHandle handle = runtime.start(editorialTeam, prompt);
        System.out.println("Execution ID: " + handle.getExecutionId());

        // Drive the 3 manual turns. Each turn the MANUAL strategy creates a
        // HumanTask and sets isWaiting=true. We poll for that state, then send
        // the selection. After the last turn the workflow completes.
        List<String> selections = List.of("writer", "fact_checker", "editor");

        for (int i = 0; i < selections.size(); i++) {
            String agentName = selections.get(i);

            boolean waiting = handle.waitUntilWaiting(120_000);
            if (!waiting) {
                System.out.println("Turn " + (i + 1) + ": workflow completed before selection");
                break;
            }
            handle.respond(Map.of("selected", agentName));
            System.out.println("Turn " + (i + 1) + ": selected '" + agentName + "'");
        }

        // Wait for final completion
        AgentResult result = handle.waitForResult();
        result.printResult();

        runtime.shutdown();
    }
}
