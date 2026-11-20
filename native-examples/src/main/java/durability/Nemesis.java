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
import java.util.List;

import durability.Processes.Dice;
import durability.Processes.Workers.Slot;

/**
 * Faults, injected at random (seeded) intervals: kills and pauses of workers and servers,
 * partitions between workers and the server, and, in a cluster, database faults. Shared by the
 * durability bench and the kitchensink chaos run.
 */
final class Nemesis {

    /** Where each fault's start and stop is recorded. */
    interface Events {
        void nemesis(String type, String fault, String target);
    }

    private final List<String> faults;
    private final double faultInterval;
    private final Dice dice;
    private final Processes.Workers workers;
    private final List<Processes.Server> servers;
    private final Proxy proxy;
    private final List<Proxy> dbProxies;
    private final Processes.Database database;
    private final Events events;

    Nemesis(List<String> faults, double faultInterval, Dice dice, Processes.Workers workers, List<Processes.Server> servers,
            Proxy proxy, List<Proxy> dbProxies, Processes.Database database, Events events) {
        this.faults = faults;
        this.faultInterval = faultInterval;
        this.dice = dice;
        this.workers = workers;
        this.servers = servers;
        this.proxy = proxy;
        this.dbProxies = dbProxies;
        this.database = database;
        this.events = events;
    }


    /** Injects a fault at random intervals until 10 s before {@code until}. */
    void run(long until) {
        if (faults.isEmpty()) {
            return;
        }
        Processes.sleepMillis(5000);
        while (true) {
            Processes.sleepMillis((long) (dice.exponential(faultInterval) * 1000));
            if (System.currentTimeMillis() > until - 10_000) {
                return;
            }
            String fault = dice.pick(faults);
            try {
                inject(fault);
            } catch (Exception e) {
                Bench.log("nemesis " + fault + " failed: " + e);
                events.nemesis("error", fault, String.valueOf(e));
            }
        }
    }

    private void inject(String fault) throws Exception {
        // Each fault holds its target, so overlapping nemeses never act on one process at once.
        switch (fault) {
            case "worker-kill", "worker-pause" -> {
                Slot slot = dice.pick(workers.slots());
                synchronized (slot) {
                    injectOn(fault, slot, null);
                }
            }
            case "server-kill", "server-pause" -> {
                Processes.Server server = dice.pick(servers);
                synchronized (server) {
                    injectOn(fault, null, server);
                }
            }
            case "cluster-kill" -> lockAll(0, () -> injectOn(fault, null, null));
            case "db-pause", "db-kill" -> {
                requireCluster(fault);
                synchronized (database) {
                    injectOn(fault, null, null);
                }
            }
            default -> injectOn(fault, null, null);
        }
    }

    private interface Action {
        void run() throws Exception;
    }

    /** Holds every server in list order (the same order as single-server faults never conflict). */
    private void lockAll(int i, Action action) throws Exception {
        if (i == servers.size()) {
            action.run();
            return;
        }
        synchronized (servers.get(i)) {
            lockAll(i + 1, action);
        }
    }

