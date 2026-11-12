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
package durability;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Query;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;

import durability.LedgerModel.LedgerInput;
import durability.LedgerModel.LedgerResult;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.awaitCondition;
import static org.conductoross.conductor.sdk.Conductor.child;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.random;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.workflowId;

/**
 * The durability workload. Each execution exercises the parts of the engine a failure can break:
 *
 * <ul>
 *   <li>recorded values ({@code once}, {@code random}, {@code now}) that every replay must see unchanged;
 *   <li>a task, then two async lanes (one with a timer) joined, then a child workflow, then a task;
 *   <li>signals ({@code deposit}) and updates ({@code bump}) arriving at any time, including while
 *       the worker or server is down;
 *   <li>a condition wait released by the {@code close} signal, which the bench sends after the faults stop.
 * </ul>
 *
 * Every value the code observes is written to the {@link Ledger} on every replay.
 */
public class LedgerWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real LedgerTasks runs on the workers.
    private final LedgerTasks tasks;

    public LedgerWorkflow(LedgerTasks tasks) {
        this.tasks = tasks;
    }

    static final int STEPS = 6;

    @Variable private List<String> deposits = new ArrayList<>();
    @Variable private int bumps;
    @Variable private boolean closed;

    @Signal("deposit")
    public void deposit(String depositId) {
        deposits.add(depositId);
        Ledger.handled(depositId, deposits.size());
    }

    @Signal("close")
    public void close() {
        closed = true;
    }

    /** The marker the query below tries to write; it must never reach the execution. */
    static final String QUERY_MARKER = "written-by-a-query";

    /**
     * The deposits applied so far. It also tries to change state, which a query must not be able
     * to do: the checker fails the run if the marker ever shows up in the output.
     */
    @Query("deposits")
    public List<String> depositsSoFar() {
        List<String> answer = List.copyOf(deposits);
        deposits.add(QUERY_MARKER);
        return answer;
    }

    @Update("bump")
    public int bump(String updateId) {
        bumps++;
        return bumps;
    }

    @Workflow(name = "durability_ledger", version = 1, ownerEmail = "bench@example.com", timeout = "PT2H")
    public LedgerResult run(LedgerInput input) {
        String id = input.id();
        String token = once("token", () -> UUID.randomUUID().toString());
        double r = random();
        long startedAt = now().toEpochMilli();
        String wf = workflowId();
        Ledger.observe(wf, "token", token);
        Ledger.observe(wf, "random", r);
        Ledger.observe(wf, "startedAt", startedAt);

        int first = tasks.step(id, 1);
        Promise<Integer> left = async(() -> tasks.step(id, 2) + tasks.step(id, 3));
        Promise<Integer> right = async(() -> {
            sleep(Duration.ofSeconds(input.sleepSeconds()));
            return tasks.step(id, 4);
        });
        join(left, right);
        int fromChild = child(LedgerChild::run, id);
        Ledger.observe(wf, "beforeClose", first + left.get() + right.get() + fromChild);

        // long enough that only the close signal releases it, even with the server's time scale
        awaitCondition(() -> closed, Duration.ofDays(2));
        int last = tasks.step(id, 6);
        int total = first + left.get() + right.get() + fromChild + last;
        return new LedgerResult(id, token, r, startedAt, total, List.copyOf(deposits), bumps);
    }
}
