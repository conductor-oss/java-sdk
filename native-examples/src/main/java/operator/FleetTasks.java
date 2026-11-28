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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import operator.FleetWorkflow.Health;
import operator.FleetWorkflow.Host;
import operator.FleetWorkflow.Region;
import operator.FleetWorkflow.Role;

/** The fleet's tasks. Each counts its runs by execution, task and host, for the operator checks. */
public class FleetTasks {

    /** Runs by "id/task/host-or-region". */
    public static final Map<String, AtomicInteger> runs = new ConcurrentHashMap<>();

    /** Executions whose patch of this host fails until it is cleared: by "id/host". */
    public static final Map<String, Boolean> broken = new ConcurrentHashMap<>();

    private static int ran(String id, String task, String on) {
        return runs.computeIfAbsent(id + "/" + task + "/" + on, k -> new AtomicInteger()).incrementAndGet();
    }

    public static int runs(String id, String task, String on) {
        AtomicInteger n = runs.get(id + "/" + task + "/" + on);
        return n == null ? 0 : n.get();
    }

    @WorkerTask("fleet_inventory")
    public List<Region> inventory(String id) {
        ran(id, "inventory", "fleet");
        return List.of(
                new Region("us", "critical", List.of(
                        new Host("web-1", Role.WEB), new Host("db-1", Role.DB), new Host("cache-1", Role.CACHE))),
                new Region("eu", "standard", List.of(new Host("web-2", Role.WEB), new Host("cache-2", Role.CACHE))),
                new Region("ap", "edge", List.of(new Host("web-3", Role.WEB))));
    }

    @WorkerTask("fleet_approve")
    public String approve(String id, String region) {
        ran(id, "approve", region);
        return "approved " + region;
    }

    @WorkerTask("fleet_announce")
    public String announce(String id, String region) {
        ran(id, "announce", region);
        return "announced " + region;
    }

    /** db hosts are degraded, cache-2 is down, the rest are healthy. */
    @WorkerTask("fleet_probe")
    public Health probe(String id, String host) {
        ran(id, "probe", host);
        if (host.equals("cache-2")) {
            return new Health("down", 0);
        }
        return host.startsWith("db") ? new Health("degraded", 90) : new Health("healthy", 30);
    }

    @WorkerTask("fleet_drain")
    public String drain(String id, String host) {
        ran(id, "drain", host);
        return "drained " + host;
    }

    @WorkerTask(value = "fleet_patch", retryCount = 0)
    public String patch(String id, String host) {
        ran(id, "patch", host);
        if (broken.getOrDefault(id + "/" + host, false)) {
            throw new NonRetryableException("patch repository unreachable from " + host);
        }
        return "patched " + host;
    }

    @WorkerTask("fleet_page")
    public String page(String id, String host) {
        ran(id, "page", host);
        return "paged on-call for " + host;
    }

    /** A db host shows its patch on the second check; the others on the first. */
    @WorkerTask("fleet_verify")
    public boolean verify(String id, String host, int attempt) {
        ran(id, "verify", host);
        return !host.startsWith("db") || attempt >= 2;
    }

    @WorkerTask("fleet_backup")
    public String backup(String id, String host) {
        ran(id, "backup", host);
        return "backed up " + host;
    }

    @WorkerTask("fleet_warm")
    public String warm(String id, String host) {
        ran(id, "warm", host);
        return "warmed " + host;
    }

    @WorkerTask("fleet_promote")
    public String promote(String id, String region) {
        ran(id, "promote", region);
        return "promote " + region;
    }

    @WorkerTask("fleet_rollback")
    public String rollback(String id, String region, int down) {
        ran(id, "rollback", region);
        return "rollback " + region;
    }

    /** Acknowledged on the second round. */
    @WorkerTask("fleet_report")
    public String report(String id, int patched, int paged, int round) {
        ran(id, "report", "fleet");
        return round >= 2 ? "ack round " + round : "pending round " + round;
    }
}
