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
package io.orkes.conductor.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.orkes.conductor.client.exceptions.SSEUnavailableException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SSE transport contract against a stub server: rejected initial connects
 * surface as {@link SSEUnavailableException} (never a silently-empty stream),
 * {@code id:} frames are tracked, and mid-stream drops reconnect with a
 * {@code Last-Event-ID} header so the server can resume delivery.
 */
@Timeout(30)
public class SseClientTest {

    private MockWebServer server;
    private ApiClient apiClient;

    @BeforeEach
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        apiClient = ApiClient.builder()
                .basePath(server.url("/api").toString())
                .build();
    }

    @AfterEach
    public void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse sse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(body);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "expected a request to reach the stub server");
        return request;
    }

    @Test
    public void connectThrowsSSEUnavailableWhenServerRejectsStream() {
        server.enqueue(new MockResponse().setResponseCode(503));

        SseClient client = new SseClient(apiClient, "e1");
        assertThrows(SSEUnavailableException.class, client::connect);
    }

    @Test
    public void streamsEventsAndTracksLastEventId() throws InterruptedException {
        server.enqueue(sse(""
                + "id: 7\n"
                + "data: {\"type\":\"message\",\"content\":\"hello\"}\n"
                + "\n"
                + "data: [DONE]\n"
                + "\n"));

        try (SseClient client = new SseClient(apiClient, "e1")) {
            client.connect();

            Map<String, Object> event = client.nextEvent();
            assertNotNull(event);
            assertEquals("message", event.get("type"));
            assertEquals("hello", event.get("content"));
            assertEquals("7", client.getLastEventId());
            assertNull(client.nextEvent(), "stream should end after [DONE]");
        }

        RecordedRequest request = takeRequest();
        assertEquals("/api/agent/stream/e1", request.getPath());
        assertNull(request.getHeader("Last-Event-ID"), "fresh connect must not send Last-Event-ID");
    }

    @Test
    public void initialConnectSendsProvidedLastEventId() throws InterruptedException {
        server.enqueue(sse("data: [DONE]\n\n"));

        try (SseClient client = new SseClient(apiClient, "e1", "41")) {
            client.connect();
            assertNull(client.nextEvent());
        }

        RecordedRequest request = takeRequest();
        assertEquals("41", request.getHeader("Last-Event-ID"));
    }

    @Test
    public void midStreamDropReconnectsWithLastEventId() throws InterruptedException {
        // First response: a complete event (with id) in the first half of the
        // body, then padding — the socket drops halfway through, after the
        // event was delivered but before the stream ends cleanly.
        StringBuilder body = new StringBuilder()
                .append("id: 42\n")
                .append("data: {\"type\":\"message\",\"content\":\"first\"}\n")
                .append("\n");
        for (int i = 0; i < 100; i++) {
            body.append(": padding to place the event in the delivered half\n");
        }
        server.enqueue(sse(body.toString()).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        server.enqueue(sse(""
                + "id: 43\n"
                + "data: {\"type\":\"done\"}\n"
                + "\n"));

        try (SseClient client = new SseClient(apiClient, "e1")) {
            client.connect();

            Map<String, Object> first = client.nextEvent();
            assertNotNull(first);
            assertEquals("first", first.get("content"));

            Map<String, Object> second = client.nextEvent();
            assertNotNull(second, "reconnect should deliver the resumed event");
            assertEquals("done", second.get("type"));
            assertNull(client.nextEvent(), "type=done ends the stream");
            assertEquals("43", client.getLastEventId());
        }

        RecordedRequest initial = takeRequest();
        assertNull(initial.getHeader("Last-Event-ID"));
        RecordedRequest reconnect = takeRequest();
        assertEquals("/api/agent/stream/e1", reconnect.getPath());
        assertEquals("42", reconnect.getHeader("Last-Event-ID"),
                "reconnect must resume from the last delivered event id");
    }
}
