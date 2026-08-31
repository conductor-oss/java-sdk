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
package com.netflix.conductor.sdk.workflow.executor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the completion-tracking monitor started by {@link WorkflowExecutor}'s constructors. The
 * monitor runs under {@code scheduleAtFixedRate}, which cancels all future ticks on any uncaught
 * exception, so a single failed {@code getWorkflow} call must not be allowed to escape.
 */
public class WorkflowExecutorMonitorTests {

    private static final String WORKFLOW_ID = "test-workflow-id";

    private static final String OTHER_WORKFLOW_ID = "other-test-workflow-id";

    private WorkflowExecutor executorFor(WorkflowClient workflowClient) {
        return new WorkflowExecutor(
                mock(TaskClient.class),
                workflowClient,
                mock(MetadataClient.class),
                mock(AnnotatedWorkerExecutor.class));
    }

    @Test
    @DisplayName("a failed poll should complete that workflow's future exceptionally")
    void monitorFailsTheFutureOnPollFailure() {
        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class))).thenReturn(WORKFLOW_ID);
        when(workflowClient.getWorkflow(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("poll failure"));

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            CompletableFuture<Workflow> future = executor.executeWorkflow("wf", 1, Map.of());

            ExecutionException thrown = assertThrows(
                    ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

            assertInstanceOf(RuntimeException.class, thrown.getCause());
            assertEquals("poll failure", thrown.getCause().getMessage());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("one workflow's poll failure should not stop the monitor tracking the rest")
    void monitorKeepsTrackingOtherWorkflowsAfterAFailure() throws Exception {
        Workflow completed = new Workflow();
        completed.setStatus(Workflow.WorkflowStatus.COMPLETED);

        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class)))
                .thenReturn(WORKFLOW_ID)
                .thenReturn(OTHER_WORKFLOW_ID);
        when(workflowClient.getWorkflow(eq(WORKFLOW_ID), anyBoolean()))
                .thenThrow(new RuntimeException("poll failure"));
        when(workflowClient.getWorkflow(eq(OTHER_WORKFLOW_ID), anyBoolean()))
                .thenReturn(completed);

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            CompletableFuture<Workflow> failing = executor.executeWorkflow("wf", 1, Map.of());
            assertThrows(ExecutionException.class, () -> failing.get(5, TimeUnit.SECONDS));

            // Registered only after the failure has already happened, so it can complete at all
            // only if the tick loop survived it -- scheduleAtFixedRate would have cancelled every
            // future tick had the exception been allowed to escape.
            CompletableFuture<Workflow> healthy = executor.executeWorkflow("wf", 1, Map.of());
            assertEquals(Workflow.WorkflowStatus.COMPLETED, healthy.get(5, TimeUnit.SECONDS).getStatus());
        } finally {
            executor.shutdown();
        }
    }
}
