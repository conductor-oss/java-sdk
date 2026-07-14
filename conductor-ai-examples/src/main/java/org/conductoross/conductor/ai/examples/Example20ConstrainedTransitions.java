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
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 20 — Constrained Speaker Transitions (code review workflow)
 *
 * <p>Demonstrates {@code allowedTransitions} which restricts which agent can
 * speak after which in a ROUND_ROBIN discussion. Useful for enforcing
 * conversational protocols.
 *
 * <p>Code review protocol:
 * <ul>
 *   <li>developer → reviewer (code must be reviewed)</li>
 *   <li>reviewer → developer OR approver (send back or escalate)</li>
 *   <li>approver → developer (request revisions)</li>
 * </ul>
 */
public class Example20ConstrainedTransitions {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent developer = Agent.builder()
            .name("developer")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a software developer. Write or revise code based on feedback. "
                + "Keep responses focused on code changes.")
            .build();

        Agent reviewer = Agent.builder()
            .name("reviewer")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a code reviewer. Review the developer's code for bugs, style, "
                + "and best practices. Provide specific, actionable feedback.")
            .build();

        Agent approver = Agent.builder()
            .name("approver")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are the tech lead. Review the code and feedback. Either approve "
                + "the code or request revisions with specific guidance.")
            .build();

        // Constrained transitions enforce the review protocol
        Agent codeReview = Agent.builder()
            .name("code_review")
            .model(Settings.LLM_MODEL)
            .agents(developer, reviewer, approver)
            .strategy(Strategy.ROUND_ROBIN)
            .maxTurns(6)
            .allowedTransitions(Map.of(
                "developer", List.of("reviewer"),
                "reviewer", List.of("developer", "approver"),
                "approver", List.of("developer")
            ))
            .build();

        AgentResult result = runtime.run(codeReview,
            "Write a Python function to validate email addresses using regex.");
        result.printResult();

        runtime.shutdown();
    }
}
