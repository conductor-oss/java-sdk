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

import java.util.function.Consumer;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.FeedbackEvent;
import org.conductoross.conductor.ai.model.OCGMemoryStore;
import org.conductoross.conductor.ai.model.SemanticMemory;

/**
 * Example 118 — OCG-backed long-term memory with human good/bad feedback links.
 *
 * <p>Enable memory on an agent and the server-side compiler does two things
 * automatically once the agent is deployed:
 *
 * <ul>
 *   <li>BEFORE a run: relevant past memories (scoped to this agent/user) are
 *       retrieved from OCG and injected into the prompt — no tool call needed.</li>
 *   <li>AFTER a run: the conversation is summarized (Claude-style: durable facts,
 *       not the raw transcript) by a small internal summarizer agent and saved back
 *       to OCG as a memory.</li>
 * </ul>
 *
 * <p>Feedback is HUMAN-only. Agents never vote. Instead, the runtime hands a
 * {@link FeedbackEvent} — including signed <i>capability URLs</i> (good/bad) — to the
 * agent's {@code feedbackSink}. A human (e.g. a support engineer) clicks a link to
 * mark the memory good or bad; the link skips auth (its signature is the
 * authorization), so the clicker needs no OCG account. Here the sink just prints the
 * URLs as they'd appear in a Zendesk ticket comment.
 *
 * <p>The SDK emits a {@code longTermMemory} block on the agent config so the server
 * activates the feature; the OCG {@code credential} is a server-resolvable secret NAME
 * ({@code OCG_PUBLIC_KEY}), never the raw client token.
 *
 * <p>Requires the OCG instance to be started with a feedback-link secret
 * ({@code OCG_FEEDBACK_LINK_SECRET}) for the capability URLs to be minted.
 *
 * <pre>
 * OCG_INSTANCE_URL=https://test.contextgraph.io \
 * OCG_TOKEN=&lt;bearer-token&gt; \
 * ./gradlew :conductor-ai-examples:run -PmainClass=org.conductoross.conductor.ai.examples.Example118OcgMemory
 * </pre>
 */
public class Example118OcgMemory {

    public static void main(String[] args) {
        String ocgUrl = System.getenv().getOrDefault("OCG_INSTANCE_URL", "");
        // Unlike the server-side OCG retrieval tools (which resolve a credential
        // server-side), the memory store calls OCG directly, so it holds the bearer token.
        String ocgToken = System.getenv("OCG_TOKEN");
        if (ocgUrl.isEmpty()) {
            System.err.println(
                    "Set OCG_INSTANCE_URL to your OCG instance, e.g. https://test.contextgraph.io");
            return;
        }

        OCGMemoryStore store = OCGMemoryStore.builder()
                .url(ocgUrl)
                .agent("agent:support")
                .user("user:alice")
                .token(ocgToken)
                .build();

        // Deliver the good/bad links to a human. In production this would POST a
        // comment to the Zendesk ticket; here we just print what would be sent.
        Consumer<FeedbackEvent> zendeskSink = event -> {
            System.out.println("\n--- would post to Zendesk ticket ---");
            System.out.println("Saved memory: " + event.getMemoryKey());
            System.out.println("Summary: " + event.getSummary());
            if (event.getGoodUrl() != null) {
                System.out.println("  Was this helpful?  " + event.getGoodUrl());
                System.out.println("  Not helpful:       " + event.getBadUrl());
            }
            System.out.println("------------------------------------\n");
        };

        Agent agent = Agent.builder()
                .name("support")
                .model(Settings.LLM_MODEL)
                .instructions(
                        "You are a customer support agent. Use any relevant context from "
                                + "memory to personalize your answer. A memory labeled [bad] was "
                                + "flagged by a human — treat it with suspicion.")
                .semanticMemory(new SemanticMemory(store, 5, null))
                .feedbackSink(zendeskSink)
                .build();

        try (AgentRuntime runtime = new AgentRuntime()) {
            System.out.println("--- Turn 1 ---");
            AgentResult turn1 =
                    runtime.run(agent, "Hi, I'm Alice. I'm on the Enterprise plan and prefer email.");
            turn1.printResult();

            System.out.println("\n--- Turn 2 (should recall Alice's plan from memory) ---");
            AgentResult turn2 = runtime.run(agent, "What plan am I on again?");
            turn2.printResult();
        }
    }
}
