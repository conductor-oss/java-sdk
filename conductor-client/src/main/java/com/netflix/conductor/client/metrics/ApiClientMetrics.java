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

import java.time.Duration;

/**
 * Hook for recording metrics about the HTTP calls made by the generated /
 * handwritten Conductor API clients.
 *
 * <p>Canonical metrics emitted by implementations:
 * <pre>
 *   http_api_client_request_seconds{method, uri, status}                 (Histogram)
 *   task_result_size_bytes{taskType}                                     (Histogram)
 *   workflow_input_size_bytes{workflowType, version}                     (Histogram)
 * </pre>
 *
 * <p>The size histograms are populated at wire time from the OkHttp
 * {@code RequestBody.contentLength()} of bodies tagged with a
 * {@link PayloadKind}; this avoids the previous double-JSON-serialization
 * cost in {@code TaskClient}/{@code WorkflowClient}, and decouples
 * payload-size observability from {@code isEnforceThresholds}.
 *
 * <p>Keeping this as an interface (rather than wiring directly to any
 * particular metrics backend) lets {@code conductor-client} stay free of a
 * Micrometer / Prometheus dependency; the {@code conductor-client-metrics}
 * module ships the {@code PrometheusApiClientMetrics} implementation.
 */
public interface ApiClientMetrics {

    /**
     * Record a single HTTP request the SDK issued to the Conductor server.
     *
     * @param method     HTTP verb (GET, POST, ...). Never null.
     * @param uri        Request path.
     * @param statusCode HTTP status code of the response, or a negative
     *                   value if the request failed before a status was
     *                   received (network error, timeout). Implementations
     *                   typically translate negative values to a
     *                   {@code status="0"} label.
     * @param duration   Wall-clock time between request issue and response
     *                   received (or error raised). Never null.
     */
    void recordRequest(String method, String uri, int statusCode, Duration duration);

    /**
     * Record the serialized size of a task-result update body. Default no-op
     * so existing implementations stay source- and binary-compatible.
     *
     * @param taskType  Task definition name. May be empty/null if unknown.
     * @param sizeBytes Size of the JSON body in bytes (from
     *                  {@code RequestBody.contentLength()}). Implementations
     *                  should ignore negative values.
     */
    default void recordTaskResultSize(String taskType, long sizeBytes) { }

    /**
     * Record the serialized size of a workflow-start input body. Default no-op
     * so existing implementations stay source- and binary-compatible.
     *
     * @param workflowType Workflow definition name. May be empty/null if unknown.
     * @param version      Workflow version. May be null.
     * @param sizeBytes    Size of the JSON body in bytes (from
     *                     {@code RequestBody.contentLength()}). Implementations
     *                     should ignore negative values.
     */
    default void recordWorkflowInputSize(String workflowType, Integer version, long sizeBytes) { }

    /**
     * No-op instance for callers that want a non-null default.
     */
    ApiClientMetrics NOOP = (method, uri, statusCode, duration) -> { };
}
