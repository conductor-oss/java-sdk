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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * How long a workflow of 10 sequential 1 ms worker tasks takes, one execution at a time: as a native
 * workflow, and as the same 10 tasks in a JSON definition, on the same worker in this process. For
 * each it prints, as p50/p90/max over the runs:
 *
 * <ul>
 *   <li>the server's time, from the execution's start to its end;
 *   <li>the caller's time, from {@code start} to the result in hand;
 *   <li>per task: how long it waited to be picked up, how long it ran and reported back, and how
 *       long from its completion to the next task scheduled (for native, the continuation's hop
 *       through the workflow's code).
 * </ul>
 *
 * Settings, from the environment: {@code LATENCY_SERVER_URL} (default {@code
 * http://localhost:8099/api}; never port 8080), {@code LATENCY_RUNS} (20), {@code LATENCY_WARMUP} (3)
 * and {@code LATENCY_MODE} ({@code both}, {@code native} or {@code json}). Polling settings are the
 * usual system properties, e.g. {@code -Dconductor.worker.all.pollingInterval=10}.
 */
public class Latency {

    public static class Tasks {

        @WorkerTask("lat_step")
        public int step(int i) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return i + 1;
        }
    }

    private final Tasks tasks;

    public Latency(Tasks tasks) {
        this.tasks = tasks;
    }

    @Workflow(name = "lat_ten", version = 1)
    public int ten(int start) {
        int x = start;
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        x = tasks.step(x);
        return x;
    }

    /** What one mode measured, in milliseconds. */
    record Sample(List<Long> server, List<Long> caller, List<Long> pickup, List<Long> run, List<Long> hop) {
        Sample() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault("LATENCY_SERVER_URL", "http://localhost:8099/api");
        if (URI.create(url).getPort() == 8080) {
            throw new IllegalArgumentException("refusing to use port 8080");
        }
        int runs = Integer.parseInt(System.getenv().getOrDefault("LATENCY_RUNS", "20"));
        int warmup = Integer.parseInt(System.getenv().getOrDefault("LATENCY_WARMUP", "3"));
        String mode = System.getenv().getOrDefault("LATENCY_MODE", "both");
        ConductorClient client = ConductorClient.builder().basePath(url).build();
        WorkflowClient workflows = new WorkflowClient(client);
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(Latency.class).tasks(new Tasks()).start();
            new MetadataClient(client).updateWorkflowDefs(List.of(jsonDefinition()));

            for (String m : mode.equals("both") ? List.of("native", "json") : List.of(mode)) {
                for (int i = 0; i < warmup; i++) {
                    run(conductor, workflows, m);
                }
                Sample sample = new Sample();
                for (int r = 0; r < runs; r++) {
                    long started = System.nanoTime();
                    String id = run(conductor, workflows, m);
                    sample.caller().add((System.nanoTime() - started) / 1_000_000);
                    measure(workflows.getWorkflow(id, true), sample);
                }
                System.out.printf("%-6s %d runs, 10 tasks of 1 ms each%n", m, runs);
                System.out.printf("  server, start to end          %s%n", stats(sample.server()));
                System.out.printf("  caller, start to result       %s%n", stats(sample.caller()));
                System.out.printf("  per task: waits for pickup    %s%n", stats(sample.pickup()));
                System.out.printf("  per task: runs and reports    %s%n", stats(sample.run()));
                System.out.printf("  per task: done to next one    %s%n", stats(sample.hop()));
            }
        }
        client.shutdown();
        System.exit(0);
    }

    private static WorkflowDef jsonDefinition() {
        WorkflowDef def = new WorkflowDef();
        def.setName("lat_ten_json");
        def.setVersion(1);
        def.setOwnerEmail("latency@example.com");
        List<WorkflowTask> steps = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            WorkflowTask step = new WorkflowTask();
            step.setName("lat_step");
            step.setTaskReferenceName("step_" + i);
            step.setInputParameters(Map.of("i", i));
            steps.add(step);
        }
        def.setTasks(steps);
        return def;
    }

    /** Runs one execution to its end and returns its id. */
    private static String run(Conductor conductor, WorkflowClient workflows, String mode) throws InterruptedException {
        if (mode.equals("native")) {
            WorkflowHandle<Integer> handle = conductor.start(Latency::ten, 0);
            handle.result(Duration.ofMinutes(2));
            return handle.workflowId();
        }
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("lat_ten_json");
        request.setVersion(1);
        request.setInput(Map.of());
        String id = workflows.startWorkflow(request);
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            if (workflows.getWorkflow(id, false).getStatus().isTerminal()) {
                return id;
            }
            Thread.sleep(5);
        }
        throw new IllegalStateException("execution " + id + " did not end in 2 minutes");
    }

    private static void measure(com.netflix.conductor.common.run.Workflow execution, Sample sample) {
        sample.server().add(execution.getEndTime() - execution.getStartTime());
        long previousEnd = execution.getStartTime();
        for (Task task : execution.getTasks()) {
            if (!"lat_step".equals(task.getTaskType())) {
                continue;
            }
            sample.pickup().add(task.getStartTime() - task.getScheduledTime());
            sample.run().add(task.getEndTime() - task.getStartTime());
            sample.hop().add(task.getScheduledTime() - previousEnd);
            previousEnd = task.getEndTime();
        }
        sample.hop().add(execution.getEndTime() - previousEnd);
    }

    private static String stats(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return String.format("p50 %4d   p90 %4d   max %4d ms", sorted.get(sorted.size() / 2),
                sorted.get((int) (sorted.size() * 0.9)), sorted.get(sorted.size() - 1));
    }
}
