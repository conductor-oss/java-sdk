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
package org.conductoross.conductor.sdk;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Entries;
import org.conductoross.conductor.sdk.internal.Json;

/**
 * Calls an MCP (Model Context Protocol) server from the Conductor server: no MCP client in the
 * worker. {@code listTools()} is a LIST_MCP_TOOLS task and {@code call(...)} a CALL_MCP_TOOL task,
 * each visible in the graph and retried like any task.
 */
public final class McpCall {

    /** A tool an MCP server offers. */
    public record McpTool(String name, String description, Object inputSchema) {}

    /** The result of a tool call; {@code isError} is the tool's own error flag. */
    public record McpResult(List<Map<String, Object>> content, Boolean isError) {

        /** The text of all text content items, joined by newlines. */
        public String text() {
            StringBuilder sb = new StringBuilder();
            if (content != null) {
                for (Map<String, Object> item : content) {
                    Object text = item.get("text");
                    if (text != null) {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(text);
                    }
                }
            }
            return sb.toString();
        }
    }

    private final String ref;
    private String server;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int retries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);

    McpCall(String ref) {
        this.ref = ref;
    }

    /** The MCP server's streamable HTTP or SSE endpoint, e.g. {@code https://tools.example.com/mcp}. */
    public McpCall server(String url) {
        this.server = url;
        return this;
    }

    public McpCall header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public McpCall retries(int count, Duration delay) {
        this.retries = count;
        this.retryDelay = delay;
        return this;
    }

    private Command command(Command.Kind kind, String type) {
        Command c = new Command(kind, ref, ref);
        c.explicitRef = ref;
        c.input.put("mcpServer", server);
        if (!headers.isEmpty()) {
            c.input.put("headers", headers);
        }
        c.fields.put("taskDefinition", Entries.retryingDefinition(ref, retries, Math.max(1, retryDelay.toSeconds())));
        return c;
    }

    /** The tools the server offers. */
    @SuppressWarnings("unchecked")
    public List<McpTool> listTools() {
        Command c = command(Command.Kind.MCP_LIST, "LIST_MCP_TOOLS");
        c.outputKey = "tools";
        c.resultType = Json.MAPPER.getTypeFactory().constructCollectionType(List.class, McpTool.class);
        return (List<McpTool>) Engine.current().call(c);
    }

    /** Calls a tool with arguments. */
    public McpResult call(String tool, Map<String, ?> arguments) {
        Command c = command(Command.Kind.MCP_CALL, "CALL_MCP_TOOL");
        c.input.put("method", tool);
        Map<String, Object> args = new LinkedHashMap<>();
        arguments.forEach((k, v) -> args.put(k, Json.plain(v)));
        c.input.put("arguments", args);
        c.resultType = McpResult.class;
        return (McpResult) Engine.current().call(c);
    }
}
