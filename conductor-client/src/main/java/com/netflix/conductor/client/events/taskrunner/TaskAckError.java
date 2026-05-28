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
 * Published when the task-ack call to the server threw an exception
 * client-side (network error, deserialization error, etc.). Distinct from
 * {@link TaskAckFailure} which is raised when the server returned a non-
 * success ack.
 */
@Getter
@ToString
public final class TaskAckError extends TaskRunnerEvent {
    private final String taskId;
    private final Throwable cause;

    public TaskAckError(String taskType, String taskId, Throwable cause) {
        super(taskType);
        this.taskId = taskId;
        this.cause = cause;
    }
}
