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
 * Example 65 — Parallel Agents with Tools (each branch has its own tools)
 *
 * <p>Extends the basic parallel pattern (Example 07) by giving each parallel
 * branch its own domain tools. All branches run concurrently and each
 * independently calls its tools.
 *
 * <pre>
 * parallel_analysis (PARALLEL)
 * ├── financial_analyst  (tools: [check_balance])
 * └── order_analyst      (tools: [lookup_order])
 * </pre>
 *
 * Both analysts run at the same time on the same input.
 */
public class Example65ParallelWithTools {

    static class FinancialTools {
        @Tool(name = "check_balance_65", description = "Check the balance of a bank account")
        public Map<String, Object> checkBalance(String accountId) {
            return Map.of(
                "account_id", accountId,
                "balance", 5432.10,
                "currency", "USD"
            );
        }
    }

    static class OrderTools {
        @Tool(name = "lookup_order_65", description = "Look up the status of an order")
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
        List<ToolDef> financialTools = ToolRegistry.fromInstance(new FinancialTools());
        List<ToolDef> orderTools = ToolRegistry.fromInstance(new OrderTools());

        Agent financialAnalyst = Agent.builder()
            .name("financial_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a financial analyst. Use check_balance_65 to look up the "
                + "account mentioned. Report the balance and any financial observations.")
            .tools(financialTools)
            .build();

        Agent orderAnalyst = Agent.builder()
            .name("order_analyst")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are an order analyst. Use lookup_order_65 to check the order "
                + "mentioned. Report the status and delivery timeline.")
            .tools(orderTools)
            .build();

        // Both analysts run concurrently
        Agent analysis = Agent.builder()
            .name("parallel_analysis_65")
            .model(Settings.LLM_MODEL)
            .agents(financialAnalyst, orderAnalyst)
            .strategy(Strategy.PARALLEL)
            .build();

        AgentResult result = runtime.run(analysis,
            "Check account ACC-200 balance and look up order ORD-300 status.");
        result.printResult();

        runtime.shutdown();
    }
}
