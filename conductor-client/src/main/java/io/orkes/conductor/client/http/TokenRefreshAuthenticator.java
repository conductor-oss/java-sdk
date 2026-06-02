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

import org.jetbrains.annotations.Nullable;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Reacts to a {@code 401 Unauthorized} (e.g. an expired token) by minting a
 * fresh token and replaying the original request with it. OkHttp invokes this
 * automatically whenever a response comes back as 401, for both synchronous and
 * asynchronous calls.
 */
public final class TokenRefreshAuthenticator implements Authenticator {

    private final OrkesAuthentication auth;

    public TokenRefreshAuthenticator(OrkesAuthentication auth) {
        this.auth = auth;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response) {
        // Never try to refresh on the token-mint endpoint itself.
        if (response.request().url().encodedPath().endsWith("/token")) {
            return null;
        }
        // Loop protection: at most one retry.
        if (responseCount(response) >= 2) {
            return null;
        }

        String stale = response.request().header("X-Authorization");
        String fresh = auth.refreshIfStale(stale);
        if (fresh == null || fresh.equals(stale)) {
            return null;
        }

        return response.request().newBuilder()
                .header("X-Authorization", fresh)
                .build();
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
