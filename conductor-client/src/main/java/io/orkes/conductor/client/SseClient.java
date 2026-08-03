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
package io.orkes.conductor.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.config.ObjectMapperProvider;

import io.orkes.conductor.client.exceptions.SSEUnavailableException;
import io.orkes.conductor.client.http.Pair;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Server-Sent Events (SSE) client for streaming agent events
 * ({@code GET /api/agent/stream/{executionId}}).
 *
 * <p>Streams through the shared native Conductor {@link ApiClient} — the request
 * is built with {@link ApiClient#buildCall} so it rides the SDK's OkHttp client
 * and token-refresh auth interceptor, exactly like every other client. The
 * response body is read incrementally; parsed events are placed into a
 * {@link LinkedBlockingQueue} and consumed via {@link #nextEvent()}.
 *
 * <p>{@link #connect()} performs the initial connection synchronously and throws
 * {@link SSEUnavailableException} if the server rejects streaming (non-2xx or
 * transport failure) — callers should degrade to status polling. On mid-stream
 * drops the client reconnects with bounded backoff, sending the
 * {@code Last-Event-ID} header so the server can resume from the last
 * delivered event.
 *
 * <p>Transport-only: events are surfaced as raw parsed JSON maps. Domain mapping
 * (e.g. the agent SDK's {@code AgentEvent}) is the caller's concern.
 */
public class SseClient implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SseClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProvider().getObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** Sentinel value to signal end-of-stream (compared by identity). */
    private static final Map<String, Object> DONE_SENTINEL = Collections.unmodifiableMap(new HashMap<>());

    static final int MAX_RECONNECT_ATTEMPTS = 5;
    static final long RECONNECT_DELAY_MS = 1_000;

    private final ApiClient apiClient;
    private final String executionId;
    private final BlockingQueue<Map<String, Object>> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Call call;
    private volatile String lastEventId;

    public SseClient(ApiClient apiClient, String executionId) {
        this(apiClient, executionId, null);
    }

    /**
     * @param lastEventId resume point sent as {@code Last-Event-ID} on the first
     *     connect ({@code null} to start from the beginning of the stream)
     */
    public SseClient(ApiClient apiClient, String executionId, String lastEventId) {
        this.apiClient = apiClient;
        this.executionId = executionId;
        this.lastEventId = lastEventId;
    }

    /**
     * Connect and start receiving SSE events in a background thread.
     *
     * @throws SSEUnavailableException if the server rejects the stream (non-2xx
     *     response or transport failure on the initial connect)
     */
    public void connect() {
        Response response;
        try {
            response = open();
        } catch (IOException e) {
            throw new SSEUnavailableException("SSE connection failed: " + e.getMessage(), e);
        }
        if (response == null) {
            throw new SSEUnavailableException("Server rejected SSE stream for execution " + executionId);
        }
        Thread streamThread = new Thread(() -> streamLoop(response), "agent-sse-" + executionId);
        streamThread.setDaemon(true);
        streamThread.start();
    }

    /**
     * Block until the next event is available and return it.
     *
     * @return the next event as a parsed JSON map, or null if the stream is done
     */
    public Map<String, Object> nextEvent() {
        try {
            Map<String, Object> event = eventQueue.take();
            return event == DONE_SENTINEL ? null : event;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** The {@code id:} of the last dispatched event ({@code null} if none yet). */
    public String getLastEventId() {
        return lastEventId;
    }

    @Override
    public void close() {
        closed.set(true);
        Call c = call;
        if (c != null) c.cancel();
        // Wake up any blocked nextEvent() calls
        eventQueue.offer(DONE_SENTINEL);
    }

    /**
     * Open the SSE connection, sending {@code Last-Event-ID} when resuming.
     *
     * @return the successful response, or {@code null} if the server answered
     *     non-2xx (an explicit rejection — do not retry)
     * @throws IOException on transport failure
     */
    private Response open() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/event-stream");
        headers.put("Cache-Control", "no-cache");
        String resumeFrom = lastEventId;
        if (resumeFrom != null && !resumeFrom.isEmpty()) {
            headers.put("Last-Event-ID", resumeFrom);
        }

        // Relative to the ApiClient's base path (the server's /api root); auth
        // and token refresh are applied by the client's OkHttp interceptor.
        Call c = apiClient.buildCall(
                "/agent/stream/" + executionId,
                "GET",
                Collections.<Pair>emptyList(),
                Collections.<Pair>emptyList(),
                null,
                headers);
        call = c;
        Response response = c.execute();
        if (!response.isSuccessful()) {
            int code = response.code();
            response.close();
            if (!closed.get()) {
                logger.warn("SSE connection rejected with status {} for execution {}", code, executionId);
            }
            return null;
        }
        return response;
    }

    private void streamLoop(Response initialResponse) {
        Response response = initialResponse;
        try {
            while (!closed.get() && response != null) {
                boolean dropped = false;
                try {
                    readEvents(response);
                } catch (IOException e) {
                    if (closed.get()) {
                        return;
                    }
                    dropped = true;
                    logger.warn("SSE connection lost ({}), reconnecting...", e.getMessage());
                } finally {
                    response.close();
                }
                if (!dropped) {
                    return; // clean end of stream
                }
                response = reconnect();
            }
        } catch (Exception e) {
            if (!closed.get()) {
                logger.error("SSE stream error: {}", e.getMessage(), e);
            }
        } finally {
            eventQueue.offer(DONE_SENTINEL);
        }
    }

    /**
     * Bounded reconnect after a mid-stream drop.
     *
     * @return the new response, or {@code null} to stop (rejected by the
     *     server, closed, or attempts exhausted)
     */
    private Response reconnect() {
        for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS && !closed.get(); attempt++) {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (closed.get()) {
                return null;
            }
            try {
                Response response = open();
                if (response == null) {
                    // Explicit server rejection — stop rather than hammer it.
                    return null;
                }
                return response;
            } catch (IOException e) {
                logger.warn("SSE reconnect attempt {}/{} failed: {}", attempt, MAX_RECONNECT_ATTEMPTS, e.getMessage());
            }
        }
        if (!closed.get()) {
            logger.warn("SSE stream for execution {} gave up after {} reconnect attempts",
                    executionId, MAX_RECONNECT_ATTEMPTS);
        }
        return null;
    }

    /** Read and dispatch events until the server ends the stream (clean close). */
    private void readEvents(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) return;

        StringBuilder dataBuffer = new StringBuilder();
        String eventType = null;
        String pendingId = null;

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
        String rawLine;
        while (!closed.get() && (rawLine = reader.readLine()) != null) {
            // Strip trailing \r if present
            String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;

            if (line.isEmpty()) {
                String data = dataBuffer.toString().trim();
                if (!data.isEmpty()) {
                    if (pendingId != null && !pendingId.isEmpty()) {
                        lastEventId = pendingId;
                    }
                    dispatchEvent(eventType, data);
                }
                dataBuffer.setLength(0);
                eventType = null;
                pendingId = null;
                continue;
            }
            if (line.startsWith(":")) {
                continue; // comment / heartbeat
            }
            if (line.startsWith("event:")) {
                eventType = line.substring(6).trim();
            } else if (line.startsWith("id:")) {
                pendingId = line.substring(3).trim();
            } else if (line.startsWith("data:")) {
                String dataChunk = line.substring(5);
                if (dataChunk.startsWith(" ")) dataChunk = dataChunk.substring(1);
                if (dataBuffer.length() > 0) dataBuffer.append("\n");
                dataBuffer.append(dataChunk);
            }
        }

        // Dispatch any remaining buffered data
        String data = dataBuffer.toString().trim();
        if (!data.isEmpty()) {
            if (pendingId != null && !pendingId.isEmpty()) {
                lastEventId = pendingId;
            }
            dispatchEvent(eventType, data);
        }
    }

    private void dispatchEvent(String eventType, String data) {
        try {
            if ("[DONE]".equals(data)) {
                eventQueue.offer(DONE_SENTINEL);
                return;
            }
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(data, MAP_TYPE);
            eventQueue.offer(parsed);
            if ("done".equals(parsed.get("type"))) {
                eventQueue.offer(DONE_SENTINEL);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse SSE event data: {} — {}", data, e.getMessage());
        }
    }
}
