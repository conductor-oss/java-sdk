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
package org.conductoross.conductor.ai.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.exceptions.AgentAPIException;
import org.conductoross.conductor.ai.internal.JsonMapper;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the OCG-backed memory HTTP adapter ({@link OCGMemoryStore}) and the
 * conversation summary helper. Pure in-process: a stub {@link OCGMemoryStore.Transport}
 * captures requests and serves canned responses (parity with the Python tests'
 * {@code httpx.MockTransport}).
 *
 * <p>Mirrors {@code tests/unit/test_ocg_memory_store.py}. The runtime save/retrieval
 * hook tests from the Python suite are intentionally omitted — this SDK has no
 * client-side agent run loop (the server-side compiler drives retrieval/distill/save
 * on the deployed path); see the PR body.
 */
class OcgMemoryStoreTest {

    /** A captured HTTP exchange. */
    private static final class Captured {
        String method;
        String url;
        JsonNode body;
    }

    private OCGMemoryStore storeWith(OCGMemoryStore.Transport transport) {
        return OCGMemoryStore.builder()
                .url("https://ocg.test")
                .agent("agent:a")
                .user("user:bob")
                .transport(transport)
                .build();
    }

    @Test
    void add_posts_value_field_and_no_confidence() {
        AtomicReference<Captured> captured = new AtomicReference<>();
        OCGMemoryStore store = storeWith((method, url, headers, body) -> {
            Captured c = new Captured();
            c.method = method;
            c.url = url;
            c.body = body != null ? JsonMapper.get().valueToTree(JsonMapper.fromJson(body, Map.class)) : null;
            captured.set(c);
            return new OCGMemoryStore.Transport.Response(200, "{\"key\":\"k1\"}");
        });

        String key = store.add(new MemoryEntry("alice prefers email", Map.of("key", "pref")));

        assertEquals("pref", key);
        Captured c = captured.get();
        assertTrue(c.url.endsWith("/api/v1/memories"), "url was: " + c.url);
        // field is "value", NOT "string_value"; confidence was removed from the API.
        assertEquals("alice prefers email", c.body.get("value").asText());
        assertFalse(c.body.has("string_value"), "body must not carry string_value");
        assertFalse(c.body.has("confidence"), "body must not carry confidence");
        assertEquals("agent:a", c.body.get("agent").asText());
        assertEquals("user:bob", c.body.get("user").asText());
    }

    @Test
    void search_folds_good_bad_signal_into_content() {
        OCGMemoryStore store = storeWith((method, url, headers, body) -> {
            assertTrue(url.endsWith("/api/v1/memories/search"), "url was: " + url);
            String json = "{\"memories\":[{"
                    + "\"key\":\"m1\","
                    + "\"value_preview\":\"use us-east-1\","
                    + "\"good_count\":2,\"bad_count\":1,\"relevance_score\":0.9,"
                    + "\"feedback_notes\":[{\"verdict\":\"bad\",\"reason\":\"stale region\"}]"
                    + "}]}";
            return new OCGMemoryStore.Transport.Response(200, json);
        });

        List<MemoryEntry> entries = store.search("which region", 5);
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).getContent().contains("[good 2 / bad 1]"), entries.get(0).getContent());
        assertTrue(entries.get(0).getContent().contains("bad: \"stale region\""), entries.get(0).getContent());
    }

    @Test
    void feedback_links_hits_mint_route() {
        OCGMemoryStore store = storeWith((method, url, headers, body) -> {
            assertTrue(
                    url.split("\\?")[0].endsWith("/api/v1/memories/k1/feedback-links"), "url was: " + url);
            String json = "{\"good_url\":\"https://ocg.test/api/v1/feedback/GOOD\","
                    + "\"bad_url\":\"https://ocg.test/api/v1/feedback/BAD\","
                    + "\"expires_at\":\"2026-09-01T00:00:00Z\"}";
            return new OCGMemoryStore.Transport.Response(200, json);
        });

        Map<String, Object> links = store.feedbackLinks("k1");
        assertTrue(String.valueOf(links.get("good_url")).endsWith("/feedback/GOOD"));
        assertTrue(String.valueOf(links.get("bad_url")).endsWith("/feedback/BAD"));
    }

    @Test
    void non_2xx_raises() {
        OCGMemoryStore store = storeWith((method, url, headers, body) -> new OCGMemoryStore.Transport.Response(500, "boom"));
        assertThrows(
                AgentAPIException.class, () -> store.add(new MemoryEntry("x", Map.of("key", "k"))));
    }

    @Test
    void delete_swallows_error_and_returns_false() {
        OCGMemoryStore store = storeWith((method, url, headers, body) -> new OCGMemoryStore.Transport.Response(404, "not found"));
        assertFalse(store.delete("missing"), "delete must return false on a non-2xx response");
    }

    @Test
    void validates_required_url_and_agent() {
        assertThrows(IllegalArgumentException.class, () -> OCGMemoryStore.builder()
                .agent("agent:a")
                .build());
        assertThrows(IllegalArgumentException.class, () -> OCGMemoryStore.builder()
                .url("https://ocg.test")
                .build());
    }

    @Test
    void build_memory_summarizer_is_recursion_safe() {
        Agent summarizer = OCGMemoryStore.buildMemorySummarizer("openai/gpt-4o");
        assertEquals("__memory_summarizer", summarizer.getName());
        assertEquals("openai/gpt-4o", summarizer.getModel());
        assertEquals(MemorySummary.class, summarizer.getOutputType());
        assertEquals(1, summarizer.getMaxTurns());
        // Created WITHOUT semantic_memory so the post-run save hook skips it (no recursion).
        assertNull(summarizer.getSemanticMemory());
    }

    @Test
    void agent_stores_memory_attrs() {
        SemanticMemory sm = new SemanticMemory(new OCGMemoryStore.Builder()
                        .url("https://ocg.test")
                        .agent("agent:a")
                        .build(),
                5,
                null);
        Agent agent = Agent.builder()
                .name("a")
                .model("openai/gpt-4o")
                .semanticMemory(sm)
                .build();
        assertEquals(sm, agent.getSemanticMemory());
        assertNull(agent.getMemorySummaryModel(), "defaults to null -> reuse agent model");
        assertNull(agent.getFeedbackSink());
    }
}
