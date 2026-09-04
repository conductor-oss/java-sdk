/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.common.model;

import java.util.List;
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import lombok.*;

@EqualsAndHashCode
@ToString
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignalResponse {

    // Common fields in all responses
    private ReturnStrategy responseType;
    private String targetWorkflowId;
    private String targetWorkflowStatus;
    private String workflowId;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private Integer priority;
    private Map<String, Object> variables;
    private boolean signalTimeout;
    /** Variables of the target (signaled) workflow. Set for every return strategy; null from older servers. */
    private Map<String, Object> targetWorkflowVariables;

    // Fields specific to TARGET_WORKFLOW & BLOCKING_WORKFLOW (also the blocking tasks for BLOCKING_TASK_LIST)
    private List<Task> tasks;
    private String createdBy;
    private Long createTime;
    private String status;
    private Long updateTime;
    /**
     * Set only on BLOCKING_WORKFLOW responses, where it always references an entry in {@link #tasks}. Null for every
     * other strategy (for TARGET_WORKFLOW the blocker may be in a descendant workflow not present in {@code tasks})
     * and from older servers.
     */
    private String blockingTaskId;
    /** Same rule as {@link #blockingTaskId}. */
    private String blockingTaskReferenceName;

    // Fields specific to BLOCKING_TASK & BLOCKING_TASK_INPUT
    private String taskType;
    private String taskId;
    private String referenceTaskName;
    private Integer retryCount;
    private String taskDefName;
    private String workflowType;

    // Helper methods to check response type
    public boolean isTargetWorkflow() {
        return ReturnStrategy.TARGET_WORKFLOW.equals(responseType);
    }

    public boolean isBlockingWorkflow() {
        return ReturnStrategy.BLOCKING_WORKFLOW.equals(responseType);
    }

    public boolean isBlockingTask() {
        return ReturnStrategy.BLOCKING_TASK.equals(responseType);
    }

    public boolean isBlockingTaskInput() {
        return ReturnStrategy.BLOCKING_TASK_INPUT.equals(responseType);
    }

    public boolean isBlockingTaskList() {
        return ReturnStrategy.BLOCKING_TASK_LIST.equals(responseType);
    }

    /** Returns whether this response identifies at least one blocking task. */
    public boolean hasBlockingTask() {
        if (responseType == null) {
            return false;
        }

        return switch (responseType) {
            case BLOCKING_WORKFLOW -> blockingTaskId != null;
            case BLOCKING_TASK, BLOCKING_TASK_INPUT -> taskId != null;
            case BLOCKING_TASK_LIST -> tasks != null && !tasks.isEmpty();
            case TARGET_WORKFLOW -> false;
        };
    }

    // Extraction methods
    public Workflow getWorkflow() {
        if (!isTargetWorkflow() && !isBlockingWorkflow()) {
            throw new IllegalStateException(
                    String.format("Response type %s does not contain workflow details", responseType));
        }

        Workflow workflow = new Workflow();
        workflow.setWorkflowId(workflowId);
        workflow.setStatus(Workflow.WorkflowStatus.valueOf(status));
        workflow.setTasks(tasks);
        workflow.setCreatedBy(createdBy);
        workflow.setCreateTime(createTime);
        workflow.setUpdateTime(updateTime);
        workflow.setInput(input);
        workflow.setOutput(output);
        workflow.setVariables(variables);
        workflow.setPriority(priority);

        return workflow;
    }

    /** Returns the blocking task, or null when {@link #hasBlockingTask()} is false. */
    public Task getBlockingTask() {
        if (!isBlockingTask() && !isBlockingTaskInput() && !isBlockingTaskList()) {
            throw new IllegalStateException(
                    String.format("Response type %s does not contain task details", responseType));
        }
        if (!hasBlockingTask()) {
            return null;
        }
        if (isBlockingTaskList()) {
            return tasks.get(0);
        }

        Task task = new Task();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setTaskDefName(taskDefName);
        task.setWorkflowType(workflowType);
        task.setReferenceTaskName(referenceTaskName);
        task.setRetryCount(retryCount);
        if (status != null) {
            task.setStatus(Task.Status.valueOf(status));
        }
        task.setInputData(input);
        task.setOutputData(output);

        return task;
    }

    /** Returns the blocking task's input, or null when nothing is blocking (then {@code input} is the workflow's). */
    public Map<String, Object> getTaskInput() {
        if (!isBlockingTaskInput()) {
            throw new IllegalStateException(
                    String.format("Response type %s does not contain task input details", responseType));
        }
        if (!hasBlockingTask()) {
            return null;
        }

        return input;
    }
}
