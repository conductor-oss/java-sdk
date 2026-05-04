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

import java.util.concurrent.TimeUnit;

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

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalPrometheusMetricsCollectorTest {

    private CanonicalPrometheusMetricsCollector collector;
    private PrometheusMeterRegistry registry;

    @BeforeEach
    void setUp() {
        collector = new CanonicalPrometheusMetricsCollector();
        registry = collector.getRegistry();
    }

    // --- Poll lifecycle ---

    @Test
    void pollStartedIncrementsCounter() {
        collector.consume(new PollStarted("HTTP"));

        double count = registry.get("task_poll_total").tag("taskType", "HTTP").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void pollCompletedRecordsTimer() {
        collector.consume(new PollCompleted("HTTP", 250));

        var timer = registry.get("task_poll_time_seconds")
                .tag("taskType", "HTTP").tag("status", "SUCCESS").timer();
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 250);
    }

    @Test
    void pollFailureRecordsTimerAndErrorCounter() {
        RuntimeException cause = new RuntimeException("timeout");
        collector.consume(new PollFailure("HTTP", 300, cause));

        var timer = registry.get("task_poll_time_seconds")
                .tag("taskType", "HTTP").tag("status", "FAILURE").timer();
        assertEquals(1, timer.count());

        double errorCount = registry.get("task_poll_error_total")
                .tag("taskType", "HTTP").tag("exception", "RuntimeException").counter().count();
        assertEquals(1.0, errorCount);
    }

    // --- Task execution ---

    @Test
    void taskExecutionStartedIncrementsCounter() {
        collector.consume(new TaskExecutionStarted("SIMPLE", "t1", "w1"));

        double count = registry.get("task_execution_started_total")
                .tag("taskType", "SIMPLE").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskExecutionCompletedRecordsTimer() {
        collector.consume(new TaskExecutionCompleted("SIMPLE", "t1", "w1", 500));

        var timer = registry.get("task_execute_time_seconds")
                .tag("taskType", "SIMPLE").tag("status", "SUCCESS").timer();
        assertEquals(1, timer.count());
    }

    @Test
    void taskExecutionFailureRecordsTimerAndErrorCounter() {
        IllegalStateException cause = new IllegalStateException("bad");
        collector.consume(new TaskExecutionFailure("SIMPLE", "t1", "w1", cause, 600));

        var timer = registry.get("task_execute_time_seconds")
                .tag("taskType", "SIMPLE").tag("status", "FAILURE").timer();
        assertEquals(1, timer.count());

        double errorCount = registry.get("task_execute_error_total")
                .tag("taskType", "SIMPLE").tag("exception", "IllegalStateException").counter().count();
        assertEquals(1.0, errorCount);
    }

    // --- Active workers gauge ---

    @Test
    void activeWorkersChangedSetsGauge() {
        collector.consume(new ActiveWorkersChanged("SIMPLE", 7));

        double val = registry.get("active_workers").tag("taskType", "SIMPLE").gauge().value();
        assertEquals(7.0, val);
    }

    @Test
    void activeWorkersChangedClampsNegativeToZero() {
        collector.consume(new ActiveWorkersChanged("SIMPLE", -1));

        double val = registry.get("active_workers").tag("taskType", "SIMPLE").gauge().value();
        assertEquals(0.0, val);
    }

    @Test
    void activeWorkersChangedUpdatesExistingGauge() {
        collector.consume(new ActiveWorkersChanged("SIMPLE", 3));
        collector.consume(new ActiveWorkersChanged("SIMPLE", 10));

        double val = registry.get("active_workers").tag("taskType", "SIMPLE").gauge().value();
        assertEquals(10.0, val);
    }

    // --- Task update ---

    @Test
    void taskUpdateCompletedRecordsTimer() {
        collector.consume(new TaskUpdateCompleted("SIMPLE", "t1", "w1", "wf1", 400));

        var timer = registry.get("task_update_time_seconds")
                .tag("taskType", "SIMPLE").tag("status", "SUCCESS").timer();
        assertEquals(1, timer.count());
    }

    @Test
    void taskUpdateFailureRecordsTimerAndErrorCounter() {
        RuntimeException cause = new RuntimeException("update failed");
        collector.consume(new TaskUpdateFailure("SIMPLE", "t1", "w1", "wf1", cause, 500));

        var timer = registry.get("task_update_time_seconds")
                .tag("taskType", "SIMPLE").tag("status", "FAILURE").timer();
        assertEquals(1, timer.count());

        double errorCount = registry.get("task_update_error_total")
                .tag("taskType", "SIMPLE").tag("exception", "RuntimeException").counter().count();
        assertEquals(1.0, errorCount);
    }

    // --- Ack / queueing / lifecycle ---

    @Test
    void taskAckFailureIncrementsCounter() {
        collector.consume(new TaskAckFailure("HTTP", "t1"));

        double count = registry.get("task_ack_failed_total")
                .tag("taskType", "HTTP").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskAckErrorIncrementsCounter() {
        collector.consume(new TaskAckError("HTTP", "t1", new RuntimeException("net")));

        double count = registry.get("task_ack_error_total")
                .tag("taskType", "HTTP").tag("exception", "RuntimeException").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskExecutionQueueFullIncrementsCounter() {
        collector.consume(new TaskExecutionQueueFull("SIMPLE"));

        double count = registry.get("task_execution_queue_full_total")
                .tag("taskType", "SIMPLE").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskPausedIncrementsCounter() {
        collector.consume(new TaskPaused("SIMPLE"));

        double count = registry.get("task_paused_total")
                .tag("taskType", "SIMPLE").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void threadUncaughtExceptionIncrementsCounter() {
        collector.consume(new ThreadUncaughtException(new NullPointerException()));

        double count = registry.get("thread_uncaught_exceptions_total")
                .tag("exception", "NullPointerException").counter().count();
        assertEquals(1.0, count);
    }

    // --- Payload / workflow events ---

    @Test
    void taskPayloadUsedIncrementsCounter() {
        collector.consume(new TaskPayloadUsedEvent("HTTP", "WRITE", "output"));

        double count = registry.get("external_payload_used_total")
                .tag("entityName", "HTTP").tag("operation", "WRITE").tag("payloadType", "output")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void taskResultPayloadSizeRecordsHistogram() {
        collector.consume(new TaskResultPayloadSizeEvent("HTTP", 50_000L));

        var summary = registry.get("task_result_size_bytes")
                .tag("taskType", "HTTP").summary();
        assertEquals(1, summary.count());
        assertEquals(50_000.0, summary.totalAmount());
    }

    @Test
    void workflowPayloadUsedIncrementsCounter() {
        collector.consume(new WorkflowPayloadUsedEvent("myWf", 1, "READ", "input"));

        double count = registry.get("external_payload_used_total")
                .tag("entityName", "myWf").tag("operation", "READ").tag("payloadType", "input")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void workflowInputPayloadSizeRecordsHistogram() {
        collector.consume(new WorkflowInputPayloadSizeEvent("myWf", 2, 10_000L));

        var summary = registry.get("workflow_input_size_bytes")
                .tag("workflowType", "myWf").tag("version", "2").summary();
        assertEquals(1, summary.count());
        assertEquals(10_000.0, summary.totalAmount());
    }

    @Test
    void workflowStartedSuccessDoesNotIncrementErrorCounter() {
        collector.consume(new WorkflowStartedEvent("myWf", 1));

        assertNull(registry.find("workflow_start_error_total").counter());
    }

    @Test
    void workflowStartedFailureIncrementsErrorCounter() {
        RuntimeException cause = new RuntimeException("start failed");
        collector.consume(new WorkflowStartedEvent("myWf", 1, false, cause));

        double count = registry.get("workflow_start_error_total")
                .tag("workflowType", "myWf").tag("exception", "RuntimeException").counter().count();
        assertEquals(1.0, count);
    }

    // --- getApiClientMetrics ---

    @Test
    void getApiClientMetricsReturnsNonNull() {
        assertNotNull(collector.getApiClientMetrics());
        assertInstanceOf(PrometheusApiClientMetrics.class, collector.getApiClientMetrics());
    }

    // --- Multiple increments accumulate ---

    @Test
    void repeatedPollStartedAccumulates() {
        collector.consume(new PollStarted("HTTP"));
        collector.consume(new PollStarted("HTTP"));
        collector.consume(new PollStarted("HTTP"));

        double count = registry.get("task_poll_total").tag("taskType", "HTTP").counter().count();
        assertEquals(3.0, count);
    }

    // --- Null-safe label handling ---

    @Test
    void taskPayloadUsedWithNullFieldsDoesNotThrow() {
        assertDoesNotThrow(() -> collector.consume(new TaskPayloadUsedEvent(null, null, null)));

        double count = registry.get("external_payload_used_total")
                .tag("entityName", "").tag("operation", "").tag("payloadType", "")
                .counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void workflowInputPayloadSizeWithNullVersion() {
        assertDoesNotThrow(() -> collector.consume(new WorkflowInputPayloadSizeEvent("wf", null, 100L)));

        var summary = registry.get("workflow_input_size_bytes")
                .tag("workflowType", "wf").tag("version", "").summary();
        assertEquals(1, summary.count());
    }
}
