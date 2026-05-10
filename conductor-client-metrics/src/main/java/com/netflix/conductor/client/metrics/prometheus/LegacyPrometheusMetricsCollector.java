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

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Legacy Prometheus metrics implementation preserving the metric names and
 * label conventions from the original java-sdk ({@code poll_started{type}},
 * {@code poll_success{type}}, etc.).
 *
 * <p>Events that have no legacy metric equivalent are consumed as no-ops.
 * This class is selected at runtime when {@code WORKER_CANONICAL_METRICS}
 * is not set to {@code true}.
 */
public class LegacyPrometheusMetricsCollector extends AbstractPrometheusMetricsCollector {

    private static final PrometheusMeterRegistry SHARED_REGISTRY =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    public LegacyPrometheusMetricsCollector() {
        super(SHARED_REGISTRY);
    }

    /** Package-private constructor for test isolation. */
    LegacyPrometheusMetricsCollector(PrometheusMeterRegistry registry) {
        super(registry);
    }

    @Override
    public String collectorName() {
        return "legacy";
    }

    @Override
    public void consume(PollStarted e) {
        registry.counter("poll_started", "type", e.getTaskType()).increment();
    }

    @Override
    public void consume(PollCompleted e) {
        registry.timer("poll_success", "type", e.getTaskType()).record(e.getDuration());
    }

    @Override
    public void consume(PollFailure e) {
        registry.timer("poll_failure", "type", e.getTaskType()).record(e.getDuration());
    }

    @Override
    public void consume(TaskExecutionStarted e) {
        registry.counter("task_execution_started", "type", e.getTaskType()).increment();
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        registry.timer("task_execution_completed", "type", e.getTaskType()).record(e.getDuration());
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        registry.timer("task_execution_failure", "type", e.getTaskType()).record(e.getDuration());
    }

    // --- Events with no legacy metric: no-op ---

    @Override public void consume(TaskUpdateCompleted e) { }
    @Override public void consume(TaskUpdateFailure e) { }
    @Override public void consume(TaskAckFailure e) { }
    @Override public void consume(TaskAckError e) { }
    @Override public void consume(TaskExecutionQueueFull e) { }
    @Override public void consume(TaskPaused e) { }
    @Override public void consume(ThreadUncaughtException e) { }
    @Override public void consume(TaskPayloadUsedEvent e) { }
    @Override public void consume(TaskResultPayloadSizeEvent e) { }
    @Override public void consume(WorkflowPayloadUsedEvent event) { }
    @Override public void consume(WorkflowInputPayloadSizeEvent event) { }
    @Override public void consume(WorkflowStartedEvent event) { }
}
