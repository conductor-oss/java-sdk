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
package switches;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.switchOn;

/**
 * Switch edge cases, each visible in the execution: a taken case with tasks, an empty case, the
 * default case, a nested switch, and a value no case handles.
 */
public class Switches {

    // Conductor passes a stand-in whose calls schedule the tasks; the real Tasks runs on the workers.
    private final Tasks tasks;

    public Switches(Tasks tasks) {
        this.tasks = tasks;
    }

    public static class Tasks {
        @WorkerTask("ship_fast")
        public String fast(String item) {
            return "fast:" + item;
        }

        @WorkerTask("ship_slow")
        public String slow(String item) {
            return "slow:" + item;
        }

        @WorkerTask("hold")
        public String hold(String item) {
            return "held:" + item;
        }

        @WorkerTask("notify")
        public String send(String text) {
            return "sent " + text;
        }
    }

    public record Input(String route, String region, String item) {}

    @Workflow(name = "switches")
    public List<String> ship(Input input) {
        List<String> log = new ArrayList<>();
        // route: a case with tasks, an empty case (nothing to do), and a default
        switchOn("route", input.route())
                .when("fast", () -> {
                    // a nested switch inside a case: region picks the carrier step
                    String result = switchOn("region", input.region())
                            .when("eu", () -> tasks.fast("eu-" + input.item()))
                            .when("us", () -> tasks.fast("us-" + input.item()))
                            .otherwise(() -> tasks.hold(input.item()))
                            .get();
                    log.add(result);
                })
                .when("slow", () -> log.add(tasks.slow(input.item())))
                .when("skip", () -> log.add("skipped")) // an empty case: no task call
                .otherwise(() -> log.add(tasks.hold(input.item())))
                .run();
        // after the switch: lands after it, whichever case ran
        log.add(tasks.send(String.join(",", log)));
        // a switch without otherwise: an unhandled value fails the workflow
        switchOn("final_check", input.item().startsWith("bad") ? "unknown" : "ok")
                .when("ok", () -> log.add("checked"))
                .run();
        return log;
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Switches.class).tasks(new Tasks()).start();
        List<Input> inputs = List.of(
                new Input("fast", "eu", "box-1"),      // case fast -> nested eu
                new Input("fast", "apac", "box-2"),    // case fast -> nested default
                new Input("skip", "eu", "box-3"),      // empty case
                new Input("express", "eu", "box-4"),   // default case
                new Input("slow", "eu", "bad-box"));   // slow, then an unhandled final_check -> FAILED
        for (Input in : inputs) {
            WorkflowHandle<List<String>> run = conductor.start(Switches::ship, in);
            String outcome;
            try {
                outcome = String.valueOf(run.result(Duration.ofSeconds(60)));
            } catch (RuntimeException e) {
                outcome = "FAILED: " + e.getMessage();
            }
            System.out.println(in + " -> " + run.workflowId() + " -> " + outcome);
        }
        conductor.close();
        client.shutdown();
    }
}
