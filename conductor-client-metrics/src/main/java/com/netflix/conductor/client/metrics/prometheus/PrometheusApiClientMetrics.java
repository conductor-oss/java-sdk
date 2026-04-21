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

import java.time.Duration;

import com.netflix.conductor.client.metrics.ApiClientMetrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Prometheus-backed implementation of {@link ApiClientMetrics} that emits the
 * canonical {@code http_api_client_request_seconds} Histogram.
 *
 * <p>Uses the same bucket set and registry as
 * {@link PrometheusMetricsCollector}, so all SDK metrics end up on a single
 * {@code /metrics} scrape surface.
 */
public final class PrometheusApiClientMetrics implements ApiClientMetrics {

    /**
     * Canonical bucket set — kept in sync with
     * {@code longrunning-wfstest/sdk-metrics-harmonization.md} and the
     * Python / Go / Ruby / Rust SDKs.
     */
    private static final Duration[] CANONICAL_BUCKETS = new Duration[] {
            Duration.ofMillis(1),
            Duration.ofMillis(5),
            Duration.ofMillis(10),
            Duration.ofMillis(25),
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(250),
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofMillis(2500),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
    };

    private final PrometheusMeterRegistry registry;

    /**
     * Construct a {@code PrometheusApiClientMetrics} that emits into a
     * fresh, standalone registry. Useful when callers don't already have a
     * {@link PrometheusMetricsCollector} wired up.
     */
    public PrometheusApiClientMetrics() {
        this(new PrometheusMeterRegistry(io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT));
    }

    /**
     * Construct a {@code PrometheusApiClientMetrics} that emits into an
     * existing registry — typically the one exposed by
     * {@link PrometheusMetricsCollector#getRegistry()} so that all SDK
     * metrics end up on the same scrape endpoint.
     */
    public PrometheusApiClientMetrics(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    @Override
    public void recordRequest(String method, String uri, int statusCode, Duration duration) {
        String statusLabel = statusCode <= 0 ? "0" : Integer.toString(statusCode);
        Timer.builder("http_api_client_request_seconds")
                .tag("method", nullToEmpty(method))
                .tag("uri", nullToEmpty(uri))
                .tag("status", statusLabel)
                .publishPercentileHistogram(false)
                .serviceLevelObjectives(CANONICAL_BUCKETS)
                .register(registry)
                .record(duration);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
