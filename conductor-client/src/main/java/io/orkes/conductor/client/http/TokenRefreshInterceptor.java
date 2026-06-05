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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Application interceptor that mirrors the Python SDK's token-expiry handling.
 *
 * <p>On a {@code 401} or {@code 403} it inspects the JSON {@code error} field of
 * the response body. Only when that field is {@code EXPIRED_TOKEN} or
 * {@code INVALID_TOKEN} does it mint a fresh token and replay the request once.
 * Any other status, error code, or unparseable body is passed straight through
 * so the normal {@code ConductorClientException} bubbles up to the caller.
 *
 * <p>Unlike OkHttp's {@code Authenticator}, an interceptor fires for both 401
 * and 403, can examine the response body before deciding to act, and retries at
 * most once (it never re-invokes itself).
 */
public final class TokenRefreshInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshInterceptor.class);

    /** Matches io.orkes.conductor.security AuthErrorType names returned by the server. */
    static final String EXPIRED_TOKEN = "EXPIRED_TOKEN";
    static final String INVALID_TOKEN = "INVALID_TOKEN";

    private static final String AUTH_HEADER = "X-Authorization";
    private static final long MAX_BODY_PEEK_BYTES = 64 * 1024;

    private final OrkesAuthentication auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenRefreshInterceptor(OrkesAuthentication auth) {
        this.auth = auth;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.code() != 401 && response.code() != 403) {
            return response;
        }

        // Never try to refresh on the token-mint endpoint itself.
        if (request.url().encodedPath().endsWith("/token")) {
            return response;
        }

        if (!isExpiredOrInvalidToken(response)) {
            return response;
        }

        String stale = request.header(AUTH_HEADER);
        String fresh;
        try {
            fresh = auth.refreshIfStale(stale);
        } catch (Exception e) {
            LOGGER.warn("Token refresh after {} failed, bubbling up original error", response.code(), e);
            return response;
        }

        if (fresh == null || fresh.equals(stale)) {
            return response;
        }

        // Discard the failed response before replaying the request once.
        response.close();
        Request retried = request.newBuilder()
                .header(AUTH_HEADER, fresh)
                .build();
        return chain.proceed(retried);
    }

    /**
     * Peeks the response body without consuming it (so a bubble-up still has the
     * original body) and returns true only when the JSON {@code error} field is
     * {@code EXPIRED_TOKEN} or {@code INVALID_TOKEN}.
     */
    private boolean isExpiredOrInvalidToken(Response response) {
        try {
            ResponseBody peeked = response.peekBody(MAX_BODY_PEEK_BYTES);
            String body = peeked.string();
            if (body.isEmpty()) {
                return false;
            }
            String error = objectMapper.readTree(body).path("error").asText(null);
            return EXPIRED_TOKEN.equals(error) || INVALID_TOKEN.equals(error);
        } catch (Exception e) {
            // Missing/blank/unparseable error field -> treat as a non-token failure.
            return false;
        }
    }
}
