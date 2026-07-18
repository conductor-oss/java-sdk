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
package org.conductoross.conductor.ai.tools;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.guardrail.Guardrail;
import org.conductoross.conductor.ai.model.GuardrailResult;
import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the tool builders — name + toolType wiring (no server). */
class ToolsTest {

    @Test
    void httpToolShape() {
        ToolDef t = HttpTool.builder()
                .name("fetch")
                .description("d")
                .url("http://x")
                .method("GET")
                .build();
        assertEquals("fetch", t.getName());
        assertEquals("http", t.getToolType());
    }

    @Test
    void httpToolRequiresName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HttpTool.builder().url("http://x").build());
    }

    @Test
    void mcpToolShape() {
        ToolDef t = McpTool.builder()
                .name("m")
                .description("d")
                .serverUrl("http://mcp")
                .build();
        assertEquals("mcp", t.getToolType());
        assertEquals("m", t.getName());
    }

    @Test
    void humanToolShape() {
        ToolDef t = HumanTool.create("ask", "d");
        assertEquals("human", t.getToolType());
        assertEquals("ask", t.getName());
    }

    @Test
    void pdfToolShape() {
        assertEquals("generate_pdf", PdfTool.create("p", "d").getToolType());
    }

    @Test
    void waitForMessageToolShape() {
        assertEquals(
                "pull_workflow_messages", WaitForMessageTool.create("w", "d").getToolType());
    }

    @Test
    void imageToolShape() {
        ToolDef t = MediaTools.imageTool("img", "d", "openai", "dall-e-3");
        assertEquals("img", t.getName());
        assertNotNull(t.getToolType());
    }

    @Test
    void withGuardrails_preserves_every_tool_field() {
        Agent child = Agent.builder().name("child").model("openai/gpt-4o-mini").build();
        ToolDef original = ToolDef.builder()
                .name("complete")
                .description("original description")
                .inputSchema(Map.of("type", "object"))
                .outputSchema(Map.of("type", "string"))
                .func(input -> "ok")
                .approvalRequired(true)
                .timeoutSeconds(42)
                .retryCount(5)
                .retryDelaySeconds(7)
                .retryPolicy("exponential_backoff")
                .toolType("agent_tool")
                .config(Map.of("endpoint", "https://example.test"))
                .credentials(List.of("API_KEY"))
                .maxCalls(3)
                .agentRef(child)
                .stateful(true)
                .build();

        ToolDef guarded = original.withGuardrails(List.of(
                Guardrail.of("safe", content -> GuardrailResult.pass()).build()));

        assertEquals(original.getName(), guarded.getName());
        assertEquals(original.getDescription(), guarded.getDescription());
        assertEquals(original.getInputSchema(), guarded.getInputSchema());
        assertEquals(original.getOutputSchema(), guarded.getOutputSchema());
        assertSame(original.getFunc(), guarded.getFunc());
        assertEquals(original.isApprovalRequired(), guarded.isApprovalRequired());
        assertEquals(original.getTimeoutSeconds(), guarded.getTimeoutSeconds());
        assertEquals(original.getRetryCount(), guarded.getRetryCount());
        assertEquals(original.getRetryDelaySeconds(), guarded.getRetryDelaySeconds());
        assertEquals(original.getRetryPolicy(), guarded.getRetryPolicy());
        assertEquals(original.getToolType(), guarded.getToolType());
        assertEquals(original.getConfig(), guarded.getConfig());
        assertEquals(original.getCredentials(), guarded.getCredentials());
        assertEquals(original.getMaxCalls(), guarded.getMaxCalls());
        assertSame(original.getAgentRef(), guarded.getAgentRef());
        assertEquals(original.isStateful(), guarded.isStateful());
        assertEquals(1, guarded.getGuardrails().size());
        assertThrows(UnsupportedOperationException.class, () -> guarded.getGuardrails().add(null));
    }
}
