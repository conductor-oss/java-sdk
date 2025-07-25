/*
 * Copyright 2020 Conductor Authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.*;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamicForkJoinTaskList {

    @Builder.Default
    private List<DynamicForkJoinTask> dynamicTasks = new ArrayList<>();

    public void add(String taskName, String workflowName, String referenceName, Map<String, Object> input) {
        dynamicTasks.add(new DynamicForkJoinTask(taskName, workflowName, referenceName, input));
    }

    public void add(DynamicForkJoinTask dtask) {
        dynamicTasks.add(dtask);
    }
}