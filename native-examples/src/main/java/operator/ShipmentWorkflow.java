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

import java.util.UUID;

import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;

import static org.conductoross.conductor.sdk.Conductor.once;

/**
 * A small workflow for operator actions: the label task fails while the carrier is down, and the
 * code does not catch it, so the execution fails with the label task as the cause.
 */
public class ShipmentWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real ShipmentTasks runs on the workers.
    private final ShipmentTasks tasks;

    public ShipmentWorkflow(ShipmentTasks tasks) {
        this.tasks = tasks;
    }

    public record Order(String orderId, int weight) {}

    public record Shipment(String orderId, String trackingId, String label) {}

    @Variable private String stage = "new";

    @Workflow(name = "native_shipment", version = 1, failureWorkflow = "native_shipment_failure")
    public Shipment ship(Order order) {
        String trackingId = once("trackingId", () -> "trk-" + UUID.randomUUID().toString().substring(0, 8));
        stage = "packing";
        int boxes = tasks.pack(order.orderId(), order.weight());
        stage = "labelling";
        String label = tasks.label(trackingId, boxes);
        stage = "shipped";
        return new Shipment(order.orderId(), trackingId, label);
    }
}
