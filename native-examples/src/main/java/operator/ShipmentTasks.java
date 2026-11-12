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
package operator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

public class ShipmentTasks {

    /** While true, the carrier rejects labels. */
    public static final AtomicBoolean carrierDown = new AtomicBoolean(true);

    public static final AtomicInteger packCalls = new AtomicInteger();

    /** The packing station drops the first request for each order; retries recover it. */
    @WorkerTask("shipment_pack")
    public int pack(String orderId, int weight) {
        if (packCalls.incrementAndGet() % 2 == 1) {
            throw new IllegalStateException("packing station busy");
        }
        return Math.max(1, weight / 10);
    }

    /** Executions the failure workflow was started for. */
    public static final java.util.List<String> failedShipments = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Runs of the scheduled workflow. */
    public static final AtomicInteger pings = new AtomicInteger();

    @WorkerTask("shipment_record_failure")
    public String recordFailure(String workflowId, String reason) {
        failedShipments.add(workflowId);
        return "recorded failure of " + workflowId + ": " + reason;
    }

    private static final java.util.Map<String, AtomicInteger> routeCalls = new java.util.concurrent.ConcurrentHashMap<>();

    /** "x" the first time for an id, "y" after that. */
    @WorkerTask("route_decide")
    public String routeDecide(String id) {
        return routeCalls.computeIfAbsent(id, k -> new AtomicInteger()).incrementAndGet() == 1 ? "x" : "y";
    }

    @WorkerTask("route_x")
    public String routeX(String id) {
        return "ran-x";
    }

    @WorkerTask("route_y")
    public String routeY(String id) {
        return "ran-y";
    }

    @WorkerTask("route_after")
    public String routeAfter(String ran) {
        return "after-" + ran;
    }

    /** Runs of each task of the forked workflow, by task name and execution input. */
    public static final java.util.Map<String, AtomicInteger> forkRuns = new java.util.concurrent.ConcurrentHashMap<>();

    private static int ranFork(String task, String id) {
        return forkRuns.computeIfAbsent(task + "/" + id, k -> new AtomicInteger()).incrementAndGet();
    }

    @WorkerTask("fork_left")
    public String forkLeft(String id) {
        ranFork("left", id);
        return "L";
    }

    @WorkerTask("fork_right")
    public String forkRight(String id) {
        ranFork("right", id);
        return "R";
    }

    @WorkerTask("lane_slow")
    public String laneSlow(String id) {
        try {
            Thread.sleep(2000); // longer than the other lane, so a join that did not wait would show
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "slow";
    }

    @WorkerTask("fork_after")
    public String forkAfter(String id, String both) {
        ranFork("after", id);
        return both + " joined";
    }

    /** Runs of each task of the looped workflow, by task and item or execution input. */
    public static final java.util.Map<String, AtomicInteger> loopRuns = new java.util.concurrent.ConcurrentHashMap<>();

    private static int ranLoop(String key) {
        return loopRuns.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    @WorkerTask("loop_item")
    public String loopItem(String item) {
        ranLoop("item/" + item);
        return item.toUpperCase();
    }

    /** Done on every third check of an execution. */
    @WorkerTask("loop_poll")
    public LoopedWorkflow.Poll loopPoll(String id) {
        int check = ranLoop("poll/" + id);
        return new LoopedWorkflow.Poll(check % 3 == 0, check);
    }

    @WorkerTask("loop_after")
    public String loopAfter(String id) {
        ranLoop("after/" + id);
        return "after";
    }

    @WorkerTask("shipment_ping")
    public String ping(String source) {
        return "ping " + pings.incrementAndGet() + " from " + source;
    }

    @WorkerTask(value = "shipment_label", retryCount = 0)
    public String label(String trackingId, int boxes) {
        if (carrierDown.get()) {
            throw new NonRetryableException("carrier unavailable");
        }
        return "LABEL-" + trackingId + "-x" + boxes;
    }
}
