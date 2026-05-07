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
package com.netflix.conductor.client.metrics.prometheus;

import java.io.IOException;
import java.time.Duration;

import com.netflix.conductor.client.metrics.ApiClientMetrics;
import com.netflix.conductor.client.metrics.PayloadKind;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp3 {@link Interceptor} that records every HTTP request made through
 * the Conductor API client into a supplied {@link ApiClientMetrics}.
 *
 * <p>The interceptor never alters the request or response and never throws
 * on metric-recording failures -- the underlying HTTP call always goes
 * through unmodified.
 */
public final class ApiClientMetricsInterceptor implements Interceptor {

    private final ApiClientMetrics metrics;

    public ApiClientMetricsInterceptor(ApiClientMetrics metrics) {
        this.metrics = metrics == null ? ApiClientMetrics.NOOP : metrics;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startNanos = System.nanoTime();
        IOException ioError = null;
        Response response = null;
        try {
            response = chain.proceed(request);
            return response;
        } catch (IOException e) {
            ioError = e;
            throw e;
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            try {
                String method = request.method();
                String uri = request.url().encodedPath();
                int status = response != null ? response.code()
                        : (ioError != null ? -1 : 0);
                metrics.recordRequest(method, uri, status, Duration.ofNanos(elapsedNanos));
                recordPayloadSizeIfTagged(request);
            } catch (Throwable ignored) {
            }
        }
    }

    private void recordPayloadSizeIfTagged(Request request) {
        PayloadKind kind = request.tag(PayloadKind.class);
        if (kind == null || request.body() == null) {
            return;
        }
        long len;
        try {
            len = request.body().contentLength();
        } catch (IOException e) {
            return;
        }
        if (len < 0) {
            return;
        }
        kind.recordSize(metrics, len);
    }
}
