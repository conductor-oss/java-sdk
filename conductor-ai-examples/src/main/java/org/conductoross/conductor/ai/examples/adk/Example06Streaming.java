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

import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.model.AgentResult;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

/**
 * Example Adk 06 — Streaming
 *
 * <p>Java port of <code>sdk/python/examples/adk/06_streaming.py</code>.
 *
 * <p>Demonstrates: a documentation lookup ADK agent with a streaming-capable
 * pattern. The Python source shows {@code runtime.stream(...)} as an
 * alternative; this Java port uses the synchronous {@code runtime.run}.
 */
public class Example06Streaming {

    @Schema(description = "Search the product documentation.")
    public static Map<String, Object> searchDocumentation(
            @Schema(name = "query", description = "Search query") String query) {
        Map<String, Map<String, Object>> docs = new LinkedHashMap<>();
        docs.put("installation", Map.of(
            "title", "Installation Guide",
            "content", "Run `pip install mypackage`. Requires Python 3.9+."));
        docs.put("authentication", Map.of(
            "title", "Authentication",
            "content", "Use API keys via the X-API-Key header. Keys are managed in the dashboard."));
        docs.put("rate limits", Map.of(
            "title", "Rate Limiting",
            "content", "Free tier: 100 req/min. Pro: 1000 req/min. Enterprise: unlimited."));

        String q = query.toLowerCase();
        for (Map.Entry<String, Map<String, Object>> entry : docs.entrySet()) {
            if (q.contains(entry.getKey())) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("found", true);
                r.putAll(entry.getValue());
                return r;
            }
        }
        return Map.of("found", false, "message", "No matching documentation found.");
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent techWriter = LlmAgent.builder()
            .name("docs_assistant")
            .description("Looks up product documentation and answers user questions about it.")
            .model(Settings.LLM_MODEL)
            .instruction(
                "You are a documentation assistant. Use the search tool to find "
                + "relevant docs and provide clear, well-formatted answers.")
            .tools(FunctionTool.create(Example06Streaming.class, "searchDocumentation"))
            .build();

        AgentResult result = runtime.run(techWriter, "How do I authenticate with the API?");
        result.printResult();

        runtime.shutdown();
    }
}
