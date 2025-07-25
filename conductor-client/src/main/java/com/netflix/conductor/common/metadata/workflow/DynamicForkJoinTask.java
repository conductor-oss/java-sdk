/*
 * Copyright 2021 Conductor Authors.
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
package com.netflix.conductor.common.metadata.workflow;

import java.util.HashMap;
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.TaskType;

import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamicForkJoinTask {

    private String taskName;

    private String workflowName;

    private String referenceName;

    @Builder.Default
    private Map<String, Object> input = new HashMap<>();

    @Builder.Default
    private String type = TaskType.SIMPLE.name();

    public DynamicForkJoinTask(String taskName, String workflowName, String referenceName, Map<String, Object> input) {
        super();
        this.taskName = taskName;
        this.workflowName = workflowName;
        this.referenceName = referenceName;
        this.input = input != null ? input : new HashMap<>();
        this.type = TaskType.SIMPLE.name();
    }
}