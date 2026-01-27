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
 * Typed wrapper for SWITCH tasks providing convenient access to switch/decision properties.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("mySwitch");
 * SwitchTask switchTask = new SwitchTask(task);
 *
 * String evaluatedCase = switchTask.getEvaluatedCase();
 * String evaluatorType = switchTask.getEvaluatorType();
 * }</pre>
 */
public class SwitchTask extends TypedTask {

    public static final String EVALUATOR_TYPE_INPUT = "evaluatorType";
    public static final String EXPRESSION_INPUT = "expression";
    public static final String SELECTED_CASE = "selectedCase";

    public static final String EVALUATOR_TYPE_VALUE_PARAM = "value-param";
    public static final String EVALUATOR_TYPE_JAVASCRIPT = "javascript";
    public static final String EVALUATOR_TYPE_GRAALJS = "graaljs";

    /**
     * Creates a SwitchTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a SWITCH task
     */
    public SwitchTask(Task task) {
        super(task, TaskType.TASK_TYPE_SWITCH);
    }

    /**
     * Checks if the given task is a SWITCH task.
     */
    public static boolean isSwitchTask(Task task) {
        return task != null && TaskType.TASK_TYPE_SWITCH.equals(task.getTaskType());
    }

    /**
     * Returns the evaluator type (e.g., "value-param", "javascript", "graaljs"), or null if not set.
     */
    public String getEvaluatorType() {
        return getInputString(EVALUATOR_TYPE_INPUT);
    }

    /**
     * Returns true if using value-param evaluator.
     */
    public boolean isValueParamEvaluator() {
        String type = getEvaluatorType();
        return type == null || EVALUATOR_TYPE_VALUE_PARAM.equals(type);
    }

    /**
     * Returns true if using JavaScript evaluator.
     */
    public boolean isJavaScriptEvaluator() {
        String type = getEvaluatorType();
        return EVALUATOR_TYPE_JAVASCRIPT.equals(type) || EVALUATOR_TYPE_GRAALJS.equals(type);
    }

    /**
     * Returns the switch expression, or null if not set.
     */
    public String getExpression() {
        return getInputString(EXPRESSION_INPUT);
    }

    /**
     * Returns the case that was selected, or null if not available.
     */
    public String getSelectedCase() {
        return getOutputString(SELECTED_CASE);
    }
}
