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
import org.conductoross.conductor.ai.model.PromptTemplate;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Example 34 — Prompt Templates
 *
 * <p>Demonstrates using server-side prompt templates for agent instructions.
 * Templates are stored once on the Conductor server and referenced by name.
 * Variables substitute {@code ${var}} placeholders at execution time.
 *
 * <p>Requires a template named {@code "order-support"} to exist on the server.
 * Create it via the Conductor UI or API with a body like:
 * <pre>
 *   You are an order support specialist. Maximum refund authority: ${max_refund}.
 *   For issues beyond your authority, escalate to ${escalation_email}.
 * </pre>
 *
 * <p>If the template does not exist on the server, the agent will still run
 * with whatever fallback the server applies for missing templates.
 */
public class Example34PromptTemplates {

    static class OrderTools {
        @Tool(name = "lookup_order_34", description = "Look up an order by ID")
        public Map<String, Object> lookupOrder(String orderId) {
            return Map.of("order_id", orderId, "status", "shipped", "eta", "2 days");
        }

        @Tool(name = "lookup_customer_34", description = "Look up customer details by email")
        public Map<String, Object> lookupCustomer(String email) {
            return Map.of("email", email, "name", "Jane Doe", "tier", "premium");
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        List<ToolDef> tools = ToolRegistry.fromInstance(new OrderTools());

        // Agent using a server-side prompt template with variable substitution
        Agent orderAgent = Agent.builder()
            .name("order_assistant_34")
            .model(Settings.LLM_MODEL)
            .instructionsTemplate(new PromptTemplate(
                "order-support",
                Map.of("max_refund", "$500", "escalation_email", "help@acme.com")
            ))
            .tools(tools)
            .build();

        AgentResult result = runtime.run(orderAgent, "Can you check order #12345?");
        result.printResult();

        runtime.shutdown();
    }
}
