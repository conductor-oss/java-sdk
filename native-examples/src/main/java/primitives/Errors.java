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
package primitives;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowFailedException;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * Failures.
 *
 * <pre>
 *   @WorkerTask(retryCount = 2, ...)   the server retries a failing task; the code sees nothing
 *                                      until the retries are used up
 *   NonRetryableException              thrown in a task: fail now, no retries
 *   catch (TaskFailure e)              a task that failed for good throws at its call; catch it to
 *                                      compensate or carry on (e.getTaskReferenceName(), getStatus())
 *   a stack of compensations           a saga: undo what succeeded, last first
 *   throw new WorkflowFailedException  fail the run with this reason. Any exception that leaves the
 *                                      workflow method fails it, an uncaught TaskFailure included;
 *                                      the diagram marks where it was thrown.
 * </pre>
 */
public class Errors {

    public static class Tasks {
        private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

        /** Fails twice for each key, then succeeds: the retries absorb it. */
        @WorkerTask(value = "p_flaky", retryCount = 2, retryDelaySeconds = 1)
        public String flaky(String key) {
            int attempt = attempts.merge(key, 1, Integer::sum);
            if (attempt < 3) {
                throw new IllegalStateException("not yet: attempt " + attempt);
            }
            return "ok after " + attempt;
        }

        @WorkerTask(value = "p_reserve", retryCount = 0)
        public String reserve(String what) {
            return "reserved " + what;
        }

        @WorkerTask(value = "p_release", retryCount = 0)
        public String release(String what) {
            return "released " + what;
        }

        /** Rejects for good: no retries even though the definition would allow them. */
        @WorkerTask(value = "p_pay", retryCount = 3)
        public String pay(int amount) {
            if (amount > 100) {
                throw new NonRetryableException("card declined for " + amount);
            }
            return "paid " + amount;
        }
    }

    private final Tasks tasks;

    public Errors(Tasks tasks) {
        this.tasks = tasks;
    }

    public record Purchase(String key, int amount, boolean failIfDeclined) {}

    @Workflow(name = "p_errors")
    public List<String> run(Purchase purchase) {
        List<String> log = new ArrayList<>();

        // retried by the server until it succeeds
        log.add(tasks.flaky(purchase.key()));

        // a saga: each step that succeeds pushes its undo
        Deque<Runnable> undo = new ArrayDeque<>();
        try {
            log.add(tasks.reserve("seat"));
            undo.push(() -> log.add(tasks.release("seat")));
            log.add(tasks.reserve("meal"));
            undo.push(() -> log.add(tasks.release("meal")));
            log.add(tasks.pay(purchase.amount()));
        } catch (TaskFailure e) {
            log.add(e.getTaskReferenceName() + " " + e.getStatus());
            while (!undo.isEmpty()) {
                undo.pop().run();
            }
            if (purchase.failIfDeclined()) {
                // fails the run, with this as its reason
                throw new WorkflowFailedException("declined: " + String.join(", ", log));
            }
        }
        return log;
    }
}
