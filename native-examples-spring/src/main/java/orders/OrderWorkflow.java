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
package orders;

import org.conductoross.conductor.sdk.Workflow;

import orders.Orders.OrderRequest;
import orders.Orders.OrderResult;
import orders.Orders.Receipt;
import orders.Orders.Reservation;

/**
 * The workflow: plain code, and not a bean. The starter finds it by its {@code @Workflow} method.
 *
 * <p>It takes its task class as a constructor parameter and calls the tasks directly. What it gets
 * is not the {@code OrderTasks} bean but a stand-in whose calls schedule each task, to run on
 * whichever instance of this service polls it; the method is replayed against the recorded tasks
 * to continue after each one, and a replayed call returns the recorded result. Its ifs, switches
 * and loops are plain Java.
 */
public class OrderWorkflow {

    private final OrderTasks tasks;

    public OrderWorkflow(OrderTasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "spring_order", version = 1)
    public OrderResult process(OrderRequest order) {
        Reservation reservation = tasks.reserve(order);
        Receipt receipt = tasks.charge(reservation);
        // A plain if: its arms call tasks, so the execution shows it as a SWITCH (if_amount) with
        // both arms and the one taken; the arm not taken shows the task it would have called.
        String tracking;
        if (receipt.amount() >= 100) {
            tracking = tasks.shipInsured(reservation, receipt.amount());
        } else {
            tracking = tasks.ship(reservation);
        }
        return new OrderResult(reservation.id(), receipt.id(), receipt.amount(), tracking);
    }
}
