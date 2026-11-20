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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.Workflow;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.fanOut;
import static org.conductoross.conductor.sdk.Conductor.join;

/**
 * Running things at the same time.
 *
 * <pre>
 *   async(() -> ...)            a lane: the block runs on its own, alongside the code after it, and
 *                               its result is a java.util.concurrent.Future. The block may call
 *                               anything (tasks, ifs, loops, child workflows, more lanes). A
 *                               FORK_JOIN in the diagram, one branch per lane.
 *   join(a, b)                  wait for lanes: a JOIN. It also waits for lanes they started, and
 *                               throws the first lane failure as the lane threw it.
 *   future.resultNow()          a joined lane's result.
 *   future.get()                the lane's result, waiting for it first: a JOIN too. As for any
 *                               Future, a lane failure comes wrapped in an ExecutionException.
 *   fanOut(name, items, f)      one lane per item, the count known only at runtime: a
 *                               FORK_JOIN_DYNAMIC. Waits for them all and returns every result,
 *                               in order, as forEach does for items one after another.
 *   fanOut(items, f)            the same, named after the call site.
 * </pre>
 *
 * A lane cannot be cancelled ({@code cancel} returns false) and {@code get(timeout, unit)} is not
 * supported: the wait is durable, on the server, not a thread blocking.
 */
public class Parallel {

    public static class Tasks {
        @WorkerTask("p_work")
        public String work(String label) {
            return label;
        }

        @WorkerTask("p_slow")
        public String slow(String label) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return label;
        }
    }

    private final Tasks tasks;

    public Parallel(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "p_parallel")
    public List<String> run(List<String> items) {
        List<String> out = new ArrayList<>();

        // two lanes, joined: the second does not wait for the first's slow task
        Future<String> left = async(() -> tasks.slow("left"));
        Future<String> right = async(() -> {
            String first = tasks.work("right-1");
            return tasks.work(first + "+right-2");
        });
        join(left, right);
        out.add(left.resultNow());
        out.add(right.resultNow());

        // a lane read with get() alone: get waits for it
        Future<String> lone = async(() -> tasks.work("lone"));
        try {
            out.add(lone.get());
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause(); // what the lane threw: a TaskFailure, say
        } catch (InterruptedException e) {
            throw new IllegalStateException(e); // a workflow's wait is not a thread's: never happens
        }

        // a lane per item, from a loop, joined together
        List<Future<String>> lanes = new ArrayList<>();
        for (String item : items) {
            lanes.add(async(() -> tasks.work("lane-" + item)));
        }
        join(lanes.toArray(Future[]::new));
        for (Future<String> lane : lanes) {
            out.add(lane.resultNow());
        }

        // fanOut: a dynamic fork, one branch per item
        out.addAll(fanOut("by_item", items, item -> tasks.work("fan-" + item)));

        // fanOut named after the call site
        out.addAll(fanOut(items, item -> tasks.work("fan2-" + item)));

        return out;
    }
}
