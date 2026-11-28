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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Query;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.awaitCondition;

/**
 * Talking to a running workflow.
 *
 * <pre>
 *   @Variable field          durable state: saved with the execution, shown as its variables,
 *                            restored on every re-run of the code
 *   @Signal("name") method   a message in, no reply:      run.signal(Messages::checkout)
 *   @Update("name") method   a message in, with a reply:  int n = run.update(Messages::add, item)
 *   @Query("name") method    read state, change nothing:  run.query(Messages::count)
 *   awaitCondition(...)      wait until a handler makes the condition true
 * </pre>
 *
 * Handlers run between the pauses of the workflow method, never at the same time as it, so they
 * read and change fields without locks.
 */
public class Messages {

    public static class Tasks {
        @WorkerTask("p_place_order")
        public String place(List<String> items) {
            return "order of " + items.size();
        }
    }

    private final Tasks tasks;

    public Messages(Tasks tasks) {
        this.tasks = tasks;
    }

    @Variable private List<String> items = new ArrayList<>();
    @Variable private boolean checkedOut;

    @Update("add")
    public int add(String item) {
        items.add(item);
        return items.size();
    }

    @Signal("checkout")
    public void checkout() {
        checkedOut = true;
    }

    @Query("count")
    public int count() {
        return items.size();
    }

    @Workflow(name = "p_messages")
    public String run(String customer) {
        // the cart fills through updates; the code wakes when the checkout signal arrives
        boolean done = awaitCondition(() -> checkedOut, Duration.ofMinutes(10));
        if (!done) {
            return customer + ": abandoned";
        }
        return customer + ": " + tasks.place(items);
    }
}
