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
package io.orkes.conductor.harness.e2e;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.ApiClient;

/**
 * Drives the end-to-end test: registers four workflows that share four tasks unevenly, starts them with Poisson
 * arrivals through a sequence of phases, waits for the system to drain, then hands everything to
 * {@link E2eAnalysis}.
 */
public class E2eDriver {

    private static final Logger log = LoggerFactory.getLogger(E2eDriver.class);

    private static final String OWNER = "java-sdk-e2e@conductor.io";

    private final WorkflowClient workflowClient;
    private final E2eSampler sampler;
    private final String simulatorUrl;
    private final String runId = "run-" + System.currentTimeMillis();
    private final Random random = new Random(42);
    private final ExecutorService starters = Executors.newFixedThreadPool(16);
    private final Queue<StartedWorkflow> started = new ConcurrentLinkedQueue<>();
    private final AtomicInteger startErrors = new AtomicInteger();
    private final HttpClient http = HttpClient.newHttpClient();

    /** A workflow the driver started, with the phase it was started in. */
    record StartedWorkflow(String workflowId, String workflowName, String phase, boolean probe) {}

    E2eDriver(ConductorClient client, String simulatorUrl, List<String> workerUrls) {
        this.workflowClient = new WorkflowClient(client);
        this.simulatorUrl = simulatorUrl;
        this.sampler = new E2eSampler(new TaskClient(client), simulatorUrl, workerUrls);
    }

    public static void main(String[] args) throws Exception {
        ConductorClient client = ApiClient.builder().useEnvVariables(true).readTimeout(30_000).build();
        Path outDir = Path.of(E2eConfig.env("E2E_OUT_DIR", "build/e2e"));
        Files.createDirectories(outDir);
        E2eDriver driver = new E2eDriver(client, E2eConfig.env("SIM_URL", "http://localhost:8090"),
                E2eConfig.envList("E2E_WORKER_METRICS", "http://localhost:9991,http://localhost:9992"));
        driver.registerMetadata(new MetadataClient(client));
        boolean passed = driver.run(outDir);
        System.exit(passed ? 0 : 1);
    }

    boolean run(Path outDir) throws Exception {
        double rate = E2eConfig.envDouble("E2E_RATE", 5);
        sampler.start();
        sampler.setPhase("baseline");
        Thread.sleep(15_000);

        runPhase("warmup", E2eConfig.envInt("E2E_WARMUP_SECONDS", 60), rate / 2);
        runPhase("steady", E2eConfig.envInt("E2E_STEADY_SECONDS", 120), rate);
        sampler.setPhase("burst");
        int burstSize = E2eConfig.envInt("E2E_BURST_SIZE", 1000);
        for (int i = 0; i < burstSize; i++) {
            submit(E2eConfig.NIGHTLY_RECONCILE, "burst", false);
        }
        runPhase("burst", E2eConfig.envInt("E2E_BURST_SECONDS", 120), rate);
        setLatencyMultiplier(E2eConfig.envDouble("E2E_SLOWDOWN_MULTIPLIER", 5));
        try {
            runPhase("slowdown", E2eConfig.envInt("E2E_SLOWDOWN_SECONDS", 90), rate);
        } finally {
            setLatencyMultiplier(1);
        }
        runPhase("idle", E2eConfig.envInt("E2E_IDLE_SECONDS", 75), 0);
        sampler.setPhase("probe");
        // After the idle period the domain must still be live, so these tasks must land in the partner domain.
        for (int i = 0; i < E2eConfig.envInt("E2E_DOMAIN_PROBES", 10); i++) {
            submit(E2eConfig.PARTNER_SYNC, "probe", true);
        }
        starters.shutdown();
        starters.awaitTermination(5, TimeUnit.MINUTES);
        updateCounters();

        sampler.setPhase("drain");
        waitForDrain(E2eConfig.envInt("E2E_DRAIN_TIMEOUT_SECONDS", 300));
        Map<String, Workflow> workflows = fetchWorkflows();
        sampler.setPhase("final");
        Thread.sleep(5_000);
        sampler.stop();

        sampler.writeCsv(outDir.resolve("timeseries.csv"));
        E2eAnalysis analysis = new E2eAnalysis(List.copyOf(started), workflows, sampler.samples(),
                sampler.workerCount(), Path.of(E2eConfig.env("E2E_LOG_DIR", "build/e2e/logs")), startErrors.get());
        return analysis.writeReport(outDir);
    }

