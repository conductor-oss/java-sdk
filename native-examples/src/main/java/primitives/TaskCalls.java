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

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.task;

/**
 * Calling tasks. A task is a {@code @WorkerTask} method on an ordinary class; a worker anywhere
 * runs it. The workflow takes the class as a constructor parameter and calls the methods: what
 * it gets is a stand-in whose calls schedule the task, wait for it and return its output.
 *
 * <pre>
 *   tasks.quote(item, qty)                     by stub: typed arguments and result, the usual way
 *   tasks.audit(text)                          a void task: scheduled and waited for all the same
 *   tasks.quote(...) twice                     each call is its own task: quote, then quote_2
 *   task("name").input(k, v).as(T.class)       by name: a task with no Java method here (another
 *                                              language, another team), or a name known at runtime
 *   task("name").ref("my_ref")                 a reference name of your choosing
 *   task("name").input(map).asMap()            untyped: the output as a map
 *   task("name").input(...).run()              by name, ignoring the output
 * </pre>
 *
 * Arguments go to the task as input keyed by parameter name (compile with {@code -parameters}),
 * records and lists included; the return value comes back as the task's output.
 */
public class TaskCalls {

    public record Quote(String item, int qty, int cents) {}

    public static class Tasks {
        @WorkerTask("p_quote")
        public Quote quote(String item, int qty) {
            return new Quote(item, qty, qty * 250);
        }

        @WorkerTask("p_total")
        public int total(List<Quote> quotes) {
            return quotes.stream().mapToInt(Quote::cents).sum();
        }

        @WorkerTask("p_audit")
        public void audit(String text) {
            System.out.println("audit: " + text);
        }

        /** Stands for a task some other worker serves: the workflow calls it by name only. */
        @WorkerTask("p_tax")
        public Map<String, Object> tax(int cents, String region) {
            return Map.of("tax", cents / 10, "region", region);
        }
    }

    private final Tasks tasks;

    public TaskCalls(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "p_tasks")
    public String run(String region) {
        // by stub: typed in, typed out
        Quote apples = tasks.quote("apple", 2);
        Quote pears = tasks.quote("pear", 3);                         // the same task again: ref p_quote_2
        int total = tasks.total(List.of(apples, pears));

        // a void task
        tasks.audit("quoted " + total);

        // by name, typed result
        int tax = task("p_tax").input("cents", total).input("region", region).as(TaxOnly.class).tax();

        // by name, with a reference name and the output as a map
        Map<String, Object> again = task("p_tax").ref("tax_check").input(Map.of("cents", 100, "region", region)).asMap();

        // by name, output not needed
        task("p_audit").input("text", "tax " + tax).run();

        return total + "+" + tax + "/" + again.get("tax");
    }

    /** The part of p_tax's output this workflow reads; other fields are ignored. */
    public record TaxOnly(int tax) {}
}
