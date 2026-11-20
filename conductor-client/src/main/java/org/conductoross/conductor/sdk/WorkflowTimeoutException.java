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
package org.conductoross.conductor.sdk;

import java.time.Duration;

import com.netflix.conductor.common.run.Workflow.WorkflowStatus;

/**
 * Thrown to a client that waited for an execution's result longer than it was willing to: the
 * execution is still going and keeps running. An {@link IllegalStateException}, as before this
 * type existed.
 */
public class WorkflowTimeoutException extends IllegalStateException {

    private final String workflowId;
    private final WorkflowStatus status;

    public WorkflowTimeoutException(String workflowId, WorkflowStatus status, Duration waited) {
        super("execution " + workflowId + " still " + status + " after " + waited);
        this.workflowId = workflowId;
        this.status = status;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    /** What the execution was doing when the wait ended: RUNNING or PAUSED. */
    public WorkflowStatus getStatus() {
        return status;
    }
}
