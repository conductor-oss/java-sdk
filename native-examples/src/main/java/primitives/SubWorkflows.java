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

import java.util.Map;

import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.child;
import static org.conductoross.conductor.sdk.Conductor.startWorkflow;

/**
 * Workflows calling workflows.
 *
 * <pre>
 *   child(Child::run, input)                 a SUB_WORKFLOW: start the child, wait for it, get its
 *                                            result. It is a full execution of its own.
 *   async(() -> child(...))                  children side by side: a child in each lane.
 *   startWorkflow(Child::run, input)         START_WORKFLOW: start it and carry on, no waiting.
 *   startWorkflow(name, version, input)      the same by name: any registered workflow, written in
 *                                            code or JSON.
 * </pre>
 */
public class SubWorkflows {

    public static class Tasks {
        @WorkerTask("p_double")
        public int twice(int n) {
            return n * 2;
        }
    }

    /** The child's input: a record, so starting it by name takes the same fields as a map. */
    public record Doubling(int n) {}

    /** The child: an ordinary workflow, here in the same file. */
    public static class Child {
        private final Tasks tasks;

        public Child(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "p_child")
        public int run(Doubling input) {
            return tasks.twice(input.n());
        }
    }

    @Workflow(name = "p_subworkflows")
    public String run(int n) {
        // wait for a child's result
        int doubled = child(Child::run, new Doubling(n));

        // two children at once
        Promise<Integer> a = async(() -> child(Child::run, new Doubling(doubled)));
        Promise<Integer> b = async(() -> child(Child::run, new Doubling(doubled + 1)));

        // fire and forget, typed and by name
        startWorkflow(Child::run, new Doubling(100));
        startWorkflow("p_child", 1, Map.of("n", 200));

        return doubled + "/" + a.get() + "/" + b.get();
    }
}
