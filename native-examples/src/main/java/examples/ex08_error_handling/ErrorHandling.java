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
package examples.ex08_error_handling;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowFailedException;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * 08 Error handling: retries, failures you handle, and undoing what was done.
 *
 * <pre>
 *   @WorkerTask(retryCount = 2, ...)   the server retries a failing task; the workflow sees
 *                                      nothing until the retries are used up
 *   NonRetryableException              thrown in a task: it fails at once, without retries
 *   catch (TaskFailure e)              a task that failed for good throws at its call: catch it to
 *                                      handle it (getTaskReferenceName(), getStatus(), getReason())
 *   a stack of undo steps              a saga: when a later step fails, undo the earlier ones, last
 *                                      first
 *   throw new WorkflowFailedException  fail the workflow with this reason. Any exception that leaves
 *                                      the workflow method fails it, an uncaught TaskFailure too.
 * </pre>
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex08_error_handling
 * </pre>
 */
public class ErrorHandling {

    public record Trip(String id, int cents) {}

    public static class Tasks {

        private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

        /** Fails twice for each trip, then works: the retries absorb it. */
        @WorkerTask(value = "lookup_fares", retryCount = 2, retryDelaySeconds = 1)
        public String lookupFares(String tripId) {
            int attempt = attempts.merge(tripId, 1, Integer::sum);
            if (attempt < 3) {
                throw new IllegalStateException("fare service busy, attempt " + attempt);
            }
            return "fares found on attempt " + attempt;
        }

        @WorkerTask(value = "book_flight", retryCount = 0)
        public String bookFlight(String tripId) {
            return "flight booked";
        }

        @WorkerTask(value = "book_hotel", retryCount = 0)
        public String bookHotel(String tripId) {
            return "hotel booked";
        }

        @WorkerTask(value = "cancel_flight", retryCount = 2)
        public String cancelFlight(String tripId) {
            return "flight cancelled";
        }

        @WorkerTask(value = "cancel_hotel", retryCount = 2)
        public String cancelHotel(String tripId) {
            return "hotel cancelled";
        }

        /** A declined card is final: retrying will not help. */
        @WorkerTask(value = "charge_card", retryCount = 3)
        public String chargeCard(String tripId, int cents) {
            if (cents > 100_000) {
                throw new NonRetryableException("card declined for " + cents + " cents");
            }
            return "charged " + cents + " cents";
        }
    }

    private final Tasks tasks;

    public ErrorHandling(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "book_trip")
    public List<String> book(Trip trip) {
        List<String> log = new ArrayList<>();

        // retried by the server until it works
        log.add(tasks.lookupFares(trip.id()));

        // a saga: each booking that works pushes its undo step
        Deque<Runnable> undo = new ArrayDeque<>();
        try {
            log.add(tasks.bookFlight(trip.id()));
            undo.push(() -> log.add(tasks.cancelFlight(trip.id())));
            log.add(tasks.bookHotel(trip.id()));
            undo.push(() -> log.add(tasks.cancelHotel(trip.id())));
            log.add(tasks.chargeCard(trip.id(), trip.cents()));
        } catch (TaskFailure e) {
            log.add(e.getTaskReferenceName() + " " + e.getStatus() + ": " + e.getReason());
            while (!undo.isEmpty()) {
                undo.pop().run();
            }
            // fails the workflow, with this as its reason
            throw new WorkflowFailedException("trip " + trip.id() + " not booked: " + String.join("; ", log));
        }
        return log;
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(ErrorHandling.class).tasks(new Tasks()).start();
            String stamp = String.valueOf(System.currentTimeMillis());

            WorkflowHandle<List<String>> booked = conductor.start(ErrorHandling::book, new Trip("ok-" + stamp, 45_000));
            System.out.println("execution: " + booked.workflowId());
            System.out.println("result:    " + booked.result(Duration.ofSeconds(30)));

            WorkflowHandle<List<String>> declined = conductor.start(ErrorHandling::book, new Trip("big-" + stamp, 250_000));
            System.out.println("execution: " + declined.workflowId());
            try {
                declined.result(Duration.ofSeconds(30));
            } catch (WorkflowFailedException e) {
                System.out.println("failed:    " + e.getMessage());
            }
        }
        client.shutdown();
    }
}
