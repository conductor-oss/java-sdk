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
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 14 — Existing Workers as Agent Tools
 *
 * <p>Demonstrates using multiple {@code @Tool}-annotated methods alongside
 * each other in a single agent — exactly how existing worker functions
 * plug into the agent loop.
 *
 * <p>Shows:
 * <ul>
 *   <li>get_customer_data — look up a customer by ID</li>
 *   <li>get_order_history — fetch recent orders for a customer</li>
 *   <li>create_support_ticket — open a new ticket</li>
 * </ul>
 */
public class Example14ExistingWorkers {

    static class CustomerSupportTools {

        @Tool(name = "get_customer_data", description = "Fetch customer data by customer ID")
        public Map<String, Object> getCustomerData(String customerId) {
            Map<String, Object> customers = Map.of(
                "C001", Map.of("name", "Alice", "plan", "Enterprise", "since", "2021-03"),
                "C002", Map.of("name", "Bob", "plan", "Starter", "since", "2023-11")
            );
            Object found = customers.get(customerId);
            if (found != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) found;
                return result;
            }
            return Map.of("error", "Customer not found");
        }

        @Tool(name = "get_order_history", description = "Retrieve recent order history for a customer")
        public Map<String, Object> getOrderHistory(String customerId, int limit) {
            Map<String, List<Map<String, Object>>> orders = Map.of(
                "C001", List.of(
                    Map.of("id", "ORD-101", "amount", 250.00, "status", "delivered"),
                    Map.of("id", "ORD-098", "amount", 89.99, "status", "delivered")
                ),
                "C002", List.of(
                    Map.of("id", "ORD-110", "amount", 45.00, "status", "shipped")
                )
            );
            List<Map<String, Object>> customerOrders = orders.getOrDefault(customerId, List.of());
            int n = Math.min(limit > 0 ? limit : customerOrders.size(), customerOrders.size());
            return Map.of("customer_id", customerId, "orders", customerOrders.subList(0, n));
        }

        @Tool(name = "create_support_ticket", description = "Create a support ticket for a customer")
        public Map<String, Object> createSupportTicket(String customerId, String issue, String priority) {
            return Map.of(
                "ticket_id", "TKT-999",
                "customer_id", customerId,
                "issue", issue,
                "priority", priority != null && !priority.isEmpty() ? priority : "medium"
            );
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> tools = ToolRegistry.fromInstance(new CustomerSupportTools());

        Agent agent = Agent.builder()
            .name("customer_support")
            .model(Settings.LLM_MODEL)
            .tools(tools)
            .instructions(
                "You are a customer support agent. Use the available tools to look up "
                + "customer information, check order history, and create support tickets.")
            .build();

        AgentResult result = runtime.run(agent,
            "Customer C001 is asking about their recent orders. Look them up and summarize.");
        result.printResult();

        runtime.shutdown();
    }
}
