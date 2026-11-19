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
package examples.ex02_conditionals;

import java.time.Duration;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.switchOn;

/**
 * 02 Conditionals: different tasks for different data.
 *
 * <p>Write a decision in plain Java ({@code if}, {@code else}, {@code switch}, switch expressions)
 * or with {@code switchOn}. Both run the same way, and the execution shows both as a SWITCH task
 * with every arm: the arm taken holds the tasks that ran, the others show what they would have
 * called.
 *
 * <pre>
 *   plain Java                          the default. Only decisions whose arms call tasks become a
 *                                       SWITCH; the rest is ordinary Java.
 *   switchOn(name, value)               a SWITCH with the name you give it, each case a lambda;
 *     .when(value, () -> ...)           .get() returns what the case taken returned, .run() is for
 *     .otherwise(() -> ...)             cases that return nothing. A value no case handles, with no
 *     .get() / .run()                   otherwise, fails the workflow.
 * </pre>
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex02_conditionals
 * </pre>
 */
public class Conditionals {

    public enum Region { EU, US, APAC }

    public record Order(String id, Region region, int weightKg, boolean express) {}

    public record Shipment(String carrier, String label, String delivery) {}

    public static class Tasks {

        @WorkerTask("book_carrier")
        public String bookCarrier(String carrier, String orderId) {
            return carrier;
        }

        @WorkerTask("print_label")
        public String printLabel(String format, String orderId) {
            return format + " label for " + orderId;
        }

        @WorkerTask("schedule_delivery")
        public String scheduleDelivery(String window, String orderId) {
            return window;
        }
    }

    private final Tasks tasks;

    public Conditionals(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "conditionals")
    public Shipment ship(Order order) {
        // a switch expression in plain Java: each arm returns what its task returned
        String carrier = switch (order.region()) {
            case EU -> tasks.bookCarrier("dhl", order.id());
            case US -> tasks.bookCarrier("ups", order.id());
            default -> tasks.bookCarrier("local post", order.id());
        };

        // if / else in plain Java
        String label;
        if (order.weightKg() > 30) {
            label = tasks.printLabel("freight", order.id());
        } else {
            label = tasks.printLabel("parcel", order.id());
        }

        // switchOn: the same kind of decision, as a SWITCH named "delivery_speed"
        String delivery = switchOn("delivery_speed", order.express() ? "express" : "standard")
                .when("express", () -> tasks.scheduleDelivery("next day", order.id()))
                .otherwise(() -> tasks.scheduleDelivery("3 to 5 days", order.id()))
                .get();

        return new Shipment(carrier, label, delivery);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Conditionals.class).tasks(new Tasks()).start();

            for (Order order : List.of(
                    new Order("A-1", Region.EU, 2, true),
                    new Order("B-2", Region.APAC, 45, false))) {
                WorkflowHandle<Shipment> run = conductor.start(Conditionals::ship, order);
                System.out.println("execution: " + run.workflowId());
                System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
            }
        }
        client.shutdown();
    }
}
