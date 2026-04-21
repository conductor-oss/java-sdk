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
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.conductor.client.events.task.TaskPayloadUsedEvent;
import com.netflix.conductor.client.events.task.TaskResultPayloadSizeEvent;
import com.netflix.conductor.client.events.taskrunner.PollCompleted;
import com.netflix.conductor.client.events.taskrunner.PollFailure;
import com.netflix.conductor.client.events.taskrunner.PollStarted;
import com.netflix.conductor.client.events.taskrunner.TaskAckError;
import com.netflix.conductor.client.events.taskrunner.TaskAckFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionQueueFull;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionStarted;
import com.netflix.conductor.client.events.taskrunner.TaskPaused;
import com.netflix.conductor.client.events.taskrunner.TaskUpdateCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskUpdateFailure;
import com.netflix.conductor.client.events.taskrunner.ThreadUncaughtException;
import com.netflix.conductor.client.events.workflow.WorkflowInputPayloadSizeEvent;
import com.netflix.conductor.client.events.workflow.WorkflowPayloadUsedEvent;
import com.netflix.conductor.client.events.workflow.WorkflowStartedEvent;
import com.netflix.conductor.client.metrics.MetricsCollector;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Prometheus {@link MetricsCollector} implementation.
 *
 * <p>Canonical metric names from
 * {@code longrunning-wfstest/sdk-metrics-harmonization.md} are emitted
 * <em>alongside</em> the legacy Java metric names (e.g. {@code poll_started},
 * {@code task_execution_completed}) so that dashboards built against either
 * naming scheme continue to work during the deprecation window. Legacy names
 * will be removed in a future major release per the harmonization rollout.
 */
public class PrometheusMetricsCollector implements MetricsCollector {

    private static final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private static final int DEFAULT_PORT = 9991;

    private static final String DEFAULT_ENDPOINT = "/metrics";

    /**
     * Canonical latency histogram bucket set shared across the Python, Go,
     * Ruby, and Rust SDKs so that cross-SDK dashboards can use a common
     * {@code le} axis.
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

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";

    /**
     * Holds the backing {@link AtomicLong}s for last-value size gauges. The
     * Micrometer {@code Gauge} API requires a stable reference to a number
     * whose value it samples on every scrape, which is the opposite of the
     * "set last value" pattern. This map gives us one {@code AtomicLong} per
     * unique label-set, keyed by {@code metricName|tag1=val1|tag2=val2}.
     */
    private static final ConcurrentMap<String, AtomicLong> SIZE_GAUGES = new ConcurrentHashMap<>();

    public  void startServer() throws IOException {
        startServer(DEFAULT_PORT, DEFAULT_ENDPOINT);
    }

