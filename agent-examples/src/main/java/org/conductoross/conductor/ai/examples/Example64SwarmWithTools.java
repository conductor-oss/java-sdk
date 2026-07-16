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
import org.conductoross.conductor.ai.handoff.OnTextMention;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 64 — Swarm with Tools (sub-agents have their own domain tools)
 *
 * <p>Extends the basic swarm pattern (Example 17) by giving each specialist
 * its own tools. The swarm transfer mechanism works alongside tools: the LLM
 * can call domain tools AND transfer tools in the same turn.
 *
 * <p>Flow:
 * <ol>
 *   <li>Front-line support triages the request</li>
 *   <li>Transfers to billing_specialist or order_specialist</li>
 *   <li>Specialist uses its domain tool (check_balance / lookup_order)</li>
 * </ol>
 */
public class Example64SwarmWithTools {

    static class BillingTools {
        @Tool(name = "check_balance", description = "Check the balance of a bank account")
        public Map<String, Object> checkBalance(String accountId) {
            return Map.of(
                "account_id", accountId,
                "balance", 5432.10,
                "currency", "USD"
            );
        }
    }

    static class OrderTools {
        @Tool(name = "lookup_order", description = "Look up the status of an order")
        public Map<String, Object> lookupOrder(String orderId) {
            return Map.of(
                "order_id", orderId,
                "status", "shipped",
                "eta", "2 days"
            );
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> billingTools = ToolRegistry.fromInstance(new BillingTools());
        List<ToolDef> orderTools = ToolRegistry.fromInstance(new OrderTools());

        // ── Specialist agents with domain tools ────────────────────────────

        Agent billingSpecialist = Agent.builder()
            .name("billing_specialist")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a billing specialist. Use the check_balance tool to look up "
                + "account balances. Include the balance amount in your response.")
            .tools(billingTools)
            .build();

        Agent orderSpecialist = Agent.builder()
            .name("order_specialist")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are an order specialist. Use the lookup_order tool to check "
                + "order status. Include the shipping status and ETA in your response.")
            .tools(orderTools)
            .build();

        // ── Front-line support with swarm strategy ─────────────────────────

        Agent support = Agent.builder()
            .name("support_64")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are front-line customer support. Triage customer requests. "
                + "Transfer to billing_specialist for account/payment questions, "
                + "order_specialist for shipping/order questions.")
            .agents(billingSpecialist, orderSpecialist)
            .strategy(Strategy.SWARM)
            .handoffs(
                OnTextMention.of("billing", "billing_specialist"),
                OnTextMention.of("order", "order_specialist"))
            .maxTurns(3)
            .build();

        System.out.println("=== Scenario 1: Billing question ===");
        AgentResult r1 = runtime.run(support,
            "What's the balance on account ACC-456?");
        r1.printResult();

        System.out.println("\n=== Scenario 2: Order question ===");
        AgentResult r2 = runtime.run(support,
            "Where is my order ORD-789?");
        r2.printResult();

        runtime.shutdown();
    }
}
