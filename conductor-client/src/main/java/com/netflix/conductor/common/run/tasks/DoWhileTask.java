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

/**
 * Typed wrapper for DO_WHILE tasks providing convenient access to loop properties.
 *
 * <p>DO_WHILE tasks execute a set of tasks repeatedly until a condition is met.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myLoop");
 * DoWhileTask doWhile = new DoWhileTask(task);
 *
 * int iteration = doWhile.getCurrentIteration();
 * String condition = doWhile.getLoopCondition();
 * }</pre>
 */
public class DoWhileTask extends TypedTask {

    public static final String LOOP_CONDITION_INPUT = "loopCondition";
    public static final String ITERATION_OUTPUT = "iteration";

    /**
     * Creates a DoWhileTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a DO_WHILE task
     */
    public DoWhileTask(Task task) {
        super(task, TaskType.TASK_TYPE_DO_WHILE);
    }

    /**
     * Checks if the given task is a DO_WHILE task.
     */
    public static boolean isDoWhileTask(Task task) {
        return task != null && TaskType.TASK_TYPE_DO_WHILE.equals(task.getTaskType());
    }

    /**
     * Returns the loop condition expression, or null if not set.
     */
    public String getLoopCondition() {
        return getInputString(LOOP_CONDITION_INPUT);
    }

    /**
     * Returns the current iteration number (1-based from the task's iteration field).
     */
    public int getCurrentIteration() {
        return task.getIteration();
    }

    /**
     * Returns the iteration count from output (may differ from current iteration), or null if not available.
     */
    public Integer getIterationCount() {
        return getOutputInteger(ITERATION_OUTPUT);
    }

    /**
     * Returns true if this is the first iteration.
     */
    public boolean isFirstIteration() {
        return getCurrentIteration() <= 1;
    }

    /**
     * Returns true if the loop task is part of a loop iteration (iteration > 0).
     */
    public boolean isLoopOverTask() {
        return task.isLoopOverTask();
    }
}
