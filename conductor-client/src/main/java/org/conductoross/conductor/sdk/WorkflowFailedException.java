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

import com.netflix.conductor.common.run.Workflow.WorkflowStatus;

/**
 * Thrown from workflow code to fail the execution with a reason. Thrown to a client waiting for the
 * result of an execution that failed, with the execution's id and final status.
 */
public class WorkflowFailedException extends RuntimeException {

    private final String workflowId;
    private final WorkflowStatus status;

    /** Fails the execution this code is running for, with this reason. */
    public WorkflowFailedException(String reason) {
        this(null, null, reason);
    }

    /** An execution that ended without completing: its id, final status and reason. */
    public WorkflowFailedException(String workflowId, WorkflowStatus status, String reason) {
        super(status == null ? reason : status + ": " + reason);
        this.workflowId = workflowId;
        this.status = status;
    }

    /** The failed execution, when this was thrown to a client; null in workflow code. */
    public String getWorkflowId() {
        return workflowId;
    }

    /** FAILED, TERMINATED or TIMED_OUT when thrown to a client; null in workflow code. */
    public WorkflowStatus getStatus() {
        return status;
    }
}