    public void startServer(int port, String endpoint) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(endpoint, (exchange -> {
            var body = prometheusRegistry.scrape();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        }));
        server.start();
    }

    // ---------------------------------------------------------------------
    // Poll lifecycle
    // ---------------------------------------------------------------------

    @Override
    public void consume(PollStarted e) {
        // Legacy
        prometheusRegistry.counter("poll_started", "type", e.getTaskType()).increment();
        // Canonical: every poll issued, regardless of outcome.
        prometheusRegistry.counter("task_poll_total", "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(PollCompleted e) {
        // Legacy
        prometheusRegistry.timer("poll_success", "type", e.getTaskType())
                .record(e.getDuration());
        // Canonical
        canonicalPollTimer(e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(PollFailure e) {
        // Legacy
        prometheusRegistry.timer("poll_failure", "type", e.getTaskType())
                .record(e.getDuration());
        // Canonical histogram
        canonicalPollTimer(e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        // Canonical error counter
        prometheusRegistry.counter(
                "task_poll_error_total",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())
        ).increment();
    }

    // ---------------------------------------------------------------------
    // Task execution
    // ---------------------------------------------------------------------

    @Override
    public void consume(TaskExecutionStarted e) {
        // Legacy
        prometheusRegistry.counter("task_execution_started", "type", e.getTaskType()).increment();
        // Canonical
        prometheusRegistry.counter("task_execution_started_total", "taskType", e.getTaskType())
                .increment();
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        // Legacy
        prometheusRegistry.timer("task_execution_completed", "type", e.getTaskType())
                .record(e.getDuration());
        // Canonical
        canonicalExecuteTimer(e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        // Legacy
        prometheusRegistry.timer("task_execution_failure", "type", e.getTaskType())
                .record(e.getDuration());
        // Canonical histogram
        canonicalExecuteTimer(e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        // Canonical error counter
        prometheusRegistry.counter(
                "task_execute_error_total",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())
        ).increment();
    }

    // ---------------------------------------------------------------------
    // Task update (canonical-only; no legacy equivalent in java-sdk)
    // ---------------------------------------------------------------------

    @Override
    public void consume(TaskUpdateCompleted e) {
        canonicalUpdateTimer(e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(TaskUpdateFailure e) {
        canonicalUpdateTimer(e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        prometheusRegistry.counter(
                "task_update_error_total",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())
        ).increment();
    }

    // ---------------------------------------------------------------------
    // Task ack / queueing / lifecycle (canonical-only)
    // ---------------------------------------------------------------------

    @Override
    public void consume(TaskAckFailure e) {
        prometheusRegistry.counter("task_ack_failed_total", "taskType", e.getTaskType())
                .increment();
    }

    @Override
    public void consume(TaskAckError e) {
        prometheusRegistry.counter(
                "task_ack_error_total",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())
        ).increment();
    }

    @Override
    public void consume(TaskExecutionQueueFull e) {
        prometheusRegistry.counter("task_execution_queue_full_total", "taskType", e.getTaskType())
                .increment();
    }

    @Override
    public void consume(TaskPaused e) {
        prometheusRegistry.counter("task_paused_total", "taskType", e.getTaskType())
                .increment();
    }

    @Override
    public void consume(ThreadUncaughtException e) {
        prometheusRegistry.counter(
                "thread_uncaught_exceptions_total",
                "exception", exceptionLabel(e.getCause())
        ).increment();
    }

    // ---------------------------------------------------------------------
    // Payload / workflow TODO stubs — now implemented
    // ---------------------------------------------------------------------

    @Override
    public void consume(TaskPayloadUsedEvent e) {
        // Canonical: external payload read/write. TaskPayloadUsedEvent covers
        // task input/output. operation ∈ READ|WRITE, payload_type derived
        // from the event.
        prometheusRegistry.counter(
                "external_payload_used_total",
                "entityName", nullToEmpty(e.getTaskType()),
                "operation", nullToEmpty(e.getOperation()),
                "payload_type", nullToEmpty(e.getPayloadType())
        ).increment();
    }

    @Override
    public void consume(TaskResultPayloadSizeEvent e) {
        updateSizeGauge(
                "task_result_size_bytes",
                Tags.of("taskType", nullToEmpty(e.getTaskType())),
                e.getSize()
        );
    }

    @Override
    public void consume(WorkflowPayloadUsedEvent event) {
        prometheusRegistry.counter(
                "external_payload_used_total",
                "entityName", nullToEmpty(event.getName()),
                "operation", nullToEmpty(event.getOperation()),
                "payload_type", nullToEmpty(event.getPayloadType())
        ).increment();
    }

    @Override
    public void consume(WorkflowInputPayloadSizeEvent event) {
        updateSizeGauge(
                "workflow_input_size_bytes",
                Tags.of(
                        "workflowType", nullToEmpty(event.getName()),
                        "version", versionLabel(event.getVersion())
                ),
                event.getSize()
        );
    }

    @Override
    public void consume(WorkflowStartedEvent event) {
        if (event.isSuccess()) {
            return;
        }
        prometheusRegistry.counter(
                "workflow_start_error_total",
                "workflowType", nullToEmpty(event.getName()),
                "exception", exceptionLabel(event.getThrowable())
        ).increment();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Accessor for the underlying registry so that higher layers (e.g. a
     * companion {@code PrometheusApiClientMetrics}) can record into the same
     * scrape surface without having to spin up a second registry.
     */
    public PrometheusMeterRegistry getRegistry() {
        return prometheusRegistry;
    }

    private static Timer canonicalPollTimer(String taskType, String status) {
        return canonicalTaskTimer("task_poll_time_seconds", taskType, status);
    }

    private static Timer canonicalExecuteTimer(String taskType, String status) {
        return canonicalTaskTimer("task_execute_time_seconds", taskType, status);
    }

    private static Timer canonicalUpdateTimer(String taskType, String status) {
        return canonicalTaskTimer("task_update_time_seconds", taskType, status);
    }

    private static Timer canonicalTaskTimer(String name, String taskType, String status) {
        return Timer.builder(name)
                .tag("taskType", nullToEmpty(taskType))
                .tag("status", status)
                .publishPercentileHistogram(false)
                .serviceLevelObjectives(CANONICAL_BUCKETS)
                .register(prometheusRegistry);
    }

    /**
     * Register-or-update a last-value size gauge. Micrometer's gauge API
     * requires the caller to hold a stable number reference that it samples;
     * we do so via {@link AtomicLong} kept alive in {@link #SIZE_GAUGES}.
     */
    private static void updateSizeGauge(String name, Tags tags, long value) {
        String key = gaugeKey(name, tags);
        AtomicLong holder = SIZE_GAUGES.computeIfAbsent(key, k -> {
            AtomicLong created = new AtomicLong();
            Gauge.builder(name, created, AtomicLong::doubleValue)
                    .tags(tags)
                    .register(prometheusRegistry);
            return created;
        });
        holder.set(value);
    }

    private static String gaugeKey(String name, Tags tags) {
        StringBuilder sb = new StringBuilder(name);
        tags.forEach(t -> sb.append('|').append(t.getKey()).append('=').append(t.getValue()));
        return sb.toString();
    }

    /**
     * Produce a bounded-cardinality label value for an exception. Uses the
     * simple class name ({@code IOException} rather than a stack trace or
     * {@code getMessage()}) so that the label space stays small even when
     * the same error type carries user-specific message content.
     */
    private static String exceptionLabel(Throwable t) {
        if (t == null) {
            return "";
        }
        // Unwrap one level of wrapper exceptions (InvocationTargetException,
        // ExecutionException, CompletionException) to get at the root cause
        // the user actually cares about.
        Throwable cause = t;
        if (cause.getCause() != null && (
                cause instanceof java.util.concurrent.ExecutionException
                || cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.lang.reflect.InvocationTargetException)) {
            cause = cause.getCause();
        }
        String simple = cause.getClass().getSimpleName();
        if (simple == null || simple.isEmpty()) {
            // Anonymous inner classes return "" from getSimpleName(); fall
            // back to the FQN's last segment so the label is never empty.
            String fqn = cause.getClass().getName();
            int dot = fqn.lastIndexOf('.');
            return dot < 0 ? fqn : fqn.substring(dot + 1);
        }
        return simple;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String versionLabel(Integer v) {
        return v == null ? "" : v.toString();
    }
}
