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
 * Example 29 — Agent Introductions
 *
 * <p>Demonstrates the {@code introduction} parameter which adds a self-introduction
 * to the conversation transcript at the start of multi-agent group chats
 * (ROUND_ROBIN, RANDOM, SWARM, MANUAL).
 *
 * <p>This helps agents understand who they're collaborating with and establishes
 * context for the discussion before the first turn.
 */
public class Example29AgentIntroductions {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // ── Agents with introductions ──────────────────────────────────────

        Agent architect = Agent.builder()
            .name("architect")
            .model(Settings.LLM_MODEL)
            .introduction(
                "I am the Software Architect. I focus on system design, scalability, "
                + "and technical trade-offs. I'll evaluate proposals from an architecture "
                + "perspective.")
            .instructions(
                "You are a software architect. Focus on system design, scalability, "
                + "and architectural patterns. Keep responses to 2-3 paragraphs.")
            .build();

        Agent securityEngineer = Agent.builder()
            .name("security_engineer")
            .model(Settings.LLM_MODEL)
            .introduction(
                "I am the Security Engineer. I focus on threat modeling, authentication, "
                + "authorization, and data protection. I'll flag any security concerns.")
            .instructions(
                "You are a security engineer. Focus on security implications, "
                + "vulnerabilities, and best practices. Keep responses to 2-3 paragraphs.")
            .build();

        Agent productManager = Agent.builder()
            .name("product_manager")
            .model(Settings.LLM_MODEL)
            .introduction(
                "I am the Product Manager. I focus on user needs, business value, "
                + "and delivery timelines. I'll ensure we stay focused on what matters "
                + "to customers.")
            .instructions(
                "You are a product manager. Focus on user needs, business value, "
                + "and prioritization. Keep responses to 2-3 paragraphs.")
            .build();

        // ── Team discussion with introductions ─────────────────────────────
        // Introductions are automatically prepended to the conversation so
        // each agent knows who's in the room.

        Agent designReview = Agent.builder()
            .name("design_review")
            .model(Settings.LLM_MODEL)
            .agents(architect, securityEngineer, productManager)
            .strategy(Strategy.ROUND_ROBIN)
            .maxTurns(6)
            .build();

        AgentResult result = runtime.run(designReview,
            "Review the design for a new user authentication system that uses "
            + "passkeys (WebAuthn) instead of passwords.");
        result.printResult();

        runtime.shutdown();
    }
}
