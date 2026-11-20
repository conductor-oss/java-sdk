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
package examples.ex04_parallel;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.fanOut;
import static org.conductoross.conductor.sdk.Conductor.join;

/**
 * 04 Parallel: several things at the same time.
 *
 * <pre>
 *   async(() -> ...)          a branch: the block runs alongside the code after it, and you get a
 *                             java.util.concurrent.Future of its result. The block may do anything a
 *                             workflow does. In the diagram, a FORK_JOIN with a branch per async.
 *   join(a, b, ...)           wait for branches: a JOIN. The first branch failure is thrown here,
 *                             as the branch threw it.
 *   future.resultNow()        a joined branch's result.
 *   fanOut(name, items, f)    a branch per item when the number of items is known only at runtime:
 *                             a dynamic fork. Waits for them all and returns the results in order.
 * </pre>
 *
 * The waiting is durable and on the server: no thread is held while the branches run.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex04_parallel
 * </pre>
 */
public class Parallel {

    public record Order(String id, int weightKg, List<String> carriers) {}

    public record Quote(String carrier, int cents) {}

    public record Checked(boolean fraudFree, String stock, Quote cheapest, List<Quote> quotes) {}

    public static class Tasks {

        @WorkerTask("check_fraud")
        public boolean checkFraud(String orderId) {
            pause();
            return true;
        }

        @WorkerTask("reserve_stock")
        public String reserveStock(String orderId) {
            pause();
            return "reserved for " + orderId;
        }

        @WorkerTask("quote_shipping")
        public Quote quote(String carrier, int weightKg) {
            pause();
            return new Quote(carrier, weightKg * (100 + carrier.length() * 10));
        }

        /** Each task takes a moment, so that running them side by side shows. */
        private static void pause() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final Tasks tasks;

    public Parallel(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "parallel")
    public Checked check(Order order) {
        // two independent checks at once
        Future<Boolean> fraud = async(() -> tasks.checkFraud(order.id()));
        Future<String> stock = async(() -> tasks.reserveStock(order.id()));
        join(fraud, stock);

        // a quote from every carrier, all at once: as many branches as carriers
        List<Quote> quotes = fanOut("quotes", order.carriers(), carrier -> tasks.quote(carrier, order.weightKg()));
        Quote cheapest = quotes.stream().min(Comparator.comparingInt(Quote::cents)).orElseThrow();

        return new Checked(fraud.resultNow(), stock.resultNow(), cheapest, quotes);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Parallel.class).tasks(new Tasks()).start();

            Order order = new Order("A-1", 3, List.of("dhl", "ups", "fedex", "local post"));
            WorkflowHandle<Checked> run = conductor.start(Parallel::check, order);
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
