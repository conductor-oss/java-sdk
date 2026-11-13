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

import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.model.AgentHandle;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/** Use a decision tool to choose one of two chat agents, without an SDK worker. */
public class ExampleDecisionRouter {
    public static Agent supportRouter() {
        String chatModel = System.getenv().getOrDefault("CONDUCTOR_AGENT_LLM_MODEL", "openai/gpt-4o-mini");
        Agent billing = Agent.builder().name("billing_java").model(chatModel)
                .instructions("Help the customer with billing and invoice questions.").build();
        Agent technical = Agent.builder().name("technical_java").model(chatModel)
                .instructions("Help the customer troubleshoot product issues.").build();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model", "jev-1.13");
        config.put("questions", Map.of("agent", Map.of(
                "type", "choice", "instructions", "Choose the right support team.",
                "choices", Map.of("billing_java", "Payments and invoices",
                        "technical_java", "Product errors and troubleshooting"))));
        String provider = System.getenv("CONDUCTOR_DECISION_PROVIDER");
        if (provider != null) config.put("provider", provider);
        ToolDef selector = ToolDef.builder().name("support_selector_java")
                .description("Choose the support agent that should handle the request.")
                .toolType("decision").config(config).build();
        return Agent.builder().name("decision_support_router_java").strategy(Strategy.ROUTER)
                .router(selector).agents(billing, technical).maxTurns(1).synthesize(false).build();
    }

    public static void main(String[] args) {
        try (AgentRuntime runtime = new AgentRuntime()) {
            Agent agent = supportRouter();
            if (args.length == 0 || !"--run".equals(args[0])) {
                System.out.println(runtime.plan(agent));
                return;
            }
            runtime.deploy(agent);
            AgentHandle handle = runtime.start(agent, "The customer reports a duplicate charge on the latest invoice.");
            System.out.println("Execution: " + handle.getExecutionId());
            AgentResult result = handle.waitForResult(120_000, 500);
            if (!result.isSuccess()) throw new IllegalStateException(result.getError());
            result.printResult();
        }
    }
}
