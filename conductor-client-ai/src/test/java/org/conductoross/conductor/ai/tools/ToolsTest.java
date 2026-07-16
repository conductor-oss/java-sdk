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
}