    private void runPhase(String phase, int seconds, double rate) throws InterruptedException {
        sampler.setPhase(phase);
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < end) {
            if (rate <= 0) {
                Thread.sleep(500);
            } else {
                // Poisson arrivals: exponential gaps between starts.
                Thread.sleep((long) (-Math.log(1 - random.nextDouble()) / rate * 1000));
                submit(pickWorkflow(), phase, false);
            }
            updateCounters();
        }
    }

    private String pickWorkflow() {
        double roll = random.nextDouble();
        if (roll < 0.65) {
            return E2eConfig.ORDER_FULFILLMENT;
        }
        return roll < 0.90 ? E2eConfig.RISK_REVIEW : E2eConfig.PARTNER_SYNC;
    }

    private void submit(String workflowName, String phase, boolean probe) {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflowName);
        request.setVersion(1);
        request.setCorrelationId(runId);
        request.setInput(input(workflowName));
        if (E2eConfig.PARTNER_SYNC.equals(workflowName)) {
            // Falls back to the default queue, which nobody polls, if no worker polled the domain recently.
            request.setTaskToDomain(Map.of(E2eConfig.NOTIFY_PARTNER, E2eConfig.PARTNER_DOMAIN + ",NO_DOMAIN"));
        }
        starters.submit(() -> {
            try {
                started.add(new StartedWorkflow(workflowClient.startWorkflow(request), workflowName, phase, probe));
            } catch (Exception e) {
                startErrors.incrementAndGet();
                log.warn("Failed to start {}: {}", workflowName, e.getMessage());
            }
        });
    }

    private synchronized Map<String, Object> input(String workflowName) {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", "c-" + random.nextInt(100_000));
        switch (workflowName) {
            case E2eConfig.ORDER_FULFILLMENT -> {
                input.put("docKb", logNormal(32, 0.7, 4, 512));
                int items = logNormal(3, 0.8, 1, 50);
                List<Map<String, Object>> tasks = new ArrayList<>();
                Map<String, Object> tasksInput = new LinkedHashMap<>();
                for (int i = 0; i < items; i++) {
                    tasks.add(Map.of("name", E2eConfig.FETCH_PROFILE, "taskReferenceName", "item_" + i, "type",
                            TaskType.SIMPLE.name()));
                    tasksInput.put("item_" + i, Map.of("id", "sku-" + random.nextInt(10_000)));
                }
                input.put("dynamicTasks", tasks);
                input.put("dynamicTasksInput", tasksInput);
            }
            case E2eConfig.RISK_REVIEW -> input.put("docKb", logNormal(128, 0.6, 16, 1024));
            case E2eConfig.PARTNER_SYNC -> input.put("partnerId", E2eConfig.PARTNER_DOMAIN);
            default -> {
            }
        }
        return input;
    }

    private int logNormal(double median, double sigma, int min, int max) {
        return (int) Math.max(min, Math.min(max, Math.round(median * Math.exp(sigma * random.nextGaussian()))));
    }

    private void updateCounters() {
        sampler.setCounters(Map.of("driver.started", (double) started.size(), "driver.startErrors",
                (double) startErrors.get()));
    }

    private void setLatencyMultiplier(double multiplier) throws Exception {
        http.send(HttpRequest.newBuilder(URI.create(
                simulatorUrl + "/control?endpoint=profile&latencyMultiplier=" + multiplier)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    /** Waits until every queue is empty and no worker is executing for 15 consecutive seconds. */
    private void waitForDrain(int timeoutSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        int quietSeconds = 0;
        while (System.nanoTime() < deadline && quietSeconds < 15) {
            Thread.sleep(1_000);
            E2eSampler.Sample latest = sampler.latest();
            boolean quiet = latest != null;
            if (latest != null) {
                for (String queue : E2eConfig.QUEUE_NAMES) {
                    quiet &= latest.get("depth." + queue) == 0;
                }
                for (int i = 0; i < sampler.workerCount(); i++) {
                    quiet &= latest.get("w" + i + ".inflight") == 0;
                }
            }
            quietSeconds = quiet ? quietSeconds + 1 : 0;
        }
        log.info("Drain finished (quiet for {}s)", quietSeconds);
    }

    /** Fetches every started workflow with its tasks, re-checking the ones still running for a short while. */
    private Map<String, Workflow> fetchWorkflows() throws Exception {
        Map<String, Workflow> workflows = new HashMap<>();
        List<String> pending = started.stream().map(StartedWorkflow::workflowId).toList();
        ExecutorService fetchers = Executors.newFixedThreadPool(16);
        try {
            for (int attempt = 0; attempt < 6 && !pending.isEmpty(); attempt++) {
                if (attempt > 0) {
                    log.info("{} workflows not finished; re-checking in 10s", pending.size());
                    Thread.sleep(10_000);
                }
                List<Future<Workflow>> futures = new ArrayList<>();
                for (String id : pending) {
                    futures.add(fetchers.submit(() -> workflowClient.getWorkflow(id, true)));
                }
                List<String> stillRunning = new ArrayList<>();
                for (Future<Workflow> future : futures) {
                    Workflow workflow = future.get();
                    workflows.put(workflow.getWorkflowId(), workflow);
                    if (!workflow.getStatus().isTerminal()) {
                        stillRunning.add(workflow.getWorkflowId());
                    }
                }
                pending = stillRunning;
            }
        } finally {
            fetchers.shutdown();
        }
        log.info("Fetched {} workflows", workflows.size());
        return workflows;
    }

    void registerMetadata(MetadataClient metadataClient) {
        List<TaskDef> taskDefs = new ArrayList<>();
        for (String taskType : E2eConfig.TASK_TYPES) {
            TaskDef taskDef = new TaskDef(taskType);
            taskDef.setOwnerEmail(OWNER);
            taskDef.setRetryCount(3);
            taskDef.setRetryLogic(TaskDef.RetryLogic.FIXED);
            taskDef.setRetryDelaySeconds(2);
            taskDef.setTimeoutSeconds(900);
            taskDef.setResponseTimeoutSeconds(120);
            taskDefs.add(taskDef);
        }
        metadataClient.registerTaskDefs(taskDefs);

        WorkflowTask fork = task(TaskType.FORK_JOIN_DYNAMIC.name(), "line_items", Map.of(
                "dynamicTasks", "${workflow.input.dynamicTasks}",
                "dynamicTasksInput", "${workflow.input.dynamicTasksInput}"));
        fork.setDynamicForkTasksParam("dynamicTasks");
        fork.setDynamicForkTasksInputParamName("dynamicTasksInput");
        WorkflowTask join = task(TaskType.JOIN.name(), "line_items_join", Map.of());
        join.setName("line_items_join");

        List<WorkflowDef> workflowDefs = List.of(
                workflow(E2eConfig.ORDER_FULFILLMENT,
                        simple(E2eConfig.FETCH_PROFILE, "customer", Map.of("id", "${workflow.input.customerId}")),
                        fork, join,
                        simple(E2eConfig.SCORE, "score", Map.of("docKb", "${workflow.input.docKb}")),
                        simple(E2eConfig.PERSIST, "persist", Map.of("score", "${score.output.score}"))),
                workflow(E2eConfig.RISK_REVIEW,
                        simple(E2eConfig.SCORE, "score_identity", Map.of("docKb", "${workflow.input.docKb}")),
                        simple(E2eConfig.SCORE, "score_transactions", Map.of("docKb", "${workflow.input.docKb}")),
                        simple(E2eConfig.PERSIST, "persist", Map.of())),
                workflow(E2eConfig.PARTNER_SYNC,
                        simple(E2eConfig.FETCH_PROFILE, "customer", Map.of("id", "${workflow.input.customerId}")),
                        simple(E2eConfig.NOTIFY_PARTNER, "notify", Map.of("partnerId",
                                "${workflow.input.partnerId}"))),
                workflow(E2eConfig.NIGHTLY_RECONCILE,
                        simple(E2eConfig.FETCH_PROFILE, "customer", Map.of("id", "${workflow.input.customerId}")),
                        simple(E2eConfig.PERSIST, "persist_ledger", Map.of()),
                        simple(E2eConfig.PERSIST, "persist_balance", Map.of()),
                        simple(E2eConfig.PERSIST, "persist_audit", Map.of())));
        metadataClient.updateWorkflowDefs(workflowDefs);
        log.info("Registered {} task definitions and {} workflows", taskDefs.size(), workflowDefs.size());
    }

    private static WorkflowDef workflow(String name, WorkflowTask... tasks) {
        WorkflowDef def = new WorkflowDef();
        def.setName(name);
        def.setVersion(1);
        def.setOwnerEmail(OWNER);
        def.setTasks(List.of(tasks));
        return def;
    }

    private static WorkflowTask simple(String taskType, String referenceName, Map<String, Object> input) {
        WorkflowTask task = task(TaskType.SIMPLE.name(), referenceName, input);
        task.setName(taskType);
        return task;
    }

    private static WorkflowTask task(String type, String referenceName, Map<String, Object> input) {
        WorkflowTask task = new WorkflowTask();
        task.setName(referenceName);
        task.setTaskReferenceName(referenceName);
        task.setType(type);
        task.setInputParameters(new HashMap<>(input));
        return task;
    }
}
