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
 * Example 05 — Multi-Agent Handoffs
 *
 * <p>Demonstrates multi-agent orchestration with handoff strategy.
 * The orchestrator LLM decides which specialist sub-agent to invoke.
 */
public class Example05Handoffs {

    static class BillingTools {
        @Tool(name = "check_balance", description = "Check the balance of a bank account")
        public Map<String, Object> checkBalance(String accountId) {
            return Map.of("account_id", accountId, "balance", 5432.10, "currency", "USD");
        }
    }

    static class TechnicalTools {
        @Tool(name = "lookup_order", description = "Look up the status of an order")
        public Map<String, Object> lookupOrder(String orderId) {
            return Map.of("order_id", orderId, "status", "shipped", "eta", "2 days");
        }
    }

    static class SalesTools {
        @Tool(name = "get_pricing", description = "Get pricing information for a product")
        public Map<String, Object> getPricing(String product) {
            return Map.of("product", product, "price", 99.99, "discount", "10% off");
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> billingTools = ToolRegistry.fromInstance(new BillingTools());
        List<ToolDef> technicalTools = ToolRegistry.fromInstance(new TechnicalTools());
        List<ToolDef> salesTools = ToolRegistry.fromInstance(new SalesTools());

        // Specialist agents with domain tools
        Agent billingAgent = Agent.builder()
            .name("billing")
            .model(Settings.LLM_MODEL)
            .instructions("You handle billing questions: balances, payments, invoices.")
            .tools(billingTools)
            .build();

        Agent technicalAgent = Agent.builder()
            .name("technical")
            .model(Settings.LLM_MODEL)
            .instructions("You handle technical questions: order status, shipping, returns.")
            .tools(technicalTools)
            .build();

        Agent salesAgent = Agent.builder()
            .name("sales")
            .model(Settings.LLM_MODEL)
            .instructions("You handle sales questions: pricing, products, promotions.")
            .tools(salesTools)
            .build();

        // Orchestrator with handoff strategy
        Agent support = Agent.builder()
            .name("support")
            .model(Settings.LLM_MODEL)
            .instructions("Route customer requests to the right specialist: billing, technical, or sales.")
            .agents(billingAgent, technicalAgent, salesAgent)
            .strategy(Strategy.HANDOFF)
            .build();

        AgentResult result = runtime.run(support, "What's the balance on account ACC-123?");
        result.printResult();

        runtime.shutdown();
    }
}
