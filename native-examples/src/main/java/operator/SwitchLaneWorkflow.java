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

import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Workflow;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.switchOn;

/** A lane that ends in a decision, joined: the join waits for the task of the chosen case. */
public class SwitchLaneWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public SwitchLaneWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "native_switch_lane", version = 1)
    public String run(String id) {
        Promise<String> routed = async(() -> switchOn("lane_route", "slow")
                .when("slow", () -> tasks.laneSlow(id))
                .when("fast", () -> tasks.routeY(id))
                .get());
        Promise<String> other = async(() -> tasks.forkRight(id));
        return routed.get() + "+" + other.get();
    }
}
