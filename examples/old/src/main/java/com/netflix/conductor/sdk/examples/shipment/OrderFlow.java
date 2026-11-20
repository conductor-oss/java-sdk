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

import java.math.BigDecimal;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.InputParam;

/**
 * Ships one line item of an order: works out the total, charges it, prints the label for the
 * shipping method, and emails the customer. A shipping method it does not know fails the run.
 */
public class OrderFlow {

    public record Request(Order orderDetail, User userDetails) {}

    private final ShipmentWorkers tasks;

    public OrderFlow(ShipmentWorkers tasks) {
        this.tasks = tasks;
    }

    @Workflow(
            name = "order_flow",
            version = 1,
            ownerEmail = "user@example.com",
            timeout = "PT60S",
            description = "Workflow to ship one order")
    public String ship(@InputParam("request") Request request) {
        Order order = request.orderDetail();
        User user = request.userDetails();

        BigDecimal total = tasks.calculateTax(order);
        tasks.chargePayment(total, user.getBillingId(), user.getBillingType());

        String label;
        switch (order.getShippingMethod()) {
            case GROUND -> label = tasks.prepareGroundShipping(user.getName(), user.getAddressLine(), order.getOrderNumber());
            case NEXT_DAY_AIR -> label = tasks.prepareAirShipping(user.getName(), user.getAddressLine(), order.getOrderNumber());
            case SAME_DAY -> label = tasks.prepareSameDayShipping(user.getName(), user.getAddressLine(), order.getOrderNumber());
            default -> throw new IllegalArgumentException("Unsupported Shipping Method");
        }

        tasks.sendEmail(user.getName(), user.getEmail(), order.getOrderNumber());
        return label;
    }
}
