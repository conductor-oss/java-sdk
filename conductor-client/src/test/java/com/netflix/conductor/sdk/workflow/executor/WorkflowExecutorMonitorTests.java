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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the completion-tracking monitor started by {@link WorkflowExecutor}'s constructors. The
 * monitor runs under {@code scheduleAtFixedRate}, which cancels all future ticks on any uncaught
 * exception, so a single failed {@code getWorkflow} call must not be allowed to escape.
 */
public class WorkflowExecutorMonitorTests {

    private static final String WORKFLOW_ID = "test-workflow-id";

    private WorkflowExecutor executorFor(WorkflowClient workflowClient) {
        return new WorkflowExecutor(
                mock(TaskClient.class),
                workflowClient,
                mock(MetadataClient.class),
                mock(AnnotatedWorkerExecutor.class));
    }

    @Test
    @DisplayName("the monitor should keep polling after a transient getWorkflow failure")
    void monitorSurvivesTransientPollFailure() throws Exception {
        Workflow completed = new Workflow();
        completed.setStatus(Workflow.WorkflowStatus.COMPLETED);

        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class))).thenReturn(WORKFLOW_ID);
        when(workflowClient.getWorkflow(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("transient failure"))
                .thenReturn(completed);

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            CompletableFuture<Workflow> future = executor.executeWorkflow("wf", 1, Map.of());

            Workflow result = future.get(5, TimeUnit.SECONDS);

            assertEquals(Workflow.WorkflowStatus.COMPLETED, result.getStatus());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("the monitor should give up and fail the future once the failure budget is spent")
    void monitorGivesUpOnPersistentPollFailure() {
        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class))).thenReturn(WORKFLOW_ID);
        when(workflowClient.getWorkflow(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("permanent failure"));

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            // Zero budget: give up on the tick after the first failure, so the test does not have
            // to sit out the production budget.
            executor.setMonitorFailureGiveUpMillis(0);

            CompletableFuture<Workflow> future = executor.executeWorkflow("wf", 1, Map.of());

            ExecutionException thrown = assertThrows(
                    ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

            assertInstanceOf(RuntimeException.class, thrown.getCause());
            assertEquals("permanent failure", thrown.getCause().getMessage());
        } finally {
            executor.shutdown();
        }
    }
}
