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

import java.time.Instant;
import java.util.UUID;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.random;
import static org.conductoross.conductor.sdk.Conductor.workflowId;

/**
 * Values that differ from one run of the code to the next. The code re-runs from the top each
 * time it wakes, so anything that is not the same every time must be recorded the first time:
 *
 * <pre>
 *   once(name, () -> ...)   run this Java here, once; later re-runs get the recorded value. For ids,
 *                           config reads, small calls that do not deserve a task.
 *   now()                   the time (not Instant.now())
 *   random()                a number in [0, 1) (not Math.random())
 *   workflowId()            this execution's id
 * </pre>
 *
 * None of these is a step in the diagram: once() values are recorded on the execution.
 */
public class SideEffects {

    public static class Tasks {
        @WorkerTask("p_echo")
        public String echo(String text) {
            return text;
        }
    }

    private final Tasks tasks;

    public SideEffects(Tasks tasks) {
        this.tasks = tasks;
    }

    public record Seen(String id, String id2, double roll, double roll2, Instant at, Instant at2, String workflowId) {}

    @Workflow(name = "p_side_effects")
    public Seen run(String prefix) {
        String id = once("order_id", () -> prefix + "-" + UUID.randomUUID());
        double roll = random();
        Instant at = now();

        // the code wakes again after this task; the values above must come back unchanged
        String echoed = tasks.echo(id);

        return new Seen(id, echoed, roll, random(), at, now(), workflowId());
    }
}
