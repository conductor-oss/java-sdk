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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPrometheusMetricsCollectorTest {

    private LegacyPrometheusMetricsCollector collector;
    private PrometheusMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        collector = new LegacyPrometheusMetricsCollector(registry);
    }

    // --- Active legacy metrics ---

    @Test
    void pollStartedRecordsLegacyCounter() {
        collector.consume(new PollStarted("HTTP"));

        double count = registry.get("poll_started").tag("type", "HTTP").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void pollCompletedRecordsLegacyTimer() {
        collector.consume(new PollCompleted("HTTP", 200));

        var timer = registry.get("poll_success").tag("type", "HTTP").timer();
        assertEquals(1, timer.count());
    }

    @Test
    void pollFailureRecordsLegacyTimer() {
        collector.consume(new PollFailure("HTTP", 300, new RuntimeException()));

        var timer = registry.get("poll_failure").tag("type", "HTTP").timer();
        assertEquals(1, timer.count());
    }

    @Test
    void taskExecutionStartedRecordsLegacyCounter() {
        collector.consume(new TaskExecutionStarted("SIMPLE", "t1", "w1"));

        double count = registry.get("task_execution_started").tag("type", "SIMPLE").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskExecutionCompletedRecordsLegacyTimer() {
        collector.consume(new TaskExecutionCompleted("SIMPLE", "t1", "w1", 400));

        var timer = registry.get("task_execution_completed").tag("type", "SIMPLE").timer();
        assertEquals(1, timer.count());
    }

    @Test
    void taskExecutionFailureRecordsLegacyTimer() {
        collector.consume(new TaskExecutionFailure("SIMPLE", "t1", "w1", new RuntimeException(), 500));

        var timer = registry.get("task_execution_failure").tag("type", "SIMPLE").timer();
        assertEquals(1, timer.count());
    }

    // --- No-op events produce no metrics ---

    @Test
    void taskUpdateCompletedIsNoop() {
        collector.consume(new TaskUpdateCompleted("SIMPLE", "t1", "w1", "wf1", 100));

        assertNull(registry.find("task_update_time_seconds").timer());
    }

    @Test
    void taskUpdateFailureIsNoop() {
        collector.consume(new TaskUpdateFailure("SIMPLE", "t1", "w1", "wf1", new RuntimeException(), 100));

        assertNull(registry.find("task_update_time_seconds").timer());
        assertNull(registry.find("task_update_error_total").counter());
    }

    @Test
    void taskAckFailureIsNoop() {
        collector.consume(new TaskAckFailure("HTTP", "t1"));

        assertNull(registry.find("task_ack_failed_total").counter());
    }

    @Test
    void taskAckErrorIsNoop() {
        collector.consume(new TaskAckError("HTTP", "t1", new RuntimeException()));

        assertNull(registry.find("task_ack_error_total").counter());
    }

    @Test
    void taskExecutionQueueFullIsNoop() {
        collector.consume(new TaskExecutionQueueFull("SIMPLE"));

        assertNull(registry.find("task_execution_queue_full_total").counter());
    }

    @Test
    void taskPausedIsNoop() {
        collector.consume(new TaskPaused("SIMPLE"));

        assertNull(registry.find("task_paused_total").counter());
    }

    @Test
    void threadUncaughtExceptionIsNoop() {
        collector.consume(new ThreadUncaughtException(new RuntimeException()));

        assertNull(registry.find("thread_uncaught_exceptions_total").counter());
    }

    @Test
    void taskPayloadUsedIsNoop() {
        collector.consume(new TaskPayloadUsedEvent("HTTP", "WRITE", "output"));

        assertNull(registry.find("external_payload_used_total").counter());
    }

    @Test
    void taskResultPayloadSizeIsNoop() {
        collector.consume(new TaskResultPayloadSizeEvent("HTTP", 1024L));

        assertNull(registry.find("task_result_size_bytes").summary());
    }

    @Test
    void workflowPayloadUsedIsNoop() {
        collector.consume(new WorkflowPayloadUsedEvent("wf", 1, "READ", "input"));

        assertNull(registry.find("external_payload_used_total").counter());
    }

    @Test
    void workflowInputPayloadSizeIsNoop() {
        collector.consume(new WorkflowInputPayloadSizeEvent("wf", 1, 1024L));

        assertNull(registry.find("workflow_input_size_bytes").summary());
    }

    @Test
    void workflowStartedIsNoop() {
        collector.consume(new WorkflowStartedEvent("wf", 1, false, new RuntimeException()));

        assertNull(registry.find("workflow_start_error_total").counter());
    }

    // --- ActiveWorkersChanged has no default implementation to override,
    //     but the interface provides a default no-op. Legacy doesn't override it,
    //     so calling it should not produce any metric. ---

    @Test
    void activeWorkersChangedUsesDefaultNoop() {
        collector.consume(new ActiveWorkersChanged("SIMPLE", 5));

        assertNull(registry.find("active_workers").gauge());
    }

    // --- getApiClientMetrics returns NOOP ---

    @Test
    void getApiClientMetricsReturnsNoop() {
        assertSame(ApiClientMetrics.NOOP, collector.getApiClientMetrics());
    }
}
