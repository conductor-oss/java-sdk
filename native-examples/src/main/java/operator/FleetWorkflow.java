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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

/**
 * A rolling patch across a fleet, written as plain Java: a loop over regions with a switch on the
 * region's tier, a loop over its hosts nested inside, an if / else if / else on each host's health
 * (one branch continues to the next host), a while loop whose condition calls a task, a switch on
 * an enum inside the nested loop, an if / else after it, and a do-while at the end.
 *
 * <p>The tasks answer by host name, so the path the run takes is known: us is critical (web-1
 * healthy, db-1 degraded and verified on the second attempt, cache-1 healthy), eu is standard
 * (web-2 healthy, cache-2 down), ap is an edge region with no gate (web-3 healthy). Five hosts are
 * patched, one is paged, eu rolls back, and the report is acknowledged on the second round.
 */
public class FleetWorkflow {

    // Conductor passes a stand-in whose calls schedule the tasks; the real FleetTasks runs on the workers.
    private final FleetTasks tasks;

    public FleetWorkflow(FleetTasks tasks) {
        this.tasks = tasks;
    }

    public enum Role { WEB, DB, CACHE }

    public record Host(String name, Role role) {}

    public record Region(String name, String tier, List<Host> hosts) {}

    public record Health(String state, int load) {}

    public static final String EXPECTED = "5 patched/1 paged/promote us,rollback eu,promote ap/ack round 2";

    @Workflow(name = "native_fleet", version = 1)
    public String run(String id) {
        List<Region> regions = tasks.inventory(id);
        List<String> verdicts = new ArrayList<>();
        int patched = 0;
        int paged = 0;

        for (Region region : regions) {
            // the change gate depends on the region's tier
            switch (region.tier()) {
                case "critical" -> tasks.approve(id, region.name());
                case "standard" -> tasks.announce(id, region.name());
                default -> { } // an edge region has no gate
            }

            int down = 0;
            for (Host host : region.hosts()) {
                Health health = tasks.probe(id, host.name());
                if (health.state().equals("healthy")) {
                    tasks.patch(id, host.name());
                } else if (health.state().equals("degraded")) {
                    tasks.drain(id, host.name());
                    tasks.patch(id, host.name());
                } else {
                    tasks.page(id, host.name());
                    down++;
                    continue;
                }

                // a patch can take a moment to show: verify until it does
                int attempt = 1;
                while (!tasks.verify(id, host.name(), attempt)) {
                    attempt++;
                }
                patched++;

                switch (host.role()) {
                    case DB -> tasks.backup(id, host.name());
                    case CACHE -> tasks.warm(id, host.name());
                    case WEB -> { }
                }
            }
            paged += down;

            if (down == 0) {
                verdicts.add(tasks.promote(id, region.name()));
            } else {
                verdicts.add(tasks.rollback(id, region.name(), down));
            }
        }

        String report;
        int round = 0;
        do {
            round++;
            report = tasks.report(id, patched, paged, round);
        } while (!report.startsWith("ack"));

        return patched + " patched/" + paged + " paged/" + String.join(",", verdicts) + "/" + report;
    }

    /** Runs the fleet once and prints the result. */
    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(FleetWorkflow.class).tasks(new FleetTasks()).start();
        WorkflowHandle<String> run = conductor.start(FleetWorkflow::run, java.util.UUID.randomUUID().toString());
        System.out.println("execution: " + run.url());
        System.out.println("result:    " + run.result(Duration.ofMinutes(2)));
        conductor.close();
        client.shutdown();
        System.exit(0);
    }
}
