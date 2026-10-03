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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import okhttp3.Dns;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
