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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.events.task.TaskPayloadUsedEvent;
import com.netflix.conductor.client.events.task.TaskResultPayloadSizeEvent;
import com.netflix.conductor.client.events.taskrunner.ActiveWorkersChanged;
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
import com.netflix.conductor.client.metrics.ApiClientMetrics;
import com.netflix.conductor.client.metrics.MetricsCollector;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Micrometer-backed {@link MetricsCollector} that records the harmonized metric
 * names defined in the cross-SDK metrics catalog into a caller-supplied
 * {@link MeterRegistry}.
 *
 * <p>The recommended usage is to <b>not</b> let the SDK start any HTTP server and
 * instead let the embedding application expose the metrics:
 *
 * <ul>
 *   <li><b>Spring Boot:</b> hand in the application's Micrometer
 *       {@code MeterRegistry} (e.g. the one backing
 *       {@code /actuator/prometheus}). The
 *       {@code conductor-client-metrics} auto-configuration does this
 *       automatically when a {@code MeterRegistry} bean is present.</li>
 *   <li><b>Plain Java:</b> construct a
 *       {@code io.micrometer.prometheusmetrics.PrometheusMeterRegistry}
 *       (or any other registry), pass it here, and expose it however you
 *       like &mdash; e.g. serve {@code registry.scrape()} from your own
 *       HTTP endpoint.</li>
 * </ul>
 *
 * <p>For backward compatibility, the no-argument constructor plus
 * {@link #startServer()} still spin up a minimal embedded scrape server, but
 * both are deprecated &mdash; a library should not run a web server. Prefer the
 * {@link #PrometheusMetricsCollector(MeterRegistry)} constructor.
 */
public class PrometheusMetricsCollector implements MetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsCollector.class);

    private static final PrometheusMeterRegistry SHARED_REGISTRY =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    private static final AtomicBoolean instantiated = new AtomicBoolean(false);

    private static final int DEFAULT_PORT = 9991;
    private static final String DEFAULT_ENDPOINT = "/metrics";

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

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";

    private final MeterRegistry registry;
    private final PrometheusApiClientMetrics apiClientMetrics;
    private final ConcurrentHashMap<String, AtomicInteger> activeWorkerGauges = new ConcurrentHashMap<>();

    /**
     * Create a collector that records into the supplied {@link MeterRegistry}.
     *
     * @param registry the Micrometer registry to publish metrics into; in a
     *                 Spring Boot app this is typically the application's
     *                 registry backing {@code /actuator/prometheus}.
     */
    public PrometheusMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        this.apiClientMetrics = new PrometheusApiClientMetrics(registry);
    }

    /**
     * Create a collector backed by a shared internal {@link PrometheusMeterRegistry}
     * and expose it via {@link #startServer()}.
     *
     * @deprecated A library should not run its own web server. Pass in the
     *     application's {@link MeterRegistry} via
     *     {@link #PrometheusMetricsCollector(MeterRegistry)} instead (in Spring
     *     Boot this is auto-configured), and let the application expose the
     *     scrape endpoint (e.g. Actuator's {@code /actuator/prometheus}).
     */
    @Deprecated
    public PrometheusMetricsCollector() {
        this(SHARED_REGISTRY);
        if (instantiated.getAndSet(true)) {
            log.warn("Multiple {} instances share a single static PrometheusMeterRegistry. "
                    + "Metrics from all instances are merged into one registry; "
                    + "call startServer() only once.",
                    getClass().getSimpleName());
        }
    }

    @Override
    public ApiClientMetrics getApiClientMetrics() {
        return apiClientMetrics;
    }

    public MeterRegistry getRegistry() {
        return registry;
    }

    /**
     * Start a minimal embedded HTTP server exposing the Prometheus scrape
     * endpoint at {@code http://localhost:9991/metrics}.
     *
     * @deprecated The SDK should not run a web server. Expose metrics from the
     *     embedding application instead (Spring Boot Actuator, or serve
     *     {@link #getRegistry()}'s {@code scrape()} from your own endpoint).
     * @throws IOException if the server cannot bind
     * @throws IllegalStateException if this collector was constructed with a
     *     non-Prometheus {@link MeterRegistry} (such a registry cannot be
     *     scraped by this server; expose it through your application instead)
     */
    @Deprecated
    public void startServer() throws IOException {
        startServer(DEFAULT_PORT, DEFAULT_ENDPOINT);
    }

    /**
     * Start a minimal embedded HTTP server exposing the Prometheus scrape
     * endpoint on the given port and path.
     *
     * @deprecated The SDK should not run a web server. Expose metrics from the
     *     embedding application instead (Spring Boot Actuator, or serve
     *     {@link #getRegistry()}'s {@code scrape()} from your own endpoint).
     * @throws IOException if the server cannot bind
     * @throws IllegalStateException if this collector was constructed with a
     *     non-Prometheus {@link MeterRegistry}
     */
    @Deprecated
    public void startServer(int port, String endpoint) throws IOException {
        if (!(registry instanceof PrometheusMeterRegistry prometheusRegistry)) {
            throw new IllegalStateException(
                    "startServer() requires a PrometheusMeterRegistry, but this collector was "
                            + "constructed with " + registry.getClass().getName() + ". Expose the "
                            + "registry through your application (e.g. Spring Boot Actuator) instead.");
        }
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(endpoint, exchange -> {
            var body = prometheusRegistry.scrape();
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.getBytes().length);
            try (var os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        });
        server.start();
    }

    // ----- Poll lifecycle -----

    @Override
    public void consume(PollStarted e) {
        counter("task_poll_total", "Incremented each time polling is done",
                "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(PollCompleted e) {
        counter("task_poll_total", "Incremented each time polling is done",
                "taskType", e.getTaskType());
        canonicalTaskTimer("task_poll_time_seconds", "Task poll latency in seconds",
                e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(PollFailure e) {
        canonicalTaskTimer("task_poll_time_seconds", "Task poll latency in seconds",
                e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        counter("task_poll_error_total", "Client error when polling for a task queue",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())).increment();
    }

    // ----- Task execution -----

    @Override
    public void consume(TaskExecutionStarted e) {
        counter("task_execution_started_total",
                "Incremented each time a polled task is dispatched to the worker function",
                "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        canonicalTaskTimer("task_execute_time_seconds", "Task execution latency in seconds",
                e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        canonicalTaskTimer("task_execute_time_seconds", "Task execution latency in seconds",
                e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        counter("task_execute_error_total", "Execution error",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())).increment();
    }

    // ----- Active workers gauge -----

    @Override
    public void consume(ActiveWorkersChanged e) {
        activeWorkerGauges
                .computeIfAbsent(e.getTaskType(), t -> {
                    AtomicInteger val = new AtomicInteger(0);
                    Gauge.builder("active_workers", val, AtomicInteger::doubleValue)
                            .description("Current number of worker threads actively executing a task")
                            .tag("taskType", t)
                            .register(registry);
                    return val;
                })
                .set(Math.max(0, e.getCount()));
    }

    // ----- Task update -----

    @Override
    public void consume(TaskUpdateCompleted e) {
        canonicalTaskTimer("task_update_time_seconds", "Task update (result-report) latency in seconds",
                e.getTaskType(), STATUS_SUCCESS).record(e.getDuration());
    }

    @Override
    public void consume(TaskUpdateFailure e) {
        canonicalTaskTimer("task_update_time_seconds", "Task update (result-report) latency in seconds",
                e.getTaskType(), STATUS_FAILURE).record(e.getDuration());
        counter("task_update_error_total", "Task status cannot be updated back to server",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())).increment();
    }

    // ----- Task ack / queueing / lifecycle -----

    @Override
    public void consume(TaskAckFailure e) {
        counter("task_ack_failed_total", "Task ack failed",
                "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(TaskAckError e) {
        counter("task_ack_error_total", "Task ack has encountered an exception",
                "taskType", e.getTaskType(),
                "exception", exceptionLabel(e.getCause())).increment();
    }

    @Override
    public void consume(TaskExecutionQueueFull e) {
        counter("task_execution_queue_full_total",
                "Incremented when a poll cycle is skipped because all worker threads are busy",
                "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(TaskPaused e) {
        counter("task_paused_total",
                "Counter for number of times the task has been polled, when the worker has been paused",
                "taskType", e.getTaskType()).increment();
    }

    @Override
    public void consume(ThreadUncaughtException e) {
        counter("thread_uncaught_exceptions_total",
                "Uncaught exceptions raised inside worker threads",
                "exception", exceptionLabel(e.getCause())).increment();
    }

    // ----- Payload / workflow events -----

    @Override
    public void consume(TaskPayloadUsedEvent e) {
        counter("external_payload_used_total",
                "Incremented each time external payload storage is used",
                "entityName", nullToEmpty(e.getTaskType()),
                "operation", nullToEmpty(e.getOperation()),
                "payloadType", nullToEmpty(e.getPayloadType())).increment();
    }

    @Override
    public void consume(TaskResultPayloadSizeEvent e) {
        apiClientMetrics.recordTaskResultSize(e.getTaskType(), e.getSize());
    }

    @Override
    public void consume(WorkflowPayloadUsedEvent event) {
        counter("external_payload_used_total",
                "Incremented each time external payload storage is used",
                "entityName", nullToEmpty(event.getName()),
                "operation", nullToEmpty(event.getOperation()),
                "payloadType", nullToEmpty(event.getPayloadType())).increment();
    }

    @Override
    public void consume(WorkflowInputPayloadSizeEvent event) {
        apiClientMetrics.recordWorkflowInputSize(event.getName(), event.getVersion(), event.getSize());
    }

    @Override
    public void consume(WorkflowStartedEvent event) {
        if (event.isSuccess()) {
            return;
        }
        counter("workflow_start_error_total", "Counter for workflow start errors",
                "workflowType", nullToEmpty(event.getName()),
                "exception", exceptionLabel(event.getThrowable())).increment();
    }

    // ----- Helpers -----

    private Counter counter(String name, String description, String... tagKv) {
        return Counter.builder(name)
                .description(description)
                .tags(tagKv)
                .register(registry);
    }

    private Timer canonicalTaskTimer(String name, String description, String taskType, String status) {
        return Timer.builder(name)
                .description(description)
                .tag("taskType", nullToEmpty(taskType))
                .tag("status", status)
                .publishPercentileHistogram(false)
                .serviceLevelObjectives(CANONICAL_TIME_BUCKETS)
                .register(registry);
    }

    static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Produce a bounded-cardinality label value for an exception. Uses the
     * simple class name so that the label space stays small.
     */
    static String exceptionLabel(Throwable t) {
        if (t == null) {
            return "";
        }
        Throwable cause = t;
        if (cause.getCause() != null && (
                cause instanceof java.util.concurrent.ExecutionException
                || cause instanceof java.util.concurrent.CompletionException
                || cause instanceof java.lang.reflect.InvocationTargetException)) {
            cause = cause.getCause();
        }
        String simple = cause.getClass().getSimpleName();
        if (simple == null || simple.isEmpty()) {
            String fqn = cause.getClass().getName();
            int dot = fqn.lastIndexOf('.');
            return dot < 0 ? fqn : fqn.substring(dot + 1);
        }
        return simple;
    }
}
