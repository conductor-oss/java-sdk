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
 * Typed wrapper for HUMAN tasks providing convenient access to human task properties.
 *
 * <p>HUMAN tasks require human intervention to complete, typically through a UI form.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("approvalTask");
 * HumanTask human = new HumanTask(task);
 *
 * String assignee = human.getAssignee();
 * String displayName = human.getDisplayName();
 * Map<String, Object> formData = human.getFormData();
 * }</pre>
 */
public class HumanTask extends TypedTask {

    public static final String ASSIGNEE_INPUT = "assignee";
    public static final String DISPLAY_NAME_INPUT = "displayName";
    public static final String FORM_TEMPLATE_INPUT = "formTemplate";
    public static final String FORM_DATA_OUTPUT = "formData";
    public static final String COMPLETED_BY_OUTPUT = "completedBy";

    /**
     * Creates a HumanTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a HUMAN task
     */
    public HumanTask(Task task) {
        super(task, TaskType.TASK_TYPE_HUMAN);
    }

    /**
     * Checks if the given task is a HUMAN task.
     */
    public static boolean isHumanTask(Task task) {
        return task != null && TaskType.TASK_TYPE_HUMAN.equals(task.getTaskType());
    }

    /**
     * Returns the assignee (user or group) for this task, or null if not set.
     */
    public String getAssignee() {
        return getInputString(ASSIGNEE_INPUT);
    }

    /**
     * Returns the display name for the task, or null if not set.
     */
    public String getDisplayName() {
        return getInputString(DISPLAY_NAME_INPUT);
    }

    /**
     * Returns the form template name or definition, or null if not set.
     */
    public String getFormTemplate() {
        return getInputString(FORM_TEMPLATE_INPUT);
    }

    /**
     * Returns the form data submitted by the user (from output).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFormData() {
        Object formData = task.getOutputData().get(FORM_DATA_OUTPUT);
        if (formData instanceof Map) {
            return (Map<String, Object>) formData;
        }
        return Collections.emptyMap();
    }

    /**
     * Returns who completed this task (if completed), or null if not yet completed.
     */
    public String getCompletedBy() {
        return getOutputString(COMPLETED_BY_OUTPUT);
    }

    /**
     * Returns true if the task has been assigned.
     */
    public boolean isAssigned() {
        return getAssignee() != null;
    }

    /**
     * Returns true if form data has been submitted.
     */
    public boolean hasFormData() {
        return !getFormData().isEmpty();
    }
}
