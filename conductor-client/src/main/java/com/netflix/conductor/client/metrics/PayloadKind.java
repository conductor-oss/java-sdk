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

/**
 * Discriminator attached to outbound {@code ConductorClientRequest}s so the
 * {@link ApiClientMetrics} interceptor can label payload-size histograms with
 * the right canonical labels without serializing the body twice.
 *
 * <p>The tag is read at wire time from the OkHttp {@code Request} (set via
 * {@code Request.Builder.tag(PayloadKind.class, …)}); the body's
 * {@code contentLength()} is then exact and free, since the body has already
 * been built by the HTTP layer.
 *
 * <p>Two kinds are recognised today:
 * <ul>
 *   <li>{@link TaskResult} — body is a {@code TaskResult}; recorded as
 *       {@code task_result_size_bytes{taskType}}.</li>
 *   <li>{@link WorkflowInput} — body is a {@code StartWorkflowRequest};
 *       recorded as {@code workflow_input_size_bytes{workflowType,version}}.</li>
 * </ul>
 *
 * <p>Add a new kind by adding a permitted record below and a matching
 * {@code recordSize(...)} hook on {@link ApiClientMetrics}.
 */
public sealed interface PayloadKind {

    /** Dispatch the recorded body size to the right canonical histogram. */
    void recordSize(ApiClientMetrics metrics, long sizeBytes);

    /** Marker for task-result update bodies (e.g. {@code POST /tasks}). */
    record TaskResult(String taskType) implements PayloadKind {
        @Override
        public void recordSize(ApiClientMetrics metrics, long sizeBytes) {
            metrics.recordTaskResultSize(taskType, sizeBytes);
        }
    }

    /** Marker for workflow-start bodies (e.g. {@code POST /workflow}). */
    record WorkflowInput(String workflowType, Integer version) implements PayloadKind {
        @Override
        public void recordSize(ApiClientMetrics metrics, long sizeBytes) {
            metrics.recordWorkflowInputSize(workflowType, version, sizeBytes);
        }
    }
}
