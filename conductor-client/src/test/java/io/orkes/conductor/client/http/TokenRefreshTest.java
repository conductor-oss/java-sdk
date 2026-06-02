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
package io.orkes.conductor.client.http;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.ApiClient;

import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulates a token expiring mid-use and verifies the client reactively refreshes
 * the token on a 401 and retries the request. These tests fail if the
 * {@link TokenRefreshAuthenticator} wiring is removed: without it the first 401
 * propagates as a {@link ConductorClientException} instead of being retried.
 */
public class TokenRefreshTest {

    private static final String AUTH_HEADER = "X-Authorization";
    private static final TypeReference<String> STRING_TYPE = new TypeReference<>() {
    };

    private MockWebServer server;

    @AfterEach
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    private ApiClient buildClient() {
        return ApiClient.builder()
                .basePath(server.url("/api").toString())
                .credentials("test-key", "test-secret")
                .build();
    }

    private ConductorClientResponse<String> callBusinessEndpoint(ApiClient client) {
        return client.execute(
                ConductorClientRequest.builder()
                        .method(Method.GET)
                        .path("/ping")
                        .build(),
                STRING_TYPE);
    }

    /**
     * Dispatcher that mints incrementing tokens on /token, and treats the very first
     * minted token as "expired" on the business endpoint (returns 401). Any other
     * token is accepted (200).
     */
    private static class ExpiringTokenDispatcher extends Dispatcher {
        final AtomicInteger tokenMints = new AtomicInteger(0);
        final AtomicReference<String> firstToken = new AtomicReference<>();
        final AtomicReference<String> lastAcceptedToken = new AtomicReference<>();
        final boolean alwaysExpire;

        ExpiringTokenDispatcher(boolean alwaysExpire) {
            this.alwaysExpire = alwaysExpire;
        }

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            String path = request.getPath() == null ? "" : request.getPath();
            if (path.endsWith("/token") && "POST".equals(request.getMethod())) {
                String token = "token-" + tokenMints.incrementAndGet();
                firstToken.compareAndSet(null, token);
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"token\":\"" + token + "\"}");
            }

            String presented = request.getHeader(AUTH_HEADER);
            boolean expired = alwaysExpire
                    || (firstToken.get() != null && firstToken.get().equals(presented));
            if (expired) {
                return new MockResponse()
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"message\":\"EXPIRED_TOKEN\"}");
            }

            lastAcceptedToken.set(presented);
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/plain")
                    .setBody("ok");
        }
    }

    @Test
    public void refreshesAndContinuesAfterExpiry() throws IOException {
        server = new MockWebServer();
        ExpiringTokenDispatcher dispatcher = new ExpiringTokenDispatcher(false);
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        ConductorClientResponse<String> response = callBusinessEndpoint(client);

        assertEquals(200, response.getStatusCode());
        assertEquals("ok", response.getData());
        // One mint at init (token-1) plus one reactive refresh after the 401 (token-2).
        assertEquals(2, dispatcher.tokenMints.get());
        // The successful (retried) request must have carried the refreshed token.
        assertEquals("token-2", dispatcher.lastAcceptedToken.get());
    }

    @Test
    public void continuesUsingCachedTokenAfterRefresh() throws IOException {
        server = new MockWebServer();
        ExpiringTokenDispatcher dispatcher = new ExpiringTokenDispatcher(false);
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        // First call triggers the refresh (token-1 -> 401 -> token-2 -> 200).
        ConductorClientResponse<String> first = callBusinessEndpoint(client);
        assertEquals(200, first.getStatusCode());
        assertEquals(2, dispatcher.tokenMints.get());

        // Second call should reuse the cached token-2 with no additional mint.
        ConductorClientResponse<String> second = callBusinessEndpoint(client);
        assertEquals(200, second.getStatusCode());
        assertEquals("ok", second.getData());
        assertEquals(2, dispatcher.tokenMints.get(), "no extra token mint expected for cached token");
        assertEquals("token-2", dispatcher.lastAcceptedToken.get());
    }

    @Test
    public void loopProtectionGivesUpAfterOneRetry() throws IOException {
        server = new MockWebServer();
        ExpiringTokenDispatcher dispatcher = new ExpiringTokenDispatcher(true);
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        assertThrows(ConductorClientException.class, () -> callBusinessEndpoint(client));
        // Init mint (token-1) plus exactly one reactive refresh attempt (token-2); then it gives up.
        assertEquals(2, dispatcher.tokenMints.get(),
                "authenticator must retry at most once and not loop");
        assertTrue(dispatcher.tokenMints.get() <= 2);
    }
}
