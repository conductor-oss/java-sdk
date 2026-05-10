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

/**
 * Source-compatibility alias for {@link LegacyPrometheusMetricsCollector}, kept
 * so callers that wrote {@code new PrometheusMetricsCollector()} against
 * earlier 4.0.x releases continue to compile and emit the same legacy meter
 * names byte-for-byte:
 *
 * <ul>
 *   <li>{@code poll_started{type}}</li>
 *   <li>{@code poll_success{type}}</li>
 *   <li>{@code poll_failure{type}}</li>
 *   <li>{@code task_execution_started{type}}</li>
 *   <li>{@code task_execution_completed{type}}</li>
 *   <li>{@code task_execution_failure{type}}</li>
 * </ul>
 *
 * <p>This class deliberately delegates to {@link LegacyPrometheusMetricsCollector}
 * (rather than {@link MetricsCollectorFactory#create()}) so that an upgrader who
 * already has {@code WORKER_CANONICAL_METRICS=true} set in their environment
 * does not silently flip to the canonical metric surface just by upgrading the
 * SDK. Callers that want the env-var-driven selection should switch to
 * {@link MetricsCollectorFactory#create()} or {@link MetricsBundle#create(int)}.
 *
 * @deprecated Use {@link MetricsCollectorFactory#create()} (or
 *             {@link MetricsBundle#create(int)}) for new code, which selects
 *             between {@link LegacyPrometheusMetricsCollector} and
 *             {@link CanonicalPrometheusMetricsCollector} based on
 *             {@code WORKER_CANONICAL_METRICS}.
 */
@Deprecated
public class PrometheusMetricsCollector extends LegacyPrometheusMetricsCollector {
}
