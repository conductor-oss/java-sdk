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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

/**
 * Typed wrapper for FORK_JOIN tasks providing convenient access to fork/join properties.
 *
 * <p>FORK_JOIN tasks execute multiple branches of tasks in parallel.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myFork");
 * ForkJoinTask fork = new ForkJoinTask(task);
 *
 * List<String> joinOn = fork.getJoinOn();
 * int branchCount = fork.getForkBranchCount();
 * }</pre>
 */
public class ForkJoinTask extends TypedTask {

    public static final String FORK_TASK_TYPE = TaskType.TASK_TYPE_FORK_JOIN;
    public static final String DYNAMIC_FORK_TASK_TYPE = TaskType.TASK_TYPE_FORK_JOIN_DYNAMIC;

    private final boolean dynamic;

    /**
     * Creates a ForkJoinTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a FORK_JOIN/FORK_JOIN_DYNAMIC task
     */
    public ForkJoinTask(Task task) {
        super(task, validateAndGetType(task));
        this.dynamic = DYNAMIC_FORK_TASK_TYPE.equals(task.getTaskType());
    }

    private static String validateAndGetType(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        String type = task.getTaskType();
        if (!FORK_TASK_TYPE.equals(type) && !DYNAMIC_FORK_TASK_TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "Expected FORK_JOIN or FORK_JOIN_DYNAMIC task but got: " + type);
        }
        return type;
    }

    /**
     * Checks if the given task is a FORK_JOIN or FORK_JOIN_DYNAMIC task.
     */
    public static boolean isForkJoinTask(Task task) {
        if (task == null) {
            return false;
        }
        String type = task.getTaskType();
        return FORK_TASK_TYPE.equals(type) || DYNAMIC_FORK_TASK_TYPE.equals(type);
    }

    /**
     * Returns true if this is a dynamic fork.
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * Returns the list of task reference names to join on.
     */
    @SuppressWarnings("unchecked")
    public List<String> getJoinOn() {
        WorkflowTask workflowTask = task.getWorkflowTask();
        if (workflowTask != null && workflowTask.getJoinOn() != null) {
            return workflowTask.getJoinOn();
        }
        // Try from input data for dynamic forks
        Object joinOn = task.getInputData().get("joinOn");
        if (joinOn instanceof List) {
            return ((List<?>) joinOn).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Returns the number of fork branches from the workflow task definition.
     */
    public int getForkBranchCount() {
        WorkflowTask workflowTask = task.getWorkflowTask();
        if (workflowTask != null && workflowTask.getForkTasks() != null) {
            return workflowTask.getForkTasks().size();
        }
        return 0;
    }

    /**
     * Returns the fork tasks definition if available.
     */
    public List<List<WorkflowTask>> getForkTasks() {
        WorkflowTask workflowTask = task.getWorkflowTask();
        if (workflowTask != null && workflowTask.getForkTasks() != null) {
            return workflowTask.getForkTasks();
        }
        return Collections.emptyList();
    }
}
