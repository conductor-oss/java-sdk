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
package com.netflix.conductor.common.run.tasks;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.run.Workflow;

/**
 * Typed wrapper for TERMINATE tasks providing convenient access to termination properties.
 *
 * <p>TERMINATE tasks end the workflow execution with a specified status.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("terminateOnError");
 * TerminateTask terminate = new TerminateTask(task);
 *
 * Workflow.WorkflowStatus status = terminate.getTerminationStatus();
 * String reason = terminate.getTerminationReason();
 * }</pre>
 */
public class TerminateTask extends TypedTask {

    public static final String TERMINATION_STATUS_INPUT = "terminationStatus";
    public static final String TERMINATION_REASON_INPUT = "terminationReason";
    public static final String WORKFLOW_OUTPUT_INPUT = "workflowOutput";

    /**
     * Creates a TerminateTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a TERMINATE task
     */
    public TerminateTask(Task task) {
        super(task, TaskType.TASK_TYPE_TERMINATE);
    }

    /**
     * Checks if the given task is a TERMINATE task.
     */
    public static boolean isTerminateTask(Task task) {
        return task != null && TaskType.TASK_TYPE_TERMINATE.equals(task.getTaskType());
    }

    /**
     * Returns the termination status, or null if not set or invalid.
     */
    public Workflow.WorkflowStatus getTerminationStatus() {
        String status = getTerminationStatusString();
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return Workflow.WorkflowStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the raw termination status string, or null if not set.
     */
    public String getTerminationStatusString() {
        return getInputString(TERMINATION_STATUS_INPUT);
    }

    /**
     * Returns true if the workflow will be terminated as COMPLETED.
     */
    public boolean isTerminatingAsCompleted() {
        return Workflow.WorkflowStatus.COMPLETED.equals(getTerminationStatus());
    }

    /**
     * Returns true if the workflow will be terminated as FAILED.
     */
    public boolean isTerminatingAsFailed() {
        return Workflow.WorkflowStatus.FAILED.equals(getTerminationStatus());
    }

    /**
     * Returns the termination reason, or null if not set.
     */
    public String getTerminationReason() {
        return getInputString(TERMINATION_REASON_INPUT);
    }

    /**
     * Returns the workflow output that will be set on termination, or null if not set.
     */
    public Object getWorkflowOutput() {
        return task.getInputData().get(WORKFLOW_OUTPUT_INPUT);
    }
}
