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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * MockWebServer dispatcher whose /token endpoint and business endpoints can
 * each be toggled between success and failure at runtime. Shared by
 * {@link TokenRefreshTest} and
 * {@link com.netflix.conductor.client.automator.TaskRunnerFatalAuthTest}.
 */
public class SwitchableTokenDispatcher extends Dispatcher {
    public final AtomicBoolean shouldFailMints = new AtomicBoolean(false);
    public final AtomicBoolean shouldExpireBusiness = new AtomicBoolean(false);
    public final AtomicInteger tokenAttempts = new AtomicInteger(0);
    public final AtomicInteger tokenMints = new AtomicInteger(0);

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

        if (shouldExpireBusiness.get()) {
            return new MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"error\":\"EXPIRED_TOKEN\"}");
        }

        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("ok");
    }
}
