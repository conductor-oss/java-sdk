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
package org.conductoross.conductor.ai.exceptions;

import io.orkes.conductor.client.exceptions.AgentspanException;

/**
 * A worker task in a stateful run sat {@code SCHEDULED} with zero polls beyond
 * the liveness window — no worker is polling its queue (spec R11). Raised by
 * the handle's wait instead of burning the full wait timeout; the usual cause
 * is a stateful run whose per-execution domain has no live worker (e.g. the
 * owning process died or never registered under that domain).
 */
public class WorkerStallError extends AgentspanException {

    private final String taskReferenceName;
    private final String executionId;

    public WorkerStallError(String taskReferenceName, String executionId) {
        super("Worker stall detected: task '" + taskReferenceName + "' in execution " + executionId
                + " has been SCHEDULED with zero polls beyond the liveness window — no worker is polling "
                + "its queue. Is the process that started this stateful run still serving its workers?");
        this.taskReferenceName = taskReferenceName;
        this.executionId = executionId;
    }

    public String getTaskReferenceName() {
        return taskReferenceName;
    }

    public String getExecutionId() {
        return executionId;
    }
}
