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
import java.util.concurrent.atomic.AtomicInteger;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // Stands in for the one-minute default so the tests need not wait it out.
    private static final long TEST_POLL_FAILURE_TIMEOUT_MILLIS = 1000;

    private WorkflowExecutor executorFor(WorkflowClient workflowClient) {
        WorkflowExecutor executor = new WorkflowExecutor(
                mock(TaskClient.class),
                workflowClient,
                mock(MetadataClient.class),
                mock(AnnotatedWorkerExecutor.class));
        executor.setPollFailureTimeoutMillis(TEST_POLL_FAILURE_TIMEOUT_MILLIS);
        return executor;
    }

    @Test
    @DisplayName("polls failing for the whole timeout should complete that workflow's future exceptionally")
    void monitorFailsTheFutureOnPersistentPollFailure() throws Exception {
        AtomicInteger polls = new AtomicInteger();
        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class))).thenReturn(WORKFLOW_ID);
        when(workflowClient.getWorkflow(anyString(), anyBoolean())).thenAnswer(invocation -> {
            polls.incrementAndGet();
            throw new RuntimeException("poll failure");
        });

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            long start = System.nanoTime();
            CompletableFuture<Workflow> future = executor.executeWorkflow("wf", 1, Map.of());

            ExecutionException thrown = assertThrows(
                    ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertInstanceOf(RuntimeException.class, thrown.getCause());
            assertEquals("poll failure", thrown.getCause().getMessage());
            assertTrue(elapsedMillis >= TEST_POLL_FAILURE_TIMEOUT_MILLIS,
                    "future failed after " + elapsedMillis + " ms, before the timeout");
            // The workflow is untracked before its future fails, so no further polls can follow.
            int pollsAtFailure = polls.get();
            Thread.sleep(300);
            assertEquals(pollsAtFailure, polls.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("poll failures shorter than the timeout should not fail the future, and a success resets it")
    void monitorToleratesTransientPollFailures() throws Exception {
        Workflow running = new Workflow();
        running.setStatus(Workflow.WorkflowStatus.RUNNING);
        Workflow completed = new Workflow();
        completed.setStatus(Workflow.WorkflowStatus.COMPLETED);

        // Two runs of failures, each spanning ~600 ms of 100 ms ticks (short of the 1 s timeout),
        // separated by a single successful poll. Together they span ~1.4 s, past the timeout, so
        // the future completes only if that success reset the clock.
        int burst = 7;
        AtomicInteger calls = new AtomicInteger();
        WorkflowClient workflowClient = mock(WorkflowClient.class);
        when(workflowClient.startWorkflow(any(StartWorkflowRequest.class))).thenReturn(WORKFLOW_ID);
        when(workflowClient.getWorkflow(anyString(), anyBoolean())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call <= burst || (call > burst + 1 && call <= 2 * burst + 1)) {
                throw new RuntimeException("transient poll failure");
            }
            return call == burst + 1 ? running : completed;
        });

        WorkflowExecutor executor = executorFor(workflowClient);
        try {
            CompletableFuture<Workflow> future = executor.executeWorkflow("wf", 1, Map.of());
            assertEquals(Workflow.WorkflowStatus.COMPLETED, future.get(10, TimeUnit.SECONDS).getStatus());
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
