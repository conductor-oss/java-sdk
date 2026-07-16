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
package org.conductoross.conductor.ai.examples.adk;

import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.model.AgentResult;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.GoogleSearchTool;

/**
 * Example Adk 36 — Built-in Tools (Google Search)
 *
 * <p>Demonstrates: native ADK ships ready-made tool instances like
 * {@link GoogleSearchTool} that the agent can use without any local
 * implementation. The bridge detects these subclasses of
 * {@code BaseTool} and emits the corresponding {@code _type} marker on
 * the wire; the server normalizer wires the built-in handler in the
 * compiled workflow.
 *
 * <p>Same pattern works for {@code BuiltInCodeExecutionTool} and
 * {@code McpToolset} (and any other {@code BaseToolset}, which the bridge
 * expands into its constituent tools via {@code getTools(null)}).
 */
public class Example36BuiltInTools {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent toolUser = LlmAgent.builder()
                .name("research_assistant")
                .description("An assistant that can search the web with the built-in Google Search tool.")
                .model(Settings.LLM_MODEL)
                .instruction(
                        "You are a research assistant. When the user asks about a topic, "
                        + "use the google_search tool to find current information, then "
                        + "summarize the most relevant facts in 2-3 sentences.")
                .tools(new GoogleSearchTool())
                .build();

        AgentResult result = runtime.run(toolUser,
                "What are the most recent developments in fusion energy research?");
        result.printResult();

        runtime.shutdown();
    }
}
