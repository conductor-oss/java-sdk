/*
 * Copyright 2023 Conductor Authors.
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
package io.orkes.conductor.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.http.OrkesWorkflowClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.awaitility.Awaitility.await;

public class TestUtil {
    private static int RETRY_ATTEMPT_LIMIT = 4;
    protected static ObjectMapper objectMapper = new ObjectMapperProvider().getObjectMapper();

    public static void retryMethodCall(VoidRunnableWithException function)
            throws Exception {
        Exception lastException = null;
        for (int retryCounter = 0; retryCounter < RETRY_ATTEMPT_LIMIT; retryCounter += 1) {
            try {
                function.run();
                return;
            } catch (Exception e) {
                lastException = e;
                System.out.println("Attempt " + (retryCounter + 1) + " failed: " + e.getMessage());
                try {
                    Thread.sleep(1000 * (1 << retryCounter)); // Sleep for 2^retryCounter second(s) before retrying
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        throw lastException;
    }

    public static Object retryMethodCall(RunnableWithException function)
            throws Exception {
        Exception lastException = null;
        for (int retryCounter = 0; retryCounter < RETRY_ATTEMPT_LIMIT; retryCounter += 1) {
            try {
                return function.run();
            } catch (Exception e) {
                lastException = e;
                System.out.println("Attempt " + (retryCounter + 1) + " failed: " + e.getMessage());
                try {
                    Thread.sleep(1000 * (1 << retryCounter)); // Sleep for 2^retryCounter second(s) before retrying
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        throw lastException;
    }

    @FunctionalInterface
    public interface RunnableWithException {
        Object run() throws Exception;
    }

    @FunctionalInterface
    public interface VoidRunnableWithException {
        void run() throws Exception;
    }

    public static WorkflowDef getWorkflowDef(String path) throws IOException {
        InputStream inputStream = TestUtil.class.getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("No file found at " + path);
        }
        return objectMapper.readValue(new InputStreamReader(inputStream), WorkflowDef.class);
    }

    /**
     * Waits for a workflow to reach the expected status with polling
     *
     * @param workflowId the workflow ID to monitor
     * @param expectedStatus the expected workflow status
     * @param maxWaitTimeMs maximum time to wait in milliseconds
     * @param pollIntervalMs polling interval in milliseconds
     * @return the final workflow details
     * @throws TimeoutException if the workflow doesn't reach expected status within maxWaitTime
     */
    public static Workflow waitForWorkflowStatus(OrkesWorkflowClient workflowClient,
                                                 String workflowId,
                                                 Workflow.WorkflowStatus expectedStatus,
                                                 long maxWaitTimeMs,
                                                 long pollIntervalMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + maxWaitTimeMs;

        while (System.currentTimeMillis() < endTime) {
            Workflow workflowDetails = workflowClient.getWorkflow(workflowId, true);

            if (expectedStatus.equals(workflowDetails.getStatus())) {
                return workflowDetails; // Success!
            }

            // Check if workflow failed or terminated
            if (workflowDetails.getStatus() == Workflow.WorkflowStatus.FAILED ||
                    workflowDetails.getStatus() == Workflow.WorkflowStatus.TERMINATED) {
                throw new RuntimeException(describeFailure(workflowDetails));
            }

            Thread.sleep(pollIntervalMs);
        }

        // Timeout
        Workflow finalState = workflowClient.getWorkflow(workflowId, true);
        throw new TimeoutException(
                String.format("Workflow %s did not reach status %s within %dms. Current status: %s, tasks: %s",
                        workflowId, expectedStatus, maxWaitTimeMs, finalState.getStatus(), taskSummary(finalState)));
    }

    /**
     * Signalling a workflow immediately after start is a race — the task being signalled may not
     * exist yet. Waits until the workflow is RUNNING with at least one non-terminal task.
     */
    public static void waitUntilSignalable(OrkesWorkflowClient workflowClient,
                                           String workflowId,
                                           long maxWaitTimeMs,
                                           long pollIntervalMs) {
        await().atMost(Duration.ofMillis(maxWaitTimeMs))
                .pollInterval(Duration.ofMillis(pollIntervalMs))
                .failFast("workflow reached a terminal state before it could be signalled",
                        () -> isTerminalFailure(workflowClient.getWorkflow(workflowId, true)))
                .until(() -> {
                    Workflow workflow = workflowClient.getWorkflow(workflowId, true);
                    return workflow.getStatus() == Workflow.WorkflowStatus.RUNNING
                            && workflow.getTasks() != null
                            && workflow.getTasks().stream().anyMatch(t -> !t.getStatus().isTerminal());
                });
    }

    /**
     * Retries {@code call} until it returns without throwing. For endpoints that are eventually
     * consistent or intermittently slow, where a single attempt after a fixed sleep turns ordinary
     * latency into a test failure.
     */
    public static <T> T retryUntil(Callable<T> call, long maxWaitTimeMs, long pollIntervalMs) {
        return await().atMost(Duration.ofMillis(maxWaitTimeMs))
                .pollInterval(Duration.ofMillis(pollIntervalMs))
                .ignoreExceptions()
                .until(call, result -> true);
    }

    private static boolean isTerminalFailure(Workflow workflow) {
        return workflow.getStatus() == Workflow.WorkflowStatus.FAILED
                || workflow.getStatus() == Workflow.WorkflowStatus.TERMINATED;
    }

    /** Includes the reason and task states — without these a failure is undiagnosable from CI logs. */
    private static String describeFailure(Workflow workflow) {
        return String.format("Workflow %s ended in unexpected state: %s. Reason: %s. Tasks: %s",
                workflow.getWorkflowId(), workflow.getStatus(),
                workflow.getReasonForIncompletion() == null ? "(none given)" : workflow.getReasonForIncompletion(),
                taskSummary(workflow));
    }

    private static String taskSummary(Workflow workflow) {
        if (workflow.getTasks() == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        workflow.getTasks().forEach(t -> sb.append(t.getReferenceTaskName()).append(':')
                .append(t.getStatus())
                .append(t.getReasonForIncompletion() == null ? "" : "(" + t.getReasonForIncompletion() + ")")
                .append(' '));
        return sb.append(']').toString();
    }
}
