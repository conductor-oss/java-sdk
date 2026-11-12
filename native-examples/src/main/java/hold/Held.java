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
package hold;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.sleep;

/**
 * A bad deploy and its rollback: the execution is started on good code, a build with incompatible
 * code holds it without failing it, and the good build continues it.
 * Run {@code start}, then {@code bad <id>}, then {@code good <id>}.
 */
public class Held {

    // Conductor passes a stand-in whose calls schedule the tasks; the real Tasks runs on the workers.
    private final Tasks tasks;

    public Held(Tasks tasks) {
        this.tasks = tasks;
    }

    static final boolean BAD = "bad".equals(System.getProperty("build"));

    public static class Tasks {
        @WorkerTask("hold_first")
        public String first(@InputParam("x") String x) {
            return "first:" + x;
        }

        @WorkerTask("hold_other")
        public String other(@InputParam("x") String x) {
            return "other:" + x;
        }
    }

    @Workflow(name = "held_on_divergence")
    public String run(String x) {
        String a = BAD ? tasks.other(x) : tasks.first(x); // the incompatible change
        sleep(Duration.ofSeconds(1));
        return tasks.first(a);
    }

    @SuppressWarnings("unchecked")
    private static void report(Conductor conductor, String id) {
        Map<String, Object> wf = conductor.client().execute(
                com.netflix.conductor.client.http.ConductorClientRequest.builder()
                        .method(com.netflix.conductor.client.http.ConductorClientRequest.Method.GET)
                        .path("/workflow/" + id).addQueryParam("includeTasks", true).build(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}).getData();
        System.out.println("status " + wf.get("status") + " output " + wf.get("output"));
        for (Map<String, Object> t : (List<Map<String, Object>>) wf.get("tasks")) {
            Map<String, Object> c = (Map<String, Object>) t.get("continuation");
            System.out.println("  " + t.get("referenceTaskName") + " " + t.get("status") + " continuation "
                    + c.get("state") + " attempts " + c.get("attempts") + " deferred " + c.get("deferred"));
        }
    }

    @SuppressWarnings("unchecked")
    private static void awaitFirstTask(Conductor conductor, String id) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            Map<String, Object> wf = conductor.client().execute(
                    com.netflix.conductor.client.http.ConductorClientRequest.builder()
                            .method(com.netflix.conductor.client.http.ConductorClientRequest.Method.GET)
                            .path("/workflow/" + id).addQueryParam("includeTasks", true).build(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}).getData();
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) wf.get("tasks");
            if (!tasks.isEmpty() && tasks.get(0).get("continuation") instanceof Map<?, ?> c
                    && "APPLIED".equals(c.get("state"))) {
                return; // leaving earlier would strand a lease for the next build to wait out
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("the first continuation was not applied");
    }

    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host().workflows(Held.class).tasks(new Tasks()).start();
        if ("start".equals(args[0])) {
            WorkflowHandle<String> run = conductor.start(Held::run, "x");
            System.out.println("execution: " + run.workflowId());
            awaitFirstTask(conductor, run.workflowId()); // the sleep and its continuation are for later builds
        } else if ("bad".equals(args[0])) {
            Thread.sleep(20_000);
            report(conductor, args[1]);
        } else {
            System.out.println("result: " + conductor.attach(Held::run, args[1]).result(Duration.ofSeconds(60)));
            report(conductor, args[1]);
        }
        conductor.close();
        client.shutdown();
        System.exit(0);
    }
}
