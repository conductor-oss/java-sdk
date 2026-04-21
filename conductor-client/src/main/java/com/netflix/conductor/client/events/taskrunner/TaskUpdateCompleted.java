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
package com.netflix.conductor.client.events.taskrunner;

import java.time.Duration;

import lombok.Getter;
import lombok.ToString;

/**
 * Published when the worker successfully reports a task result back to the
 * Conductor server (an UpdateTask / UpdateTaskV2 call that returned without
 * throwing).
 */
@Getter
@ToString
public final class TaskUpdateCompleted extends TaskRunnerEvent {
    private final String taskId;
    private final String workerId;
    private final String workflowInstanceId;
    private final Duration duration;

    public TaskUpdateCompleted(String taskType, String taskId, String workerId, String workflowInstanceId, long durationInMillis) {
        super(taskType);
        this.taskId = taskId;
        this.workerId = workerId;
        this.workflowInstanceId = workflowInstanceId;
        this.duration = Duration.ofMillis(durationInMillis);
    }
}
