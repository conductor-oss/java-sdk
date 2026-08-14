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
import java.util.concurrent.CompletableFuture;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.ToolDef;

/**
 * Tool name collision — two agents sharing one tool name.
 *
 * <p>Agent names are unique. Only the tool name is shared:
 * <ul>
 *   <li>agent_alpha — get_secret_code returns ALPHA-111</li>
 *   <li>agent_bravo — get_secret_code returns BRAVO-222</li>
 * </ul>
 *
 * <p>Only agent_alpha runs. It answers BRAVO-222.
 *
 * <p>A tool name is the worker queue name. Registering agent_bravo replaces
 * agent_alpha's handler for that queue.
 */
public class ToolNameCollisionDemo {

    public static class AlphaTools {
        @Tool(name = "get_secret_code", description = "Return the secret code")
        public String getSecretCode() {
            return "ALPHA-111";
        }
    }

    public static class BravoTools {
        @Tool(name = "get_secret_code", description = "Return the secret code")
        public String getSecretCode() {
            return "BRAVO-222";
        }
    }

    public static void main(String[] args) throws Exception {
        AgentRuntime runtime = new AgentRuntime();

        List<ToolDef> alphaTools = ToolRegistry.fromInstance(new AlphaTools());
        List<ToolDef> bravoTools = ToolRegistry.fromInstance(new BravoTools());

        Agent alpha = Agent.builder()
            .name("agent_alpha")
            .model(Settings.LLM_MODEL)
            .instructions("Call get_secret_code and report the code verbatim.")
            .tools(alphaTools)
            .build();

        Agent bravo = Agent.builder()
            .name("agent_bravo")
            .model(Settings.LLM_MODEL)
            .instructions("Call get_secret_code and report the code verbatim.")
            .tools(bravoTools)
            .build();

        System.out.println("shared tool name: " + alphaTools.get(0).getName());

        CompletableFuture<AgentResult> future = runtime.runAsync(alpha, "What is the secret code?");

        Thread.sleep(300);
        runtime.prepareWorkers(bravo);
        System.out.println("registered agent_bravo while agent_alpha was in flight");

        String output = String.valueOf(future.get().getOutput());

        System.out.println();
        System.out.println("agent_alpha answered: " + output);
        System.out.println("expected ALPHA-111  : " + output.contains("ALPHA-111"));
        System.out.println("got BRAVO-222       : " + output.contains("BRAVO-222"));
        System.out.println();
        System.out.println(output.contains("BRAVO-222")
            ? "HIJACKED — agent_alpha ran agent_bravo's implementation"
            : "not reproduced on this run");

        runtime.shutdown();
        System.exit(0);
    }
}
