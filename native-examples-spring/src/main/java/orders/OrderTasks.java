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

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import orders.Orders.OrderRequest;
import orders.Orders.Receipt;
import orders.Orders.Reservation;

/**
 * The tasks: an ordinary bean with its dependencies injected. The starter polls its
 * {@code @WorkerTask} methods and registers their task definitions from the annotation attributes.
 */
@Component
public class OrderTasks {

    private final Catalog catalog;

    public OrderTasks(Catalog catalog) {
        this.catalog = catalog;
    }

    @WorkerTask("spring_order_reserve")
    public Reservation reserve(OrderRequest order) {
        return new Reservation("res-" + UUID.randomUUID(), order.item(), order.quantity());
    }

    @WorkerTask(value = "spring_order_charge", retryCount = 5, retryDelaySeconds = 2)
    public Receipt charge(Reservation reservation) {
        return new Receipt("rcpt-" + UUID.randomUUID(), catalog.price(reservation.item()) * reservation.quantity());
    }

    @WorkerTask("spring_order_ship")
    public String ship(Reservation reservation) {
        return "trk-" + reservation.id().substring(4, 12);
    }

    @WorkerTask("spring_order_ship_insured")
    public String shipInsured(Reservation reservation, double insuredValue) {
        return "ins-" + reservation.id().substring(4, 12);
    }
}
