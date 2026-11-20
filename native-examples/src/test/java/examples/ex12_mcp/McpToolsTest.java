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
package examples.ex12_mcp;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.McpCall.McpResult;
import org.conductoross.conductor.sdk.McpCall.McpTool;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex12_mcp.McpTools.Request;
import examples.ex12_mcp.McpTools.Used;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolsTest {

    @Test
    void listsThenCallsTheTools() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(McpTools.class);
        env.mcp("list_tools").respond(List.of(new McpTool("get_weather", "weather", Map.of()), new McpTool("add", "adds", Map.of())));
        env.mcp("weather").respond(text("Rain in Oslo"));
        env.mcp("add").respond(text("42"));

        Used used = env.start(McpTools::use, new Request("http://mcp", "Oslo")).result();

        assertEquals(new Used(List.of("get_weather", "add"), "Rain in Oslo", "42"), used);
    }

    private static McpResult text(String text) {
        return new McpResult(List.of(Map.of("type", "text", "text", text)), false);
    }
}
