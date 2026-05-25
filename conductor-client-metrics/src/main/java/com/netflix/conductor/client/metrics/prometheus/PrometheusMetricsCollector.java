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
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Prometheus metrics implementation emitting the harmonized metric names
 * defined in the cross-SDK metrics catalog.
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

    private final PrometheusMeterRegistry registry;
    private final PrometheusApiClientMetrics apiClientMetrics;
    private final ConcurrentHashMap<String, AtomicInteger> activeWorkerGauges = new ConcurrentHashMap<>();

    public PrometheusMetricsCollector() {
        this(SHARED_REGISTRY);
        if (instantiated.getAndSet(true)) {
            log.warn("Multiple {} instances share a single static PrometheusMeterRegistry. "
                    + "Metrics from all instances are merged into one registry; "
                    + "call startServer() only once.",
                    getClass().getSimpleName());
        }
    }

    /** Package-private constructor for test isolation. */
    PrometheusMetricsCollector(PrometheusMeterRegistry registry) {
        this.registry = registry;
        this.apiClientMetrics = new PrometheusApiClientMetrics(registry);
    }

    @Override
    public ApiClientMetrics getApiClientMetrics() {
        return apiClientMetrics;
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    public void startServer() throws IOException {
        startServer(DEFAULT_PORT, DEFAULT_ENDPOINT);
    }

    public void startServer(int port, String endpoint) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(endpoint, exchange -> {
            var body = registry.scrape();
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
