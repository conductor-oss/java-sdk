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
package com.netflix.conductor.client.metrics;

import com.netflix.conductor.client.events.listeners.TaskClientListener;
import com.netflix.conductor.client.events.listeners.TaskRunnerEventsListener;
import com.netflix.conductor.client.events.listeners.WorkflowClientListener;

public interface MetricsCollector extends TaskRunnerEventsListener, WorkflowClientListener, TaskClientListener {

    default ApiClientMetrics getApiClientMetrics() {
        return ApiClientMetrics.NOOP;
    }

    /**
     * Whether downstream clients ({@code TaskClient}, {@code WorkflowClient},
     * {@code TaskRunnerConfigurer}) should automatically register this
     * collector as an event listener when a {@code ConductorClient} built with
     * {@link com.netflix.conductor.client.http.ConductorClient.Builder#withMetricsCollector}
     * is detected.
     *
     * <p>Defaults to {@code false} so that legacy SDK upgraders see no
     * constructor side-effects. The canonical collector overrides this to
     * {@code true}. Call {@link #setAutoWiringEnabled(boolean)} to override
     * the default for any implementation.
     */
    default boolean isAutoWiringEnabled() {
        return false;
    }

    /**
     * Override the default auto-wiring behavior. No-op by default; concrete
     * implementations that support the toggle should override this.
     */
    default void setAutoWiringEnabled(boolean enabled) { }

    /**
     * Whether {@code TaskRunner} should publish {@code ActiveWorkersChanged}
     * events on every task start/finish to drive the {@code active_workers}
     * gauge. This adds two async event dispatches per task execution.
     *
     * <p>Defaults to {@code false} so legacy SDK upgraders see no additional
     * hot-path overhead. The canonical collector overrides this to
     * {@code true}. Call {@link #setActiveWorkersTrackingEnabled(boolean)} to
     * override the default for any implementation.
     */
    default boolean isActiveWorkersTrackingEnabled() {
        return false;
    }

    /**
     * Override the default active-workers tracking behavior. No-op by
     * default; concrete implementations that support the toggle should
     * override this.
     */
    default void setActiveWorkersTrackingEnabled(boolean enabled) { }

    /**
     * Whether {@code TaskRunner} should publish per-poll-cycle diagnostic
     * events ({@code TaskPaused}, {@code TaskExecutionQueueFull}) and
     * whether {@code TaskClient} should emit ack diagnostic events
     * ({@code TaskAckFailure}, {@code TaskAckError}).
     *
     * <p>Defaults to {@code false} so legacy SDK upgraders see no
     * additional hot-path overhead. The canonical collector overrides this
     * to {@code true}. Call {@link #setDiagnosticEventsEnabled(boolean)}
     * to override the default for any implementation.
     */
    default boolean isDiagnosticEventsEnabled() {
        return false;
    }

    /**
     * Override the default diagnostic-events behavior. No-op by default;
     * concrete implementations that support the toggle should override this.
     */
    default void setDiagnosticEventsEnabled(boolean enabled) { }
}
