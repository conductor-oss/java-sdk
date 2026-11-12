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

/** Two branches joined, then a task: for reruns inside a fork. */
public class ForkedWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public ForkedWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "native_forked", version = 1)
    public String run(String id) {
        Promise<String> left = async(() -> tasks.forkLeft(id));
        Promise<String> right = async(() -> tasks.forkRight(id));
        String both = left.get() + "+" + right.get();
        return tasks.forkAfter(id, both);
    }
}
