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

import java.time.LocalDate;

import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Example LangGraph 02 — ReAct Agent with Tools using native LangGraph4j SDK.
 *
 * <p>Java port (concepts) of
 * <code>sdk/python/examples/langgraph/02_react_with_tools.py</code>. Builds a
 * real LangGraph4j {@code AgentExecutor.Builder} (a ReAct {@code StateGraph})
 * and hands it straight to {@link Agentspan#run} via the drop-in overload.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Defining tools with native {@link Tool @Tool} on a POJO</li>
 *   <li>Passing the tool POJO straight to
 *       {@link Agentspan#run(AgentExecutor.Builder, String, Object...)} via
 *       the drop-in overload — internally LangGraph4j calls
 *       {@code toolsFromObject(...)}</li>
 *   <li>Calculator, word count, and date utilities</li>
 * </ul>
 */
public class Example02ReactWithTools {

    /** Tool POJO. LangGraph4j discovers @Tool methods via reflection. */
    static class UtilityTools {

        @Tool("Add two integers and return the sum.")
        public String add(@P("a") int a, @P("b") int b) {
            return String.valueOf(a + b);
        }

        @Tool("Count the number of words in the provided text.")
        public String countWords(@P("text") String text) {
            if (text == null || text.trim().isEmpty()) {
                return "0 words";
            }
            int n = text.trim().split("\\s+").length;
            return n + " words";
        }

        @Tool("Return today's date in YYYY-MM-DD format.")
        public String getToday() {
            return LocalDate.now().toString();
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // apiKey is required by LangChain4j's builder but unused — Agentspan
        // runs the LLM call on the server with server-registered credentials.
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("agentspan-server-handles-credentials")
                .modelName("gpt-4o-mini")
                .build();

        UtilityTools tools = new UtilityTools();
        AgentExecutor.Builder agent = AgentExecutor.builder().chatModel(model);
        agent.toolsFromObject(tools);

        AgentResult result = runtime.run(
                agent,
                "What is 17 + 25? Also count words in 'the quick brown fox jumps'. "
                + "And what is today's date?",
                tools
        );
        System.out.println("Status: " + result.getStatus());
        result.printResult();

        runtime.shutdown();
    }
}
