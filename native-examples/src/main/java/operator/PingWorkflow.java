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

import org.conductoross.conductor.sdk.Schedule;
import org.conductoross.conductor.sdk.Workflow;


/** Runs every five seconds on the Conductor scheduler, registered from the annotation. */
public class PingWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public PingWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    @Schedule(cron = "*/5 * * * * *", name = "native_ping_every_5s", input = "{\"source\": \"schedule\"}")
    @Workflow(name = "native_ping", version = 1)
    public String ping(Map<String, Object> input) {
        return tasks.ping(String.valueOf(input.get("source")));
    }
}
