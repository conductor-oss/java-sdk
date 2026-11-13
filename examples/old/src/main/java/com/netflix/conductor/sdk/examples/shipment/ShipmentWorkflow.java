/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.sdk.examples.shipment;

import java.util.List;

import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.InputParam;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.child;
import static org.conductoross.conductor.sdk.Conductor.fanOut;
import static org.conductoross.conductor.sdk.Conductor.join;

/**
 * Tracks a shipment: gets the order and the user in parallel, then ships every line item of the
 * order in parallel, each as its own {@link OrderFlow}, and records that the shipment shipped.
 */
public class ShipmentWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentWorkers runs on the workers.
    private final ShipmentWorkers tasks;

    @Variable private boolean shipped;

    public ShipmentWorkflow(ShipmentWorkers tasks) {
        this.tasks = tasks;
    }

    @Workflow(
            name = "shipment_workflow",
            version = 1,
            ownerEmail = "user@example.com",
            timeout = "PT60S",
            description = "Workflow to track shipment")
    public List<String> ship(@InputParam("shipment") Shipment shipment) {
        Promise<List<Order>> orders = async(() -> tasks.getOrderDetails(shipment.getOrderNo()));
        Promise<User> user = async(() -> tasks.getUserDetails(shipment.getUserId()));
        join(orders, user);

        // one order flow per line item, all at once
        List<String> labels =
                fanOut(
                                "process_order",
                                orders.get(),
                                order -> child(OrderFlow::ship, new OrderFlow.Request(order, user.get())))
                        .get();

        shipped = true;
        return labels;
    }
}
