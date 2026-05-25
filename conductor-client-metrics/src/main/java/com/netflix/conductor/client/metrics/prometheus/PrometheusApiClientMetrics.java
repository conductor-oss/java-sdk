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
package com.netflix.conductor.client.metrics.prometheus;

import java.time.Duration;

import com.netflix.conductor.client.metrics.ApiClientMetrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Prometheus-backed implementation of {@link ApiClientMetrics} that emits the
 * canonical {@code http_api_client_request_seconds},
 * {@code task_result_size_bytes}, and {@code workflow_input_size_bytes}
 * histograms.
 */
public final class PrometheusApiClientMetrics implements ApiClientMetrics {

    private static final Duration[] CANONICAL_TIME_BUCKETS = {
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

    private static final double[] CANONICAL_SIZE_BUCKETS = {
            100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000
    };

    private final PrometheusMeterRegistry registry;

    public PrometheusApiClientMetrics(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordRequest(String method, String uri, int statusCode, Duration duration) {
        String statusLabel = statusCode <= 0 ? "0" : Integer.toString(statusCode);
        Timer.builder("http_api_client_request_seconds")
                .description("HTTP API client request latency in seconds")
                .tag("method", nullToEmpty(method))
                .tag("uri", nullToEmpty(uri))
                .tag("status", statusLabel)
                .publishPercentileHistogram(false)
                .serviceLevelObjectives(CANONICAL_TIME_BUCKETS)
                .register(registry)
                .record(duration);
    }

    @Override
    public void recordTaskResultSize(String taskType, long sizeBytes) {
        if (sizeBytes < 0) {
            return;
        }
        DistributionSummary.builder("task_result_size_bytes")
                .description("Records output payload size of a task in bytes")
                .tag("taskType", nullToEmpty(taskType))
                .serviceLevelObjectives(CANONICAL_SIZE_BUCKETS)
                .register(registry)
                .record(sizeBytes);
    }

    @Override
    public void recordWorkflowInputSize(String workflowType, Integer version, long sizeBytes) {
        if (sizeBytes < 0) {
            return;
        }
        DistributionSummary.builder("workflow_input_size_bytes")
                .description("Records input payload size of a workflow in bytes")
                .tag("workflowType", nullToEmpty(workflowType))
                .tag("version", version == null ? "" : version.toString())
                .serviceLevelObjectives(CANONICAL_SIZE_BUCKETS)
                .register(registry)
                .record(sizeBytes);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