    private void injectOn(String fault, Slot slot, Processes.Server server) throws Exception {
        switch (fault) {
            case "worker-kill" -> {
                events.nemesis("start", fault, slot.workerId);
                Bench.log("nemesis: kill -9 worker " + slot.workerId);
                workers.kill(slot);
                Processes.sleepMillis(dice.between(1, 20) * 1000L);
                // half the time a new host replaces it, half the time the same host comes back
                String id = dice.chance(0.5) ? workers.freshId(slot) : slot.workerId;
                workers.start(slot, id);
                events.nemesis("stop", fault, id);
                Bench.log("nemesis: worker restarted as " + id);
            }
            case "worker-pause" -> {
                int seconds = dice.between(5, 45);
                events.nemesis("start", fault, slot.workerId);
                Bench.log("nemesis: SIGSTOP worker " + slot.workerId + " for " + seconds + "s");
                workers.pause(slot);
                Processes.sleepMillis(seconds * 1000L);
                workers.resume(slot);
                events.nemesis("stop", fault, slot.workerId);
            }
            case "server-kill" -> {
                events.nemesis("start", fault, "server-" + server.port);
                Bench.log("nemesis: kill -9 server " + server.port);
                server.kill();
                Processes.sleepMillis(dice.between(1, 10) * 1000L);
                server.start();
                server.ensureUp(Duration.ofMinutes(4));
                events.nemesis("stop", fault, "server-" + server.port);
                Bench.log("nemesis: server " + server.port + " back");
            }
            case "server-pause" -> {
                int seconds = dice.between(5, 25);
                events.nemesis("start", fault, "server-" + server.port);
                Bench.log("nemesis: SIGSTOP server " + server.port + " for " + seconds + "s");
                server.pause();
                Processes.sleepMillis(seconds * 1000L);
                server.resume();
                events.nemesis("stop", fault, "server-" + server.port);
            }
            case "cluster-kill" -> {
                events.nemesis("start", fault, "all servers");
                Bench.log("nemesis: kill -9 every server");
                for (Processes.Server each : servers) {
                    each.kill();
                }
                Processes.sleepMillis(dice.between(1, 10) * 1000L);
                for (Processes.Server each : servers) {
                    each.start();
                    each.ensureUp(Duration.ofMinutes(4));
                }
                events.nemesis("stop", fault, "all servers");
                Bench.log("nemesis: cluster back");
            }
            case "db-pause" -> {
                requireCluster(fault);
                int seconds = dice.between(5, 25);
                events.nemesis("start", fault, "database");
                Bench.log("nemesis: pause the database for " + seconds + "s");
                database.pause();
                Processes.sleepMillis(seconds * 1000L);
                database.resume();
                events.nemesis("stop", fault, "database");
            }
            case "db-kill" -> {
                requireCluster(fault);
                events.nemesis("start", fault, "database");
                Bench.log("nemesis: kill -9 the database");
                database.kill();
                Processes.sleepMillis(dice.between(1, 10) * 1000L);
                database.ensureUp(Duration.ofMinutes(2));
                events.nemesis("stop", fault, "database");
                Bench.log("nemesis: database back");
            }
            case "db-partition" -> {
                requireCluster(fault);
                int i = dice.between(0, servers.size() - 1);
                int seconds = dice.between(5, 30);
                events.nemesis("start", fault, "server-" + servers.get(i).port + "|database");
                Bench.log("nemesis: partition server " + servers.get(i).port + " from the database for " + seconds + "s");
                dbProxies.get(i).partition();
                Processes.sleepMillis(seconds * 1000L);
                dbProxies.get(i).heal();
                events.nemesis("stop", fault, "server-" + servers.get(i).port + "|database");
            }
            case "partition" -> {
                int seconds = dice.between(5, 40);
                events.nemesis("start", fault, "workers|server");
                Bench.log("nemesis: partition workers from server for " + seconds + "s");
                proxy.partition();
                Processes.sleepMillis(seconds * 1000L);
                proxy.heal();
                events.nemesis("stop", fault, "workers|server");
            }
            default -> throw new IllegalArgumentException("unknown fault " + fault);
        }
    }

    private void requireCluster(String fault) {
        if (database == null) {
            throw new IllegalStateException(fault + " needs BENCH_CLUSTER");
        }
    }

    /** Ends every fault: heals partitions, resumes paused processes, restarts dead ones. */
    void heal() throws Exception {
        proxy.heal();
        for (Proxy p : dbProxies) {
            p.heal();
        }
        if (database != null) {
            database.ensureUp(Duration.ofMinutes(2));
        }
        for (Processes.Server server : servers) {
            server.ensureUp(Duration.ofMinutes(4));
        }
        workers.ensureAll();
    }
}
