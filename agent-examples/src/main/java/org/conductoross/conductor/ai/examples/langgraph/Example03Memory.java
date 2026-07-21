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
package org.conductoross.conductor.ai.examples.langgraph;

import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Example LangGraph 03 — Multi-turn conversation (memory-style).
 *
 * <p>Inspired by <code>sdk/python/examples/langgraph/03_memory.py</code> which
 * attaches a {@code MemorySaver} checkpointer to {@code create_agent}. In this
 * Java/Conductor port we demonstrate the same surface — a single agent invoked
 * three times in sequence — but without an in-process checkpointer. The history
 * is passed back to the LLM directly inside the prompt for each turn, which is
 * the simplest portable pattern when running on the durable Conductor runtime.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Reusing a single {@link AgentExecutor.Builder}-built agent for
 *       multiple turns via the drop-in {@link AgentRuntime#run} overload</li>
 *   <li>Pure prompt-based history (no in-memory checkpointer required)</li>
 *   <li>How an LLM can recall facts when given prior context</li>
 * </ul>
 */
public class Example03Memory {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // apiKey is required by LangChain4j's builder but unused — Conductor
        // runs the LLM call on the server with server-registered credentials.
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("conductor-server-handles-credentials")
                .modelName("gpt-4o-mini")
                .build();

        AgentExecutor.Builder agent = AgentExecutor.builder().chatModel(model);

        // The drop-in overload does not take a system prompt — fold the
        // persona into each user message instead.
        String persona =
                "You are a friendly assistant. Pay close attention to facts the user shares.\n\n";

        System.out.println("=== Turn 1: Introduce a name ===");
        AgentResult turn1 = runtime.run(
                agent,
                persona + "My name is Alice. Please remember that."
        );
        turn1.printResult();

        System.out.println("\n=== Turn 2: Ask the agent to recall ===");
        AgentResult turn2 = runtime.run(
                agent,
                persona + "Earlier I told you my name was Alice. What is my name?"
        );
        turn2.printResult();

        System.out.println("\n=== Turn 3: Continue the conversation ===");
        AgentResult turn3 = runtime.run(
                agent,
                persona + "Tell me one fun fact about the name Alice."
        );
        turn3.printResult();

        runtime.shutdown();
    }
}
