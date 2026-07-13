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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.exceptions.AgentAPIException;
import org.conductoross.conductor.ai.internal.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Back agentspan {@link SemanticMemory} with an OCG (Open Context Graph) instance.
 *
 * <p>A synchronous HTTP adapter implementing the {@link MemoryStore} interface over
 * the OCG BFF, so an agent's memories persist in OCG and ride OCG's feedback-aware
 * ranking:
 *
 * <ul>
 *   <li>{@code add}      &rarr; {@code POST   /api/v1/memories}</li>
 *   <li>{@code search}   &rarr; {@code POST   /api/v1/memories/search} (feedback-blended ranking)</li>
 *   <li>{@code delete}   &rarr; {@code DELETE /api/v1/memories/{key}}</li>
 *   <li>{@code listAll}  &rarr; {@code GET    /api/v1/memories}</li>
 *   <li>{@link #feedbackLinks} &rarr; {@code POST /api/v1/memories/{key}/feedback-links} (mints signed URLs)</li>
 * </ul>
 *
 * <p>Design notes (mirrors the Python {@code OCGMemoryStore} in {@code ocg_memory.py}):
 *
 * <ul>
 *   <li>The OCG bearer {@code token} is held <b>client-side</b> here (e.g. from
 *       {@code OCG_TOKEN}), unlike the server-side retrieval tools which resolve a
 *       credential server-side.</li>
 *   <li>Agents only ever <b>create and read</b> memories. Good/bad feedback is
 *       human-only: it is delivered out-of-band through the agent's {@code feedbackSink}
 *       (e.g. into a Zendesk ticket) and the capability URLs are never surfaced to the
 *       agent's LLM.</li>
 *   <li>{@link #getCredential()} is a server-resolvable secret NAME (default
 *       {@code "OCG_PUBLIC_KEY"}) used by the COMPILED/deployed path — distinct from
 *       {@code token}, the raw client token. The
 *       {@link org.conductoross.conductor.ai.internal.AgentConfigSerializer} emits it as
 *       {@code longTermMemory.credential} so the server can resolve the bearer token via a
 *       {@code #{NAME}} HTTP-header placeholder.</li>
 * </ul>
 */
public class OCGMemoryStore implements MemoryStore {

    private static final Logger logger = LoggerFactory.getLogger(OCGMemoryStore.class);

    private final String baseUrl;
    private final String agent;
    private final String user;
    private final String credential;
    private final String scope;
    private final Map<String, String> headers;
    private final Transport transport;

    private OCGMemoryStore(Builder b) {
        if (b.url == null || b.url.trim().isEmpty()) {
            throw new IllegalArgumentException("OCGMemoryStore requires a non-blank OCG instance url");
        }
        if (b.agent == null || b.agent.trim().isEmpty()) {
            throw new IllegalArgumentException("OCGMemoryStore requires a non-blank agent owner");
        }
        this.baseUrl = stripTrailingSlashes(b.url.trim());
        this.agent = b.agent;
        this.user = b.user;
        this.credential = b.credential != null ? b.credential : "OCG_PUBLIC_KEY";
        this.scope = b.scope != null ? b.scope : "user";
        this.headers = new LinkedHashMap<>();
        if (b.token != null && !b.token.isEmpty()) {
            this.headers.put("Authorization", "Bearer " + b.token);
        }
        this.transport = b.transport != null ? b.transport : new JdkTransport(Duration.ofSeconds(b.timeoutSeconds));
    }

    public static Builder builder() {
        return new Builder();
    }

    // ── Accessors (read by the config serializer for the compiled path) ─────

    /** OCG instance base url, trailing slashes stripped. */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** Agent owner key, e.g. {@code "agent:support"}. */
    public String getAgent() {
        return agent;
    }

    /** Optional user owner, e.g. {@code "user:alice"}, or {@code null}. */
    public String getUser() {
        return user;
    }

    /** Server-resolvable credential NAME for the OCG bearer token (never the raw token). */
    public String getCredential() {
        return credential;
    }

    /** Memory scope for writes (default {@code "user"}). */
    public String getScope() {
        return scope;
    }

    // ── MemoryStore interface ───────────────────────────────────────────────

    @Override
    public String add(MemoryEntry entry) {
        String key = entry.getId();
        if (key == null || key.isEmpty()) {
            Object mk = entry.getMetadata().get("key");
            key = mk != null ? String.valueOf(mk) : "";
        }
        if (key.isEmpty()) {
            key = hashKey(entry.getContent());
        }

        String content = entry.getContent() != null ? entry.getContent() : "";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", key);
        body.put("agent", agent);
        body.put("value", content);
        body.put("description", content.substring(0, Math.min(200, content.length())));
        body.put("scope", scope);
        body.put("source", "agent_inferred");
        body.put("tags", asStringList(entry.getMetadata().get("tags")));
        if (user != null) {
            body.put("user", user);
        }

        request("POST", "/api/v1/memories", body, null);
        entry.setId(key);
        return key;
    }

    @Override
    public List<MemoryEntry> search(String query, int topK) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("agent", agent);
        body.put("limit", topK);
        body.put("include_shared", true);
        if (user != null) {
            body.put("user", user);
        }

        JsonNode resp = request("POST", "/api/v1/memories/search", body, null);
        List<MemoryEntry> out = new ArrayList<>();
        for (JsonNode m : memories(resp)) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("relevance_score", m.has("relevance_score") ? m.get("relevance_score").asDouble() : null);
            metadata.put("good_count", intOf(m, "good_count"));
            metadata.put("bad_count", intOf(m, "bad_count"));
            MemoryEntry entry = new MemoryEntry(withSignal(text(m, "value_preview"), m), metadata);
            entry.setId(text(m, "key"));
            out.add(entry);
        }
        return out;
    }

    @Override
    public boolean delete(String memoryId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("agent", agent);
        if (user != null) {
            params.put("user", user);
        }
        try {
            request("DELETE", "/api/v1/memories/" + memoryId, null, params);
        } catch (AgentAPIException e) {
            return false;
        }
        return true;
    }

    @Override
    public void clear() {
        // No bulk-clear endpoint — fan out over the listed keys. Guard usage:
        // this deletes every memory for the configured agent/user.
        List<MemoryEntry> entries = listAll();
        logger.warn("OCGMemoryStore.clear() deleting {} memories for {}", entries.size(), agent);
        for (MemoryEntry e : entries) {
            delete(e.getId());
        }
    }

    @Override
    public List<MemoryEntry> listAll() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("agent", agent);
        params.put("limit", "200");
        if (user != null) {
            params.put("user", user);
        }
        JsonNode resp = request("GET", "/api/v1/memories", null, params);
        List<MemoryEntry> out = new ArrayList<>();
        for (JsonNode m : memories(resp)) {
            MemoryEntry entry = new MemoryEntry(text(m, "value_preview"));
            entry.setId(text(m, "key"));
            out.add(entry);
        }
        return out;
    }

    // ── Capability feedback links (human-only, out-of-band) ─────────────────

    /**
     * Mint signed good/bad capability URLs for a memory.
     *
     * <p>Returns {@code {"good_url", "bad_url", "expires_at"}}. The URLs require no OCG
     * login — a human (e.g. a support engineer) clicks them to vote. Requires the OCG
     * instance to have a feedback-link secret configured (else OCG returns 501).
     *
     * @param key the memory key to mint links for
     * @return a map of the minted link fields
     */
    public Map<String, Object> feedbackLinks(String key) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("agent", agent);
        if (user != null) {
            params.put("user", user);
        }
        JsonNode resp = request("POST", "/api/v1/memories/" + key + "/feedback-links", null, params);
        Map<String, Object> out = new LinkedHashMap<>();
        if (resp != null && resp.isObject()) {
            resp.fields().forEachRemaining(e -> out.put(e.getKey(), toPlain(e.getValue())));
        }
        return out;
    }

    // ── HTTP plumbing ───────────────────────────────────────────────────────

    private JsonNode request(String method, String path, Object jsonBody, Map<String, String> queryParams) {
        String url = baseUrl + path + queryString(queryParams);
        String body = jsonBody != null ? JsonMapper.toJson(jsonBody) : null;
        Transport.Response resp = transport.send(method, url, headers, body);
        if (resp.status() >= 400) {
            throw new AgentAPIException(resp.status(), resp.body() != null ? resp.body() : "");
        }
        String respBody = resp.body();
        if (respBody == null || respBody.isEmpty()) {
            return null;
        }
        try {
            return JsonMapper.get().readTree(respBody);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pluggable HTTP transport. The default ({@link JdkTransport}) uses the JDK
     * {@link HttpClient}; tests inject a stub to capture requests and serve canned
     * responses (parity with the Python tests' {@code httpx.MockTransport}).
     */
    @FunctionalInterface
    public interface Transport {
        /**
         * Perform an HTTP exchange.
         *
         * @param method  the HTTP method (GET/POST/DELETE)
         * @param url     the fully-qualified request URL (base + path + query string)
         * @param headers request headers (e.g. Authorization)
         * @param body    the JSON request body, or {@code null} for no body
         * @return the response status and body
         */
        Response send(String method, String url, Map<String, String> headers, String body);

        /** A minimal HTTP response: status code and raw body text. */
        final class Response {
            private final int status;
            private final String body;

            public Response(int status, String body) {
                this.status = status;
                this.body = body;
            }

            public int status() {
                return status;
            }

            public String body() {
                return body;
            }
        }
    }

    private static final class JdkTransport implements Transport {
        private final HttpClient client;
        private final Duration timeout;

        JdkTransport(Duration timeout) {
            this.timeout = timeout;
            this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        }

        @Override
        public Response send(String method, String url, Map<String, String> headers, String body) {
            try {
                HttpRequest.Builder b =
                        HttpRequest.newBuilder().uri(URI.create(url)).timeout(timeout);
                if (headers != null) {
                    headers.forEach(b::header);
                }
                if (body != null) {
                    b.header("Content-Type", "application/json");
                    b.method(method, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    b.method(method, HttpRequest.BodyPublishers.noBody());
                }
                HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
                return new Response(r.statusCode(), r.body());
            } catch (Exception e) {
                // network/timeout — surface as a status-0 API error (matches Python).
                throw new AgentAPIException(0, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
    }

    // ── Conversation summarization (Claude-style distillation) ──────────────

    /** Instructions for the internal conversation summarizer sub-agent. */
    public static final String MEMORY_SUMMARIZER_INSTRUCTIONS =
            "You distill a conversation into a durable memory. Read the transcript and "
                    + "extract only reusable, durable facts about the user, their preferences, and "
                    + "the task — the kind of thing worth remembering for next time. Ignore greetings, "
                    + "filler, and one-off details. Write a one-paragraph summary, a short list of "
                    + "facts, and a few topical tags. Be concise and concrete.";

    /**
     * Build the internal agent that summarizes a conversation into a memory.
     *
     * <p>It uses {@link MemorySummary} structured output and is intentionally created
     * <b>without</b> {@code semanticMemory} so the post-run save hook skips it (no
     * recursion). Mirrors the Python {@code build_memory_summarizer}.
     *
     * @param model the model id to run the summarizer with (reuses the agent's model)
     * @return the summarizer {@link Agent}
     */
    public static Agent buildMemorySummarizer(String model) {
        return buildMemorySummarizer(model, "__memory_summarizer");
    }

    /** Variant of {@link #buildMemorySummarizer(String)} with an explicit agent name. */
    public static Agent buildMemorySummarizer(String model, String name) {
        return Agent.builder()
                .name(name)
                .model(model)
                .instructions(MEMORY_SUMMARIZER_INSTRUCTIONS)
                .outputType(MemorySummary.class)
                .maxTurns(1)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Fold the human good/bad signal into a search result's content so the injected
     * prompt context shows the agent when a memory was marked bad and why.
     */
    static String withSignal(String content, JsonNode m) {
        int good = intOf(m, "good_count");
        int bad = intOf(m, "bad_count");
        if (good == 0 && bad == 0) {
            return content;
        }
        StringBuilder sb = new StringBuilder(content);
        sb.append("  [good ").append(good).append(" / bad ").append(bad).append("]");
        JsonNode notes = m.get("feedback_notes");
        if (notes != null && notes.isArray()) {
            for (JsonNode note : notes) {
                String verdict = text(note, "verdict");
                String reason = text(note, "reason");
                if ("bad".equals(verdict) && reason != null && !reason.isEmpty()) {
                    sb.append(" (bad: \"").append(reason).append("\")");
                }
            }
        }
        return sb.toString();
    }

    private static Iterable<JsonNode> memories(JsonNode resp) {
        if (resp != null) {
            JsonNode mems = resp.get("memories");
            if (mems != null && mems.isArray()) {
                return mems;
            }
        }
        return new ArrayList<>();
    }

    private static int intOf(JsonNode node, String field) {
        JsonNode v = node != null ? node.get(field) : null;
        return v != null && v.isNumber() ? v.asInt(0) : (v != null && v.isTextual() ? parseInt(v.asText()) : 0);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node != null ? node.get(field) : null;
        return v != null && !v.isNull() ? v.asText("") : "";
    }

    private static Object toPlain(JsonNode v) {
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        if (v.isBoolean()) return v.asBoolean();
        if (v.isNumber()) return v.numberValue();
        return v.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object o : (Collection<Object>) value) {
                if (o != null) out.add(String.valueOf(o));
            }
        }
        return out;
    }

    private static String queryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }

    private static String hashKey(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("mem-");
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "mem-" + Integer.toHexString((content != null ? content : "").hashCode());
        }
    }

    /** Fluent builder for {@link OCGMemoryStore}. */
    public static class Builder {
        private String url;
        private String agent;
        private String user;
        private String token;
        private String credential = "OCG_PUBLIC_KEY";
        private String scope = "user";
        private long timeoutSeconds = 10L;
        private Transport transport;

        /** Base URL of the OCG instance (required). */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /** Agent owner key, e.g. {@code "agent:support"} (required). */
        public Builder agent(String agent) {
            this.agent = agent;
            return this;
        }

        /** Optional user owner, e.g. {@code "user:alice"}. */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /** OCG bearer token, held client-side (e.g. from {@code OCG_TOKEN}). */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Server-resolvable credential NAME (default {@code "OCG_PUBLIC_KEY"}) for the
         * OCG bearer token. Used by the COMPILED/deployed path.
         */
        public Builder credential(String credential) {
            this.credential = credential;
            return this;
        }

        /** Memory scope for writes (default {@code "user"}). */
        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        /** Per-request timeout in seconds (default 10). */
        public Builder timeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        /** Inject a custom HTTP {@link Transport} (mainly for tests). */
        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public OCGMemoryStore build() {
            return new OCGMemoryStore(this);
        }
    }
}
