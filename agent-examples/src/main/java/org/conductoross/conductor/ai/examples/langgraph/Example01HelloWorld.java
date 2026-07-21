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
 * Example LangGraph 01 — Hello World using the native LangGraph4j SDK.
 *
 * <p>Builds a real LangGraph4j {@code AgentExecutor.Builder} (the same builder
 * the LangGraph4j docs use for the prebuilt ReAct agent) and hands it directly
 * to {@link AgentRuntime#run(AgentExecutor.Builder, String, Object...)} via the
 * drop-in overload so it runs on the durable Conductor runtime.
 */
public class Example01HelloWorld {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // apiKey is required by LangChain4j's builder but unused — Conductor
        // runs the LLM call on the server with server-registered credentials.
        ChatModel model = OpenAiChatModel.builder()
                .apiKey("conductor-server-handles-credentials")
                .modelName("gpt-4o-mini")
                .build();

        AgentExecutor.Builder agent = AgentExecutor.builder().chatModel(model);

        AgentResult result = runtime.run(
                agent,
                "Say hello and tell me a fun fact about state machines."
        );
        result.printResult();

        runtime.shutdown();
    }
}
