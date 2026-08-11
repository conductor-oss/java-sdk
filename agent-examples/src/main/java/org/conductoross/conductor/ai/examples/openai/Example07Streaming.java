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
package org.conductoross.conductor.ai.examples.openai;

import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.frameworks.OpenAIAgent;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example OpenAi 07 — Streaming
 *
 * <p>Java port of <code>sdk/python/examples/openai/07_streaming.py</code>.
 *
 * <p>Demonstrates: an OpenAI Agents SDK support agent backed by a single
 * knowledge-base tool. The Python original is named "streaming" because it
 * is meant to be invoked via {@code runtime.stream(...)}, but its actual
 * call site uses {@code runtime.run(...)}; we mirror the run path here.
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini</li>
 * </ul>
 */
public class Example07Streaming {

    static class KnowledgeBaseTools {

        @Tool(name = "search_knowledge_base", description = "Search the knowledge base for relevant information.")
        public String searchKnowledgeBase(String query) {
            Map<String, String> knowledge = new LinkedHashMap<>();
            knowledge.put("return policy",
                    "Returns accepted within 30 days with receipt. "
                            + "Electronics have a 15-day return window.");
            knowledge.put("shipping",
                    "Free shipping on orders over $50. "
                            + "Standard delivery: 3-5 business days.");
            knowledge.put("warranty",
                    "All products come with a 1-year manufacturer warranty. "
                            + "Extended warranty available for electronics.");

            String queryLower = query.toLowerCase();
            for (Map.Entry<String, String> entry : knowledge.entrySet()) {
                if (queryLower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return "No relevant information found for your query.";
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = OpenAIAgent.builder()
                .name("support_agent")
                .instructions(
                        "You are a customer support agent. Use the knowledge base to answer "
                                + "questions accurately. If you can't find the answer, say so honestly.")
                .model(Settings.LLM_MODEL)
                .tools(new KnowledgeBaseTools())
                .build();

        AgentResult result = runtime.run(agent, "What's your return policy for electronics?");
        result.printResult();

        runtime.shutdown();
    }
}
