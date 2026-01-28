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
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;

/**
 * Typed wrapper for SUB_WORKFLOW tasks providing convenient access to sub-workflow properties.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("mySubWorkflow");
 * SubWorkflowTask subWf = new SubWorkflowTask(task);
 *
 * String subWorkflowId = subWf.getSubWorkflowId();
 * String subWorkflowName = subWf.getSubWorkflowName();
 * }</pre>
 */
public class SubWorkflowTask extends TypedTask {

    public static final String SUB_WORKFLOW_PARAM = "subWorkflowParam";
    public static final String SUB_WORKFLOW_NAME = "name";
    public static final String SUB_WORKFLOW_VERSION = "version";
    public static final String SUB_WORKFLOW_ID_OUTPUT = "subWorkflowId";

    /**
     * Creates a SubWorkflowTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a SUB_WORKFLOW task
     */
    public SubWorkflowTask(Task task) {
        super(task, TaskType.TASK_TYPE_SUB_WORKFLOW);
    }

    /**
     * Checks if the given task is a SUB_WORKFLOW task.
     */
    public static boolean isSubWorkflowTask(Task task) {
        return task != null && TaskType.TASK_TYPE_SUB_WORKFLOW.equals(task.getTaskType());
    }

    /**
     * Returns the sub-workflow parameters as a map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSubWorkflowParam() {
        Object param = task.getInputData().get(SUB_WORKFLOW_PARAM);
        if (param instanceof Map) {
            return (Map<String, Object>) param;
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the sub-workflow name, or null if not set.
     */
    public String getSubWorkflowName() {
        Object name = getSubWorkflowParam().get(SUB_WORKFLOW_NAME);
        if (name == null) {
            name = task.getInputData().get(SUB_WORKFLOW_NAME);
        }
        return name != null ? name.toString() : null;
    }

    /**
     * Returns the sub-workflow version, or null if not set.
     */
    public Integer getSubWorkflowVersion() {
        Object version = getSubWorkflowParam().get(SUB_WORKFLOW_VERSION);
        if (version == null) {
            version = task.getInputData().get(SUB_WORKFLOW_VERSION);
        }
        if (version instanceof Number) {
            return ((Number) version).intValue();
        }
        return null;
    }

    /**
     * Returns the sub-workflow instance ID (available after the sub-workflow starts), or null if not started.
     */
    public String getSubWorkflowId() {
        // Use the Task's built-in method which handles backwards compatibility
        return task.getSubWorkflowId();
    }

}
