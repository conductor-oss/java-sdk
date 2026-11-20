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

import org.conductoross.conductor.sdk.Workflow;

import static org.conductoross.conductor.sdk.Conductor.switchOn;

/**
 * A decision in code between two tasks, for reruns. {@link ShipmentTasks#routeDecide} answers
 * differently the second time, so a rerun shows whether the code decided again.
 */
public class RouteWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public RouteWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "native_route", version = 1)
    public String route(String id) {
        String route = tasks.routeDecide(id);
        String ran = switchOn("route", route)
                .when("x", () -> tasks.routeX(id))
                .when("y", () -> tasks.routeY(id))
                .get();
        return route + "/" + ran + "/" + tasks.routeAfter(ran);
    }
}
