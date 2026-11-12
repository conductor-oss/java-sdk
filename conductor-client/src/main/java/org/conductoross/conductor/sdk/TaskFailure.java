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

/**
 * Thrown at the call site in workflow code when a task the code waited on failed and its retries
 * are exhausted. Catch it to compensate or continue; let it propagate and the workflow fails with
 * its reason. The task shows FAILED in the UI either way.
 */
public class TaskFailure extends RuntimeException {

    private final String taskReferenceName;
    private final String status;

    public TaskFailure(String taskReferenceName, String status, String reason) {
        super(
                "task "
                        + taskReferenceName
                        + " "
                        + status
                        + (reason == null || reason.isBlank() ? "" : ": " + reason));
        this.taskReferenceName = taskReferenceName;
        this.status = status;
    }

    public String getTaskReferenceName() {
        return taskReferenceName;
    }

    /** The task's terminal status: FAILED, FAILED_WITH_TERMINAL_ERROR, TIMED_OUT or CANCELED. */
    public String getStatus() {
        return status;
    }
}
