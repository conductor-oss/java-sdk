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
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentHandle;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/** A normal chat agent calling a server-side, branchless decision SWITCH tool. */
public class ExampleDecisionTool {
    public static Agent supportAgent() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model", "jev-1.13");
        config.put("questions", Map.of("department", Map.of(
                "type", "choice", "instructions", "Choose the correct support department.",
                "choices", Map.of("billing", "Payments and invoices",
                        "technical", "Product errors and troubleshooting"))));
        String provider = System.getenv("CONDUCTOR_DECISION_PROVIDER");
        if (provider != null) config.put("provider", provider);
        ToolDef classify = ToolDef.builder().name("classify_request_java")
                .description("Classify the customer request before answering.").toolType("decision")
                .inputSchema(Map.of("type", "object", "properties", Map.of("state", Map.of("type", "string")),
                        "required", List.of("state")))
                .config(config).build();
        return Agent.builder().name("decision_tool_support_java")
                .model(System.getenv().getOrDefault("CONDUCTOR_AGENT_LLM_MODEL", "openai/gpt-4o-mini"))
                .instructions("Call classify_request_java before answering the customer.")
                .tools(classify).build();
    }

    public static void main(String[] args) {
        try (AgentRuntime runtime = new AgentRuntime()) {
            AgentHandle handle = runtime.start(supportAgent(), "I was charged twice on my latest invoice.");
            System.out.println("Execution: " + handle.getExecutionId());
            AgentResult result = handle.waitForResult(120_000, 500);
            if (!result.isSuccess()) throw new IllegalStateException(result.getError());
            result.printResult();
        }
    }
}
