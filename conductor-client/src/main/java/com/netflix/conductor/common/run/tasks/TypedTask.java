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

import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.Task;

/**
 * Base class for typed task wrappers that provide convenient access to task-specific properties.
 * Typed tasks wrap a runtime {@link Task} and offer type-safe accessors for task-specific
 * input and output data.
 */
public abstract class TypedTask {

    protected final Task task;

    /**
     * Creates a typed task wrapper.
     *
     * @param task the underlying task to wrap
     * @param expectedTaskType the expected task type
     * @throws IllegalArgumentException if task is null or not of the expected type
     */
    protected TypedTask(Task task, String expectedTaskType) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (!expectedTaskType.equals(task.getTaskType())) {
            throw new IllegalArgumentException(
                    "Expected " + expectedTaskType + " task but got: " + task.getTaskType());
        }
        this.task = task;
    }

    /**
     * Returns the underlying Task object.
     */
    public Task getTask() {
        return task;
    }

    /**
     * Returns the task ID.
     */
    public String getTaskId() {
        return task.getTaskId();
    }

    /**
     * Returns the task status.
     */
    public Task.Status getStatus() {
        return task.getStatus();
    }

    /**
     * Returns the reference task name.
     */
    public String getReferenceTaskName() {
        return task.getReferenceTaskName();
    }

    /**
     * Returns the task type.
     */
    public String getTaskType() {
        return task.getTaskType();
    }

    /**
     * Returns the workflow instance ID.
     */
    public String getWorkflowInstanceId() {
        return task.getWorkflowInstanceId();
    }

    /**
     * Returns the input data map.
     */
    public Map<String, Object> getInputData() {
        return task.getInputData();
    }

    /**
     * Returns the output data map.
     */
    public Map<String, Object> getOutputData() {
        return task.getOutputData();
    }

    /**
     * Returns the user who created this task (from _createdBy input field).
     */
    public String getCreatedBy() {
        Object value = task.getInputData().get("_createdBy");
        return value != null ? value.toString() : null;
    }

    /**
     * Helper to get an input value as a String.
     */
    protected String getInputString(String key) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Helper to get an output value as a String.
     */
    protected String getOutputString(String key) {
        Object value = task.getOutputData().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Helper to get an input value as an Integer.
     */
    protected Integer getInputInteger(String key) {
        Object value = task.getInputData().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Helper to get an output value as an Integer.
     */
    protected Integer getOutputInteger(String key) {
        Object value = task.getOutputData().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Helper to get an input value as a Boolean.
     */
    protected Boolean getInputBoolean(String key) {
        Object value = task.getInputData().get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    /**
     * Checks if an input key exists and is non-null.
     */
    protected boolean hasInput(String key) {
        return task.getInputData().containsKey(key) && task.getInputData().get(key) != null;
    }

    /**
     * Checks if an output key exists and is non-null.
     */
    protected boolean hasOutput(String key) {
        return task.getOutputData().containsKey(key) && task.getOutputData().get(key) != null;
    }
}
