/*
 * Copyright 2025 Conductor Authors.
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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import static org.junit.jupiter.api.Assertions.*;

class PrometheusApiClientMetricsTest {

    private PrometheusMeterRegistry registry;
    private PrometheusApiClientMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        metrics = new PrometheusApiClientMetrics(registry);
    }

    @Test
    void recordRequestCreatesTimerWithCorrectTags() {
        metrics.recordRequest("GET", "/api/tasks", 200, Duration.ofMillis(150));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "GET")
                .tag("uri", "/api/tasks")
                .tag("status", "200")
                .timer();
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 150);
    }

    @Test
    void recordRequestWithPostMethod() {
        metrics.recordRequest("POST", "/api/workflows", 201, Duration.ofMillis(50));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "POST")
                .tag("uri", "/api/workflows")
                .tag("status", "201")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void recordRequestNegativeStatusCodeBecomesZero() {
        metrics.recordRequest("GET", "/api/tasks", -1, Duration.ofMillis(10));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "GET")
                .tag("uri", "/api/tasks")
                .tag("status", "0")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void recordRequestZeroStatusCodeBecomesZero() {
        metrics.recordRequest("GET", "/api/tasks", 0, Duration.ofMillis(10));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("status", "0")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void recordRequestNullMethodBecomesEmpty() {
        metrics.recordRequest(null, "/api/tasks", 200, Duration.ofMillis(10));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "")
                .tag("uri", "/api/tasks")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void recordRequestNullUriBecomesEmpty() {
        metrics.recordRequest("GET", null, 200, Duration.ofMillis(10));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "GET")
                .tag("uri", "")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void multipleRequestsAccumulateInSameTimer() {
        metrics.recordRequest("GET", "/api/tasks", 200, Duration.ofMillis(100));
        metrics.recordRequest("GET", "/api/tasks", 200, Duration.ofMillis(200));
        metrics.recordRequest("GET", "/api/tasks", 200, Duration.ofMillis(300));

        var timer = registry.get("http_api_client_request_seconds")
                .tag("method", "GET")
                .tag("uri", "/api/tasks")
                .tag("status", "200")
                .timer();
        assertEquals(3, timer.count());
    }

    @Test
    void differentStatusCodesCreateSeparateTimers() {
        metrics.recordRequest("GET", "/api/tasks", 200, Duration.ofMillis(10));
        metrics.recordRequest("GET", "/api/tasks", 500, Duration.ofMillis(10));

        var timer200 = registry.get("http_api_client_request_seconds")
                .tag("status", "200").timer();
        var timer500 = registry.get("http_api_client_request_seconds")
                .tag("status", "500").timer();

        assertEquals(1, timer200.count());
        assertEquals(1, timer500.count());
    }
}
