/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.ai;

import java.util.List;

import org.conductoross.conductor.ai.guardrail.Guardrail;
import org.conductoross.conductor.ai.guardrail.LLMGuardrail;
import org.conductoross.conductor.ai.guardrail.RegexGuardrail;
import org.conductoross.conductor.ai.model.GuardrailDef;
import org.conductoross.conductor.ai.model.GuardrailResult;
import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.ApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for local tool guardrail worker registration. */
class ToolGuardrailRegistrationTest {
    private static final String DOMAIN = "guardrail-test-domain";

    @Test
    void local_guardrail_is_registered_for_every_tool_type() {
        AgentRuntime runtime = runtime();
        try {
            for (String toolType : List.of("worker", "http", "api", "mcp", "agent_tool", "human",
                    "pull_workflow_messages", "rag", "generate_pdf", "generate_image", "generate_audio", "generate_video")) {
                ToolDef tool = ToolDef.builder().name("tool_" + toolType).toolType(toolType)
                        .guardrails(List.of(local("local_" + toolType))).build();
                runtime.prepareWorkers(agent(tool), DOMAIN);
                String taskName = tool.getName() + "_output_guardrail";
                assertTrue(runtime.isWorkerRegisteredForTest(taskName), "local tool guardrail must have a worker");
                assertEquals(DOMAIN, runtime.workerDomainForTest(taskName), "guardrail must poll the execution domain");
            }
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void server_and_external_guardrails_do_not_create_local_workers() {
        AgentRuntime runtime = runtime();
        try {
            ToolDef tool = ToolDef.builder().name("server_owned").toolType("http").guardrails(List.of(
                    RegexGuardrail.builder().name("regex").patterns("secret").build(),
                    LLMGuardrail.builder().name("judge").model("openai/gpt-4o-mini").policy("safe").build(),
                    Guardrail.external("external_guard").build())).build();
            runtime.prepareWorkers(agent(tool), DOMAIN);
            assertFalse(runtime.isWorkerRegisteredForTest("server_owned_output_guardrail"));
        } finally {
            runtime.shutdown();
        }
    }

    private static GuardrailDef local(String name) {
        return Guardrail.of(name, content -> GuardrailResult.pass()).build();
    }

    private static Agent agent(ToolDef tool) {
        return Agent.builder().name("registration_agent_" + tool.getName()).model("openai/gpt-4o-mini")
                .tools(List.of(tool)).build();
    }

    private static AgentRuntime runtime() {
        return new AgentRuntime(ApiClient.builder().basePath("http://127.0.0.1:1/api").connectTimeout(1).readTimeout(1).build());
    }
}
