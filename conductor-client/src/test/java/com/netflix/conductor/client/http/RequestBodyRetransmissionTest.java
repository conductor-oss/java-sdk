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
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import okhttp3.Dns;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A request body that is not one-shot can be sent twice: OkHttp retransmits it after a
 * recoverable connection failure, and the caller is then told about the second attempt rather
 * than the first. For a non-idempotent call that means the work happened and the error says it
 * did not.
 *
 * <p>The decisive pair below makes one successful call first, so the connection is pooled, then
 * severs the next one: a connection taken from the pool retries on its own address, which is the
 * real-world shape this SDK must handle. The remaining tests pin the one-shot contract directly
 * and check that a failure before the request is sent still falls back to another route - the
 * behaviour that must not be lost in exchange.
 */
class RequestBodyRetransmissionTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start(InetAddress.getByName("127.0.0.1"), 0);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("default (retransmitRequestBodies true): a body severed on a pooled connection "
            + "is retransmitted and the call succeeds")
    void retransmitEnabled_bodyIsRetransmittedOnPooledConnection_callSucceeds() {
        server.enqueue(new MockResponse().setBody("{}")); // warm-up: pools the connection
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        server.enqueue(new MockResponse().setBody("{}")); // served only if retransmitted

        var client = new ConductorClient(
                ConductorClient.builder().basePath(basePath()).retransmitRequestBodies(true));

        assertDoesNotThrow(() -> client.execute(warmupRequest()));

        assertDoesNotThrow(() -> client.execute(postRequest()),
                "retransmission must hide the severed attempt from the caller");
        assertEquals(3, server.getRequestCount(), "warm-up + severed attempt + retransmit");
    }

    @Test
    @DisplayName("opt-in (retransmitRequestBodies(false)): a body severed on a pooled connection "
            + "is not retransmitted and the call fails")
    void optOut_bodyIsNotRetransmittedOnPooledConnection_callFails() {
        server.enqueue(new MockResponse().setBody("{}")); // warm-up: pools the connection
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        server.enqueue(new MockResponse().setBody("{}")); // must never be reached

        var client = new ConductorClient(
                ConductorClient.builder().basePath(basePath()).retransmitRequestBodies(false));

        assertDoesNotThrow(() -> client.execute(warmupRequest()));

        var e = assertThrows(ConductorClientException.class, () -> client.execute(postRequest()));

        assertEquals(2, server.getRequestCount(), "warm-up + severed attempt, no retransmit");
        System.out.println("optOut_bodyIsNotRetransmittedOnPooledConnection_callFails threw: " + e);
    }

    @Test
    @DisplayName("contract pin: the built request body is one-shot only when opted out")
    void requestBody_isOneShot_onlyWhenRetransmitDisabled() {
        var defaultClient = new ConductorClient(ConductorClient.builder().basePath(basePath()));
        var optOutClient = new ConductorClient(
                ConductorClient.builder().basePath(basePath()).retransmitRequestBodies(false));

        assertFalse(builtRequestBody(defaultClient).isOneShot());
        assertTrue(builtRequestBody(optOutClient).isOneShot());
    }

    @Test
    @DisplayName("a pre-send connection failure still falls back to another route")
    void preSendConnectFailure_fallsBackToAnotherRoute() {
        server.enqueue(new MockResponse().setBody("{}"));

        Dns twoRouteDns = hostname -> List.of(
                InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.1"));

        var client = new ConductorClient(
                ConductorClient.builder()
                        .basePath("http://multi-route.invalid:" + server.getPort() + "/api")
                        .connectTimeout(1000)
                        .configureOkHttp(b -> b.dns(twoRouteDns)));

        assertDoesNotThrow(() -> client.execute(warmupRequest()),
                "the dead first route must not fail the call: OkHttp should fall back to the live second route");
        assertEquals(1, server.getRequestCount(), "request must have reached the server via the fallback route");
    }

    private String basePath() {
        return "http://127.0.0.1:" + server.getPort() + "/api";
    }

    private static ConductorClientRequest warmupRequest() {
        return ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.GET)
                .path("/workflow")
                .build();
    }

    private static ConductorClientRequest postRequest() {
        return ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.POST)
                .path("/workflow")
                .body("{\"name\":\"test\"}")
                .build();
    }

    private static RequestBody builtRequestBody(ConductorClient client) {
        return client.buildRequest("POST", "/workflow", List.of(), List.of(), Map.of(), "{\"name\":\"test\"}").body();
    }
}
