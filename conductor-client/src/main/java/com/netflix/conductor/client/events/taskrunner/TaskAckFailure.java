/*
 * Copyright 2026 Conductor Authors.
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

import lombok.Getter;
import lombok.ToString;

/**
 * Published when the server responded to a task-ack with a non-success result
 * (the ack did not throw but was declined). Distinct from {@link TaskAckError}
 * which is raised when the ack call itself throws.
 */
@Getter
@ToString
public final class TaskAckFailure extends TaskRunnerEvent {
    private final String taskId;

    public TaskAckFailure(String taskType, String taskId) {
        super(taskType);
        this.taskId = taskId;
    }
}
