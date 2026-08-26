/*
 * Copyright 2022 Conductor Authors.
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
package com.netflix.conductor.sdk.workflow.def.tasks;

import java.util.Arrays;
import java.util.List;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

public class ExclusiveJoin extends Task<ExclusiveJoin> {

    private final String[] joinOn;

    private String[] defaultExclusiveJoinTask = new String[0];

    /**
     * @param taskReferenceName task reference name
     * @param joinOn list of task reference names to join on
     */
    public ExclusiveJoin(String taskReferenceName, String... joinOn) {
        super(taskReferenceName, TaskType.EXCLUSIVE_JOIN);
        this.joinOn = joinOn;
    }

    ExclusiveJoin(WorkflowTask workflowTask) {
        super(workflowTask);
        this.joinOn = workflowTask.getJoinOn().toArray(new String[0]);
        List<String> defaults = workflowTask.getDefaultExclusiveJoinTask();
        this.defaultExclusiveJoinTask = defaults.toArray(new String[0]);
    }

    public ExclusiveJoin defaultExclusiveJoinTask(String... tasks) {
        this.defaultExclusiveJoinTask = tasks;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setJoinOn(Arrays.asList(joinOn));
        workflowTask.setDefaultExclusiveJoinTask(Arrays.asList(defaultExclusiveJoinTask));
    }

    public String[] getJoinOn() {
        return joinOn;
    }

    public String[] getDefaultExclusiveJoinTask() {
        return defaultExclusiveJoinTask;
    }
}
