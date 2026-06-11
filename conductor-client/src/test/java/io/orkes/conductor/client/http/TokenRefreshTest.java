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
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.ApiClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulates a token expiring mid-use and verifies the client reactively refreshes
 * the token on a 401/403 and retries the request. These tests fail if the
 * {@link TokenRefreshInterceptor} wiring is removed: without it the first 401
 * propagates as a {@link ConductorClientException} instead of being retried.
 *
 * <p>The interceptor only refreshes when the JSON {@code error} field is
 * {@code EXPIRED_TOKEN} or {@code INVALID_TOKEN}; any other status, error code,
 * or unparseable body bubbles straight up.
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
     * minted token as "expired" on the business endpoint (rejecting it with the
     * configured status code and {@code error} value). Any other token is accepted (200).
     */
    private static class ExpiringTokenDispatcher extends Dispatcher {
        final AtomicInteger tokenMints = new AtomicInteger(0);
        final AtomicReference<String> firstToken = new AtomicReference<>();
        final AtomicReference<String> lastAcceptedToken = new AtomicReference<>();
        final boolean alwaysExpire;
        final int rejectCode;
        final String errorCode;

        ExpiringTokenDispatcher(boolean alwaysExpire) {
            this(alwaysExpire, 401, "EXPIRED_TOKEN");
        }

        ExpiringTokenDispatcher(boolean alwaysExpire, int rejectCode, String errorCode) {
            this.alwaysExpire = alwaysExpire;
            this.rejectCode = rejectCode;
            this.errorCode = errorCode;
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
                        .setResponseCode(rejectCode)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"" + errorCode + "\"}");
            }

            lastAcceptedToken.set(presented);
            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/plain")
                    .setBody("ok");
        }
    }

    /**
     * Dispatcher that mints tokens on /token but always rejects the business endpoint
     * with a fixed status code and body, regardless of the token presented.
     */
    private static class StaticRejectDispatcher extends Dispatcher {
        final AtomicInteger tokenMints = new AtomicInteger(0);
        final int rejectCode;
        final String body;
        final String contentType;

        StaticRejectDispatcher(int rejectCode, String body) {
            this(rejectCode, body, "application/json");
        }

        StaticRejectDispatcher(int rejectCode, String body, String contentType) {
            this.rejectCode = rejectCode;
            this.body = body;
            this.contentType = contentType;
        }

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            String path = request.getPath() == null ? "" : request.getPath();
            if (path.endsWith("/token") && "POST".equals(request.getMethod())) {
                String token = "token-" + tokenMints.incrementAndGet();
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"token\":\"" + token + "\"}");
            }

            return new MockResponse()
                    .setResponseCode(rejectCode)
                    .setHeader("Content-Type", contentType)
                    .setBody(body);
        }
    }

    /**
     * Dispatcher whose /token endpoint always fails, so every mint attempt throws.
     * Counts how many times /token was actually hit.
     */
    private static class FailingTokenDispatcher extends Dispatcher {
        final AtomicInteger tokenAttempts = new AtomicInteger(0);

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            String path = request.getPath() == null ? "" : request.getPath();
            if (path.endsWith("/token") && "POST".equals(request.getMethod())) {
                tokenAttempts.incrementAndGet();
                return new MockResponse()
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"error\":\"INVALID_TOKEN\"}");
            }

            return new MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"error\":\"EXPIRED_TOKEN\"}");
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
    public void refreshesAndContinuesAfter403() throws IOException {
        server = new MockWebServer();
        // Server rejects the stale token with 403 + INVALID_TOKEN instead of 401.
        ExpiringTokenDispatcher dispatcher = new ExpiringTokenDispatcher(false, 403, "INVALID_TOKEN");
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        ConductorClientResponse<String> response = callBusinessEndpoint(client);

        assertEquals(200, response.getStatusCode());
        assertEquals("ok", response.getData());
        assertEquals(2, dispatcher.tokenMints.get());
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
                "interceptor must retry at most once and not loop");
        assertTrue(dispatcher.tokenMints.get() <= 2);
    }

    @Test
    public void nonTokenErrorBubblesUpWithoutRefresh() throws IOException {
        server = new MockWebServer();
        // 401 whose error code is not a token-expiry/invalid code -> must not refresh.
        StaticRejectDispatcher dispatcher = new StaticRejectDispatcher(401, "{\"error\":\"USER_NOT_FOUND\"}");
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        assertThrows(ConductorClientException.class, () -> callBusinessEndpoint(client));
        // Only the init mint happened; no reactive refresh for a non-token error.
        assertEquals(1, dispatcher.tokenMints.get(),
                "non-token 401 must bubble up without minting a new token");
    }

    @Test
    public void blankErrorBodyBubblesUpWithoutRefresh() throws IOException {
        server = new MockWebServer();
        // 401 with no error field at all -> treated as a non-token failure.
        StaticRejectDispatcher dispatcher = new StaticRejectDispatcher(401, "{}");
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        assertThrows(ConductorClientException.class, () -> callBusinessEndpoint(client));
        assertEquals(1, dispatcher.tokenMints.get(),
                "401 without an error field must bubble up without minting a new token");
    }

    @Test
    public void nonJsonBodyBubblesUpWithoutRefresh() throws IOException {
        server = new MockWebServer();
        // 401 with a non-JSON (HTML) body, e.g. from a proxy/gateway -> must not refresh.
        StaticRejectDispatcher dispatcher = new StaticRejectDispatcher(
                401, "<html><body>401 Unauthorized</body></html>", "text/html");
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        assertThrows(ConductorClientException.class, () -> callBusinessEndpoint(client));
        assertEquals(1, dispatcher.tokenMints.get(),
                "401 with an unparseable body must bubble up without minting a new token");
    }

    @Test
    public void emptyBodyBubblesUpWithoutRefresh() throws IOException {
        server = new MockWebServer();
        // 401 with no body at all -> must not refresh.
        StaticRejectDispatcher dispatcher = new StaticRejectDispatcher(401, "", "text/plain");
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();

        assertThrows(ConductorClientException.class, () -> callBusinessEndpoint(client));
        assertEquals(1, dispatcher.tokenMints.get(),
                "401 with an empty body must bubble up without minting a new token");
    }

    @Test
    public void backoffThrottlesRepeatedTokenMintFailures() throws IOException {
        server = new MockWebServer();
        FailingTokenDispatcher dispatcher = new FailingTokenDispatcher();
        server.setDispatcher(dispatcher);
        server.start();

        // Init attempts one mint that fails (failure count -> 1), but build still succeeds.
        ApiClient client = buildClient();
        assertEquals(1, dispatcher.tokenAttempts.get(), "init should attempt exactly one mint");

        // Rapid subsequent calls are throttled by the exponential backoff and must not
        // hammer the /token endpoint again within the backoff window.
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () -> callBusinessEndpoint(client));
        }
        assertEquals(1, dispatcher.tokenAttempts.get(),
                "backoff must prevent additional mint attempts within the backoff window");
    }

    @Test
    public void fatalExceptionAfterMaxRefreshFailures() throws Exception {
        server = new MockWebServer();
        FailingTokenDispatcher dispatcher = new FailingTokenDispatcher();
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();
        OrkesAuthentication auth = extractAuth(client);
        assertEquals(1, dispatcher.tokenAttempts.get(), "init should attempt exactly one mint");

        // Drive 4 more mint failures (total 5) by bypassing the backoff between each.
        for (int i = 0; i < 4; i++) {
            resetBackoff(auth);
            assertThrows(RuntimeException.class, () -> auth.refreshIfStale("stale"));
        }
        assertEquals(5, getFailureCount(auth));

        // The next call must throw FatalAuthenticationException because the counter
        // has reached MAX_TOKEN_REFRESH_FAILURES.
        resetBackoff(auth);
        assertThrows(FatalAuthenticationException.class, () -> auth.refreshIfStale("stale"));
    }

    @Test
    public void fatalExceptionPropagatesThroughHeaderSupplier() throws Exception {
        server = new MockWebServer();
        FailingTokenDispatcher dispatcher = new FailingTokenDispatcher();
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();
        OrkesAuthentication auth = extractAuth(client);

        // Force the failure counter past the fatal threshold and clear the cached
        // token so the next HTTP call triggers a fresh refreshToken() via the
        // header supplier path (getToken -> cache miss -> refreshToken).
        setFailureCount(auth, 5);
        invalidateTokenCache(auth);

        // The FatalAuthenticationException should propagate out of client.execute()
        // (wrapped in UncheckedExecutionException from Guava Cache) so that
        // TaskRunner.hasFatalAuthCause() can find it in the cause chain.
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> callBusinessEndpoint(client));
        assertTrue(hasFatalAuthCause(thrown),
                "FatalAuthenticationException must be present in the cause chain");
    }

    @Test
    public void successfulMintResetsFailureCounter() throws Exception {
        server = new MockWebServer();
        SwitchableTokenDispatcher dispatcher = new SwitchableTokenDispatcher();
        dispatcher.shouldFailMints.set(true);
        server.setDispatcher(dispatcher);
        server.start();

        ApiClient client = buildClient();
        OrkesAuthentication auth = extractAuth(client);
        assertEquals(1, dispatcher.tokenAttempts.get(), "init should attempt exactly one mint");

        // Accumulate 4 consecutive failures (1 from init + 3 more).
        for (int i = 0; i < 3; i++) {
            resetBackoff(auth);
            assertThrows(RuntimeException.class, () -> auth.refreshIfStale("stale"));
        }
        assertEquals(4, getFailureCount(auth));

        // Let the next mint succeed — this must reset the failure counter to 0.
        dispatcher.shouldFailMints.set(false);
        resetBackoff(auth);
        String freshToken = auth.refreshIfStale("stale");
        assertTrue(freshToken.startsWith("token-"), "should have received a valid token");
        assertEquals(0, getFailureCount(auth), "successful mint must reset the failure counter");

        // A subsequent call through the full HTTP stack should succeed.
        ConductorClientResponse<String> response = callBusinessEndpoint(client);
        assertEquals(200, response.getStatusCode());

        // Now fail again — the counter starts from zero so 4 new failures must NOT
        // trigger FatalAuthenticationException (would need 5 to hit the threshold).
        dispatcher.shouldFailMints.set(true);
        invalidateTokenCache(auth);
        for (int i = 0; i < 4; i++) {
            resetBackoff(auth);
            assertThrows(RuntimeException.class, () -> auth.refreshIfStale("stale"));
        }
        assertEquals(4, getFailureCount(auth),
                "failure counter must have restarted from zero after the earlier success");
    }

    // ------------------------------------------------------------------
    // Dispatcher whose /token endpoint can be toggled between success and
    // failure at runtime. Business endpoint always returns 200 when a
    // valid token is present.
    // ------------------------------------------------------------------

    private static class SwitchableTokenDispatcher extends Dispatcher {
        final AtomicBoolean shouldFailMints = new AtomicBoolean(false);
        final AtomicInteger tokenAttempts = new AtomicInteger(0);
        final AtomicInteger tokenMints = new AtomicInteger(0);

        @NotNull
        @Override
        public MockResponse dispatch(@NotNull RecordedRequest request) {
            String path = request.getPath() == null ? "" : request.getPath();
            if (path.endsWith("/token") && "POST".equals(request.getMethod())) {
                tokenAttempts.incrementAndGet();
                if (shouldFailMints.get()) {
                    return new MockResponse()
                            .setResponseCode(401)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{\"error\":\"INVALID_TOKEN\"}");
                }
                String token = "token-" + tokenMints.incrementAndGet();
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("{\"token\":\"" + token + "\"}");
            }

            return new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "text/plain")
                    .setBody("ok");
        }
    }

    // ------------------------------------------------------------------
    // Reflection helpers — allow tests to bypass the real-time backoff
    // and inspect internal counters without exposing them in production.
    // ------------------------------------------------------------------

    private static OrkesAuthentication extractAuth(ApiClient client) throws Exception {
        Field f = ApiClient.class.getDeclaredField("authentication");
        f.setAccessible(true);
        return (OrkesAuthentication) f.get(client);
    }

    private static void resetBackoff(OrkesAuthentication auth) throws Exception {
        Field f = OrkesAuthentication.class.getDeclaredField("lastTokenRefreshAttempt");
        f.setAccessible(true);
        f.set(auth, 0L);
    }

    private static int getFailureCount(OrkesAuthentication auth) throws Exception {
        Field f = OrkesAuthentication.class.getDeclaredField("tokenRefreshFailures");
        f.setAccessible(true);
        return (int) f.get(auth);
    }

    private static void setFailureCount(OrkesAuthentication auth, int count) throws Exception {
        Field f = OrkesAuthentication.class.getDeclaredField("tokenRefreshFailures");
        f.setAccessible(true);
        f.set(auth, count);
    }

    @SuppressWarnings("unchecked")
    private static void invalidateTokenCache(OrkesAuthentication auth) throws Exception {
        Field f = OrkesAuthentication.class.getDeclaredField("tokenCache");
        f.setAccessible(true);
        ((Cache<String, String>) f.get(auth)).invalidateAll();
    }

    private static boolean hasFatalAuthCause(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof FatalAuthenticationException) {
                return true;
            }
        }
        return false;
    }
}
