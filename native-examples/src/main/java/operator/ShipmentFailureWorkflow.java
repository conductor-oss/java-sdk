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
package operator;

import java.util.Map;

import org.conductoross.conductor.sdk.Workflow;


/**
 * Started by Conductor when a native_shipment execution fails: the failure workflow named in its
 * {@code @Workflow(failureWorkflow = ...)}. Its input is the failed execution's id, status and
 * reason.
 */
public class ShipmentFailureWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public ShipmentFailureWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "native_shipment_failure", version = 1)
    public String handle(Map<String, Object> failure) {
        return tasks.recordFailure(String.valueOf(failure.get("workflowId")), String.valueOf(failure.get("reason")));
    }
}
