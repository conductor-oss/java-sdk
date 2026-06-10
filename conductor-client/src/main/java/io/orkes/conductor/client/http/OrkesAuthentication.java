/*
 * Copyright 2024 Conductor Authors.
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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.HeaderSupplier;

import io.orkes.conductor.client.model.GenerateTokenRequest;
import io.orkes.conductor.client.model.TokenResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class OrkesAuthentication implements HeaderSupplier {

    public static final String PROP_TOKEN_REFRESH_INTERVAL = "CONDUCTOR_SECURITY_TOKEN_REFRESH_INTERVAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(OrkesAuthentication.class);
    private static final String TOKEN_CACHE_KEY = "TOKEN";

    // Stop minting after this many consecutive failures (mirrors the Python SDK).
    private static final int MAX_TOKEN_REFRESH_FAILURES = 5;

    private final Cache<String, String> tokenCache;
    private final String keyId;
    private final String keySecret;
    private final long tokenRefreshInSeconds;
    private final Object refreshLock = new Object();

    // Guarded by refreshLock.
    private int tokenRefreshFailures = 0;
    private long lastTokenRefreshAttempt = 0;

    private TokenResource tokenResource;

    public OrkesAuthentication(String keyId, String keySecret) {
        this(keyId, keySecret, 0);
    }

    public OrkesAuthentication(String keyId, String keySecret, long tokenRefreshInSeconds) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.tokenRefreshInSeconds = getTokenRefreshInSeconds(tokenRefreshInSeconds);
        this.tokenCache = CacheBuilder.newBuilder().expireAfterWrite(this.tokenRefreshInSeconds, TimeUnit.SECONDS).build();
        LOGGER.info("Setting token refresh interval to {} seconds", this.tokenRefreshInSeconds);
    }

    private long getTokenRefreshInSeconds(long tokenRefreshInSeconds) {
        if (tokenRefreshInSeconds == 0) {
            String refreshInterval = System.getenv(PROP_TOKEN_REFRESH_INTERVAL);
            if (refreshInterval == null) {
                refreshInterval = System.getProperty(PROP_TOKEN_REFRESH_INTERVAL);
            }

            if (refreshInterval != null) {
                try {
                    return Integer.parseInt(refreshInterval);
                } catch (Exception ignored) {
                }
            }

            return 2700; // 45 minutes
        }

        return tokenRefreshInSeconds;
    }

    @Override
    public void init(ConductorClient client) {
        this.tokenResource = new TokenResource(client);
        try {
            getToken();
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
        }
    }

    @Override
    public Map<String, String> get(String method, String path) {
        if ("/token".equalsIgnoreCase(path)) {
            return Map.of();
        }

        return Map.of("X-Authorization", getToken());
    }

    public String getToken() {
        try {
            return tokenCache.get(TOKEN_CACHE_KEY, this::refreshToken);
        } catch (ExecutionException e) {
            return null;
        }
    }

    /**
     * Force a token refresh, but only if the current cached token still matches the
     * stale token that produced a 401. Prevents a thundering herd when many worker
     * threads hit the expired token simultaneously.
     *
     * @param staleToken the token value that just produced a 401 response
     * @return a fresh (or already-rotated) token
     */
    public String refreshIfStale(String staleToken) {
        synchronized (refreshLock) {
            String current = tokenCache.getIfPresent(TOKEN_CACHE_KEY);
            if (current != null && !current.equals(staleToken)) {
                return current; // another thread already rotated it
            }
            String fresh = refreshToken();
            tokenCache.put(TOKEN_CACHE_KEY, fresh); // resets TTL and unblocks others
            return fresh;
        }
    }

    /**
     * Mints a fresh token. Shared by the lazy cache-TTL reload ({@link #getToken()})
     * and the reactive {@link #refreshIfStale(String)} path. Mirrors the Python SDK's
     * backoff: after a failure, attempts are spaced by an exponential delay
     * ({@code 2^failures} seconds) and stop entirely once
     * {@link #MAX_TOKEN_REFRESH_FAILURES} consecutive failures are reached. A
     * successful mint resets the failure counter.
     */
    private String refreshToken() {
        synchronized (refreshLock) {
            if (tokenRefreshFailures >= MAX_TOKEN_REFRESH_FAILURES) {
                throw new FatalAuthenticationException("Token refresh has failed " + tokenRefreshFailures
                        + " times. Please check your authentication credentials (CONDUCTOR_AUTH_KEY/"
                        + "CONDUCTOR_AUTH_SECRET). Stopping token refresh attempts.");
            }

            if (tokenRefreshFailures > 0) {
                long backoffMillis = (1L << tokenRefreshFailures) * 1000L; // 2^failures seconds
                long sinceLastAttempt = System.currentTimeMillis() - lastTokenRefreshAttempt;
                if (sinceLastAttempt < backoffMillis) {
                    long remaining = backoffMillis - sinceLastAttempt;
                    throw new ConductorClientException("Token refresh backoff active. Please wait " + remaining
                            + "ms before the next attempt. (Failure count: " + tokenRefreshFailures + ")");
                }
            }

            lastTokenRefreshAttempt = System.currentTimeMillis();

            if (keyId == null || keySecret == null) {
                tokenRefreshFailures++;
                throw new ConductorClientException(
                        "KeyId and KeySecret must be set in order to get an authentication token");
            }

            LOGGER.debug("Refreshing token @ {}", Instant.now());
            try {
                GenerateTokenRequest generateTokenRequest = new GenerateTokenRequest(keyId, keySecret);
                TokenResponse response = tokenResource.generate(generateTokenRequest).getData();
                tokenRefreshFailures = 0;
                return response.getToken();
            } catch (RuntimeException e) {
                tokenRefreshFailures++;
                LOGGER.error("Failed to refresh authentication token (attempt {}): {}",
                        tokenRefreshFailures, e.getMessage());
                throw e;
            }
        }
    }
}
