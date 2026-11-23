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
package examples.ex15_strict_determinism;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.random;

/**
 * 15 Strict determinism: a workflow that is refused, and the same workflow fixed.
 *
 * <p>Workflow code runs again from the top each time it continues, and again when an execution is
 * retried or rerun from a task; the results of the tasks it already ran are given back to it. So it
 * must take the same path every time. {@code Math.random()}, {@code UUID.randomUUID()} and {@code
 * Instant.now()} give another answer on each run: a coupon decided by one would be decided again,
 * differently, on a retry.
 *
 * <p>The workers read the compiled workflow code when they start and find such calls, through
 * lambdas and helper methods. By default each is a warning in the log. With the strict determinism
 * setting the workflow is refused: {@code start()} throws {@link NonDeterminismException} naming
 * every place and what to use instead, before anything is registered or polled.
 *
 * <pre>
 *   conductor.workflow.&lt;name&gt;.strictDeterminism=true   refuse this workflow when its code is not
 *   conductor.workflow.all.strictDeterminism=true      deterministic; or every workflow. A system
 *                                                      property, or the Spring environment.
 *
 *   instead of                          use
 *   Math.random(), new Random()         random()                 a number, recorded
 *   Instant.now(), System.currentTime.. now()                    the time, recorded
 *   UUID.randomUUID(), anything else    once(name, () -> ...)    runs once, its value recorded
 * </pre>
 *
 * <p>{@link RandomCoupon} below is refused. {@link RecordedCoupon} is the same workflow with the
 * three calls replaced, and runs.
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex15_strict_determinism
 * </pre>
 */
public class StrictDeterminism {

    public record Coupon(String code, int percent, Instant issued, String sentTo) {}

    public static class Tasks {

        @WorkerTask("apply_discount")
        public int applyDiscount(String customer, int percent) {
            return percent;
        }

        @WorkerTask("send_coupon")
        public String sendCoupon(String customer, String code) {
            return customer + "@example.com";
        }
    }

    /** Refused under strict determinism: three places ask the JVM for a value that changes each run. */
    public static class RandomCoupon {

        private final Tasks tasks;

        public RandomCoupon(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "random_coupon")
        public Coupon issue(String customer) {
            // a new number each time this code runs: a retry after apply_discount could find the
            // customer lucky the second time, and take the other arm
            boolean lucky = Math.random() < 0.5;
            int percent;
            if (lucky) {
                percent = tasks.applyDiscount(customer, 20);
            } else {
                percent = tasks.applyDiscount(customer, 10);
            }
            String code = newCode();
            Instant issued = Instant.now(); // another time on every run
            String sentTo = tasks.sendCoupon(customer, code);
            return new Coupon(code, percent, issued, sentTo);
        }

        // found here too: the check follows the helpers the workflow calls
        private static String newCode() {
            return UUID.randomUUID().toString();
        }
    }

    /** The same workflow with every such value recorded: it takes the same path on every run. */
    public static class RecordedCoupon {

        private final Tasks tasks;

        public RecordedCoupon(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "recorded_coupon")
        public Coupon issue(String customer) {
            boolean lucky = random() < 0.5; // drawn once, the same number on every run after
            int percent;
            if (lucky) {
                percent = tasks.applyDiscount(customer, 20);
            } else {
                percent = tasks.applyDiscount(customer, 10);
            }
            String code = once("coupon_code", () -> UUID.randomUUID().toString());
            Instant issued = now();
            String sentTo = tasks.sendCoupon(customer, code);
            return new Coupon(code, percent, issued, sentTo);
        }
    }

    public static void main(String[] args) {
        // usually -Dconductor.workflow.all.strictDeterminism=true, or the same key in application.properties
        System.setProperty("conductor.workflow.all.strictDeterminism", "true");

        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            try {
                conductor.workflows(RandomCoupon.class).tasks(new Tasks()).start();
                System.out.println("not expected: random_coupon was hosted");
            } catch (NonDeterminismException refused) {
                System.out.println("random_coupon was refused:\n\n" + refused.getMessage() + "\n");
            }

            conductor.workflows(RecordedCoupon.class).tasks(new Tasks()).start();
            WorkflowHandle<Coupon> run = conductor.start(RecordedCoupon::issue, "ada");
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
