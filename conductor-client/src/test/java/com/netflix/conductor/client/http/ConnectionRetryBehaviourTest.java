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
package com.netflix.conductor.client.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.concurrent.TaskRunner;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.Http2Connection;
import okhttp3.internal.http2.Http2Stream;
import okhttp3.internal.http2.StreamResetException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.BufferedSink;
import okio.Okio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code localhost} resolves to both an IPv4 and an IPv6 route on this machine, but MockWebServer
 * only listens on one of them. A request that is fully delivered and then has its socket severed
 * is, with {@code retransmitRequestBodies(true)}, retried on the other (unlistened) route and
 * reported as a connect failure - even though the server already received it. With the SDK
 * default (one-shot bodies), the caller instead sees the raw failure from the single,
 * already-delivered attempt.
 */
class ConnectionRetryBehaviourTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        Thread.interrupted();
    }

    @Test
    @DisplayName("retransmitRequestBodies(true) delivers the request but reports it as a connect failure")
    void retransmitEnabled_requestIsDeliveredThenRetried_reportsConnectFailure() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

        var client = new ConductorClient(
                ConductorClient.builder()
                        .basePath(server.url("/api").toString())
                        .retransmitRequestBodies(true));

        var request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.POST)
                .path("/workflow")
                .body("{\"name\":\"test\"}")
                .build();

        var e = assertThrows(ConductorClientException.class, () -> client.execute(request));

        assertEquals(1, server.getRequestCount(), "request must have been delivered to the server");
        assertTrue(
                e.getCause() instanceof ConnectException,
                "expected a ConnectException from the retried (unreachable) route, got: " + e.getCause());
        assertTrue(
                e.getCause().getMessage().contains("Failed to connect"),
                "message should read like a connect failure, was: " + e.getCause().getMessage());
    }

    @Test
    @DisplayName("default one-shot bodies report the raw single-attempt failure, not a connect failure")
    void oneShotByDefault_requestIsDeliveredOnce_reportsRawFailure() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

        var client = new ConductorClient(
                ConductorClient.builder().basePath(server.url("/api").toString()));

        var request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.POST)
                .path("/workflow")
                .body("{\"name\":\"test\"}")
                .build();

        var e = assertThrows(ConductorClientException.class, () -> client.execute(request));

        assertEquals(1, server.getRequestCount(), "request must have been delivered to the server");
        assertTrue(
                !(e.getCause() instanceof ConnectException),
                "must not be the connect-failure seen under retransmission, got: " + e.getCause());
        assertTrue(
                e.getCause() instanceof IOException
                        && e.getCause().getMessage().contains("unexpected end of stream"),
                "expected the raw single-attempt stream failure, got: " + e.getCause());
    }

    @Test
    @DisplayName("a pre-send connection failure still falls back to another route")
    void preSendConnectFailure_fallsBackToAnotherRoute() throws Exception {
        // Fake Dns: dead loopback route first, the real MockWebServer route second.
        server.enqueue(new MockResponse().setBody("{}"));

        Dns twoRouteDns = hostname -> List.of(
                InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.1"));

        var client = new ConductorClient(
                ConductorClient.builder()
                        .basePath("http://multi-route.invalid:" + server.getPort() + "/api")
                        .connectTimeout(1000)
                        .configureOkHttp(b -> b.dns(twoRouteDns)));

        var request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.GET)
                .path("/workflow")
                .build();

        assertDoesNotThrow(() -> client.execute(request),
                "the dead first route must not fail the call: OkHttp should fall back to the live second route");
        assertEquals(1, server.getRequestCount(), "request must have reached the server via the fallback route");
    }

    // Same-route retransmission via a genuine mid-exchange HTTP/2 REFUSED_STREAM reset, using a
    // hand-rolled Http2Connection peer since MockWebServer can't reset a stream after reading it.

    @Test
    @DisplayName("non-one-shot body: a request already read by the server is retransmitted on "
            + "the same connection and the caller sees the second attempt's failure")
    void refusedStreamAfterFullRead_bodyNotOneShot_retransmitsAndReportsSecondAttempt() throws Exception {
        try (var h2Server = new ScriptedH2Server()) {
            var client = new OkHttpClient.Builder()
                    .protocols(List.of(Protocol.H2_PRIOR_KNOWLEDGE))
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build();

            var request = new Request.Builder()
                    .url("http://127.0.0.1:" + h2Server.port() + "/api/workflow")
                    .post(jsonBody())
                    .build();

            var e = assertThrows(IOException.class, () -> client.newCall(request).execute());

            assertEquals(
                    List.of(15L, 15L),
                    h2Server.receivedBodySizes(),
                    "the 15-byte body must have been fully read by the server on both the first "
                            + "and the retried attempt - proof of retransmission");
            assertEquals(1, h2Server.connectionsAccepted(),
                    "the retry must reuse the same TCP connection - same route, not a fallback");

            // Empirically a severed connection surfaces client-side as a local CANCEL reset, not a plain IOException.
            assertInstanceOf(StreamResetException.class, e, "got: " + e);
            assertEquals(
                    ErrorCode.CANCEL,
                    ((StreamResetException) e).errorCode,
                    "final error must describe the second attempt's local teardown, not the "
                            + "first attempt's REFUSED_STREAM, got: " + e);
        }
    }

    @Test
    @DisplayName("one-shot body (the SDK default): the request is delivered once and is not "
            + "retransmitted after the same REFUSED_STREAM reset")
    void refusedStreamAfterFullRead_oneShotBody_deliversOnceWithoutRetransmission() throws Exception {
        try (var h2Server = new ScriptedH2Server()) {
            var client = new OkHttpClient.Builder()
                    .protocols(List.of(Protocol.H2_PRIOR_KNOWLEDGE))
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build();

            var request = new Request.Builder()
                    .url("http://127.0.0.1:" + h2Server.port() + "/api/workflow")
                    .post(oneShot(jsonBody()))
                    .build();

            var e = assertThrows(IOException.class, () -> client.newCall(request).execute());

            assertEquals(
                    List.of(15L),
                    h2Server.receivedBodySizes(),
                    "the body must have been read by the server exactly once - no retransmission");
            assertEquals(1, h2Server.connectionsAccepted(), "no second connection must be opened");

            assertInstanceOf(StreamResetException.class, e, "got: " + e);
            assertEquals(ErrorCode.REFUSED_STREAM, ((StreamResetException) e).errorCode, "got: " + e);
        }
    }

    @Test
    @DisplayName("empirically: SocketPolicy.RESET_STREAM_AT_START resets before reading, so it "
            + "cannot prove the server received the request")
    void resetStreamAtStart_doesNotProveTheRequestWasRead() throws Exception {
        server.shutdown();
        server = new MockWebServer();
        server.setProtocols(List.of(Protocol.H2_PRIOR_KNOWLEDGE));
        server.start(InetAddress.getByName("127.0.0.1"), 0);
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.RESET_STREAM_AT_START));

        var client = new OkHttpClient.Builder()
                .protocols(List.of(Protocol.H2_PRIOR_KNOWLEDGE))
                .callTimeout(5, TimeUnit.SECONDS)
                .build();

        var request = new Request.Builder()
                .url("http://127.0.0.1:" + server.getPort() + "/api/workflow")
                .post(jsonBody())
                .build();

        assertThrows(IOException.class, () -> client.newCall(request).execute());

        assertEquals(1, server.getRequestCount(),
                "getRequestCount() is incremented for bookkeeping even though nothing was read");

        var recorded = server.takeRequest();
        assertEquals("", recorded.getRequestLine(),
                "the recorded request is an empty placeholder - the real request line was never parsed");
        assertEquals(0L, recorded.getBodySize(),
                "the recorded request carries no body - it was never read off the wire");
    }

    private static RequestBody jsonBody() {
        return RequestBody.create("{\"name\":\"test\"}", MediaType.parse("application/json"));
    }

    private static RequestBody oneShot(RequestBody delegate) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return delegate.contentType();
            }

            @Override
            public long contentLength() throws IOException {
                return delegate.contentLength();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                delegate.writeTo(sink);
            }

            @Override
            public boolean isOneShot() {
                return true;
            }
        };
    }

    /** Tiny Http2Connection-based peer: refuses the first stream it reads, severs any further one. */
    private static final class ScriptedH2Server implements Closeable {

        private final ServerSocket serverSocket;
        private final AtomicInteger connectionsAccepted = new AtomicInteger();
        private final AtomicInteger streamsHandled = new AtomicInteger();
        private final List<Long> receivedBodySizes = new CopyOnWriteArrayList<>();
        private final Thread acceptThread;
        private volatile boolean stopped;

        ScriptedH2Server() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            acceptThread = new Thread(this::acceptLoop, "scripted-h2-server");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int connectionsAccepted() {
            return connectionsAccepted.get();
        }

        List<Long> receivedBodySizes() {
            return List.copyOf(receivedBodySizes);
        }

        private void acceptLoop() {
            while (!stopped) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    return;
                }
                connectionsAccepted.incrementAndGet();
                try {
                    handleConnection(socket);
                } catch (IOException ignored) {
                    // The client side of the test observes and asserts on the resulting failure.
                }
            }
        }

        private void handleConnection(Socket socket) throws IOException {
            var listener = new Http2Connection.Listener() {
                @Override
                public void onStream(Http2Stream stream) throws IOException {
                    stream.takeHeaders();
                    var body = Okio.buffer(stream.getSource());
                    receivedBodySizes.add((long) body.readByteString().size());
                    if (streamsHandled.incrementAndGet() == 1) {
                        stream.close(ErrorCode.REFUSED_STREAM, null);
                    } else {
                        socket.close();
                    }
                }
            };
            new Http2Connection.Builder(false, TaskRunner.INSTANCE)
                    .socket(socket)
                    .listener(listener)
                    .build()
                    .start();
        }

        @Override
        public void close() throws IOException {
            stopped = true;
            serverSocket.close();
        }
    }
}
