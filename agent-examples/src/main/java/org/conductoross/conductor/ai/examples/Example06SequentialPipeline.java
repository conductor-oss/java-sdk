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

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example 06 — Sequential Pipeline
 *
 * <p>Demonstrates chaining agents into a sequential pipeline using {@link Agent#then(Agent)}.
 * Each agent's output becomes the next agent's input.
 */
public class Example06SequentialPipeline {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // Step 1: Researcher gathers information
        Agent researcher = Agent.builder()
            .name("researcher")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a researcher. Given a topic, provide 3-5 key facts and current trends. "
                + "Be concise and factual. Format as a numbered list.")
            .build();

        // Step 2: Writer turns research into an article
        Agent writer = Agent.builder()
            .name("writer")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are a content writer. Given research findings, write a well-structured "
                + "2-3 paragraph article for a general audience. "
                + "Make it engaging and informative.")
            .build();

        // Step 3: Editor polishes the article
        Agent editor = Agent.builder()
            .name("editor")
            .model(Settings.LLM_MODEL)
            .instructions(
                "You are an editor. Review and improve the provided article. "
                + "Fix grammar, improve clarity, and ensure it's publication-ready. "
                + "Add a compelling title at the beginning.")
            .build();

        // Chain into a sequential pipeline using .then()
        Agent contentPipeline = researcher.then(writer).then(editor);

        System.out.println("Pipeline: " + contentPipeline.getName());
        System.out.println("Sub-agents: " + contentPipeline.getAgents().size());

        AgentResult result = runtime.run(contentPipeline,
            "Write an article about the future of renewable energy");
        result.printResult();

        runtime.shutdown();
    }
}
