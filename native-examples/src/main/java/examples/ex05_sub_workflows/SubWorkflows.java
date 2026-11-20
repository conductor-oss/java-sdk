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
package examples.ex05_sub_workflows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.child;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.startWorkflow;

/**
 * 05 Sub workflows: workflows calling workflows.
 *
 * <pre>
 *   child(Payment::charge, input)            a SUB_WORKFLOW: start the child, wait for it and get
 *                                            its result. The child is a full execution of its own,
 *                                            with its own page in the UI.
 *   async(() -> child(...))                  children side by side, one in each branch.
 *   startWorkflow(Receipt::send, input)      a START_WORKFLOW: start it and carry on without waiting.
 *   startWorkflow(name, version, input)      the same by name: any registered workflow, whether
 *                                            written in code or in JSON.
 * </pre>
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex05_sub_workflows
 * </pre>
 */
public class SubWorkflows {

    public record Order(String id, int cents, List<String> parcels) {}

    public record Charge(String orderId, int cents) {}

    public record Parcel(String orderId, String parcel) {}

    /** The receipt workflow's input: a record, so that starting it by name takes the same fields as a map. */
    public record Notice(String orderId) {}

    public record Done(String payment, List<String> shipped) {}

    public static class Tasks {

        @WorkerTask("authorize_card")
        public String authorize(String orderId, int cents) {
            return "auth-" + orderId;
        }

        @WorkerTask("capture_payment")
        public String capture(String authorization) {
            return "captured " + authorization;
        }

        @WorkerTask("ship_parcel")
        public String shipParcel(String parcel) {
            return "shipped " + parcel;
        }

        @WorkerTask("email_receipt")
        public String emailReceipt(String orderId) {
            return "receipt sent for " + orderId;
        }
    }

    /** A child workflow: authorize, then capture. */
    public static class Payment {
        private final Tasks tasks;

        public Payment(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "payment")
        public String charge(Charge charge) {
            String authorization = tasks.authorize(charge.orderId(), charge.cents());
            return tasks.capture(authorization);
        }
    }

    /** A child workflow, one per parcel. */
    public static class Shipping {
        private final Tasks tasks;

        public Shipping(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "shipping")
        public String ship(Parcel parcel) {
            return tasks.shipParcel(parcel.parcel());
        }
    }

    /** A child workflow that nobody waits for. */
    public static class Receipt {
        private final Tasks tasks;

        public Receipt(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "receipt")
        public String send(Notice notice) {
            return tasks.emailReceipt(notice.orderId());
        }
    }

    @Workflow(name = "sub_workflows")
    public Done fulfil(Order order) {
        // wait for a child's result
        String payment = child(Payment::charge, new Charge(order.id(), order.cents()));

        // two children at once
        Future<String> first = async(() -> child(Shipping::ship, new Parcel(order.id(), order.parcels().get(0))));
        Future<String> second = async(() -> child(Shipping::ship, new Parcel(order.id(), order.parcels().get(1))));
        join(first, second);

        // start one and do not wait for it; typed, then by name
        startWorkflow(Receipt::send, new Notice(order.id()));
        startWorkflow("receipt", 1, Map.of("orderId", order.id() + "-copy"));

        return new Done(payment, List.of(first.resultNow(), second.resultNow()));
    }

    public static void main(String[] args) throws InterruptedException {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(SubWorkflows.class, Payment.class, Shipping.class, Receipt.class)
                    .tasks(new Tasks())
                    .start();

            WorkflowHandle<Done> run = conductor.start(SubWorkflows::fulfil, new Order("A-1", 4250, List.of("box 1", "box 2")));
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(60)));

            // the receipts were started, not waited for: keep the workers up a moment so they finish
            Thread.sleep(3_000);
        }
        client.shutdown();
    }
}
