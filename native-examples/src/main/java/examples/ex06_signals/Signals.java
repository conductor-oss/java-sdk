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
package examples.ex06_signals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Query;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.awaitCondition;

/**
 * 06 Signals: sending messages to a running workflow.
 *
 * <pre>
 *   @Variable field            state that lasts: saved with the execution and shown as its
 *                              variables in the UI
 *   @Signal("name") method     a message in, no reply           run.signal(Cart::checkout)
 *                                                               run.signal(Cart::cancel, "reason")
 *   @Update("name") method     a message in, with a reply       int n = run.update(Cart::addItem, "mug")
 *   @Query("name") method      reads the state, changes nothing run.query(Cart::items)
 *   awaitCondition(test, t)    waits until a handler makes the test true; false when the time runs
 *                              out first
 * </pre>
 *
 * Handlers run between the workflow method's waits, never at the same time as it, so they read and
 * change fields without locks. A cart can stay open for days: nothing is held while it waits.
 *
 * <p>Handlers need the server started with {@code conductor.workflow-message-queue.enabled=true}.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex06_signals
 * </pre>
 */
public class Signals {

    public static class Tasks {

        @WorkerTask("place_order")
        public String placeOrder(String customer, List<String> items) {
            return customer + " ordered " + String.join(", ", items);
        }
    }

    /** A shopping cart: items come in by update, the checkout or a cancellation by signal. */
    public static class Cart {
        private final Tasks tasks;

        @Variable private List<String> items = new ArrayList<>();
        @Variable private boolean checkedOut;
        @Variable private String cancelled;

        public Cart(Tasks tasks) {
            this.tasks = tasks;
        }

        @Update("add_item")
        public int addItem(String item) {
            items.add(item);
            return items.size();
        }

        @Query("items")
        public List<String> items() {
            return List.copyOf(items);
        }

        @Signal("checkout")
        public void checkout() {
            checkedOut = true;
        }

        @Signal("cancel")
        public void cancel(String reason) {
            cancelled = reason;
        }

        @Workflow(name = "shopping_cart")
        public String shop(String customer) {
            boolean decided = awaitCondition(() -> checkedOut || cancelled != null, Duration.ofDays(7));
            if (!decided) {
                return customer + ": cart abandoned";
            }
            if (cancelled != null) {
                return customer + ": cancelled (" + cancelled + ")";
            }
            return tasks.placeOrder(customer, items);
        }
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Cart.class).tasks(new Tasks()).start();

            WorkflowHandle<String> cart = conductor.start(Cart::shop, "ana");
            System.out.println("execution: " + cart.workflowId());
            System.out.println("items:     " + cart.update(Cart::addItem, "mug"));
            System.out.println("items:     " + cart.update(Cart::addItem, "tea"));
            System.out.println("in cart:   " + cart.query(Cart::items));
            cart.signal(Cart::checkout);
            System.out.println("result:    " + cart.result(Duration.ofSeconds(30)));

            WorkflowHandle<String> other = conductor.start(Cart::shop, "raj");
            other.update(Cart::addItem, "plate");
            other.signal(Cart::cancel, "changed my mind");
            System.out.println("result:    " + other.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
