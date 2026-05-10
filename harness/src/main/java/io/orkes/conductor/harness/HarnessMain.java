/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.harness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.metrics.prometheus.AbstractPrometheusMetricsCollector;
import com.netflix.conductor.client.metrics.prometheus.MetricsBundle;
import com.netflix.conductor.client.metrics.prometheus.MetricsCollectorFactory;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import io.orkes.conductor.client.ApiClient;

public class HarnessMain {

    private static final Logger log = LoggerFactory.getLogger(HarnessMain.class);

    private static final String WORKFLOW_NAME = "java_simulated_tasks_workflow";

    private static final String[][] SIMULATED_WORKERS = {
        {"java_worker_0", "quickpulse", "1"},
        {"java_worker_1", "whisperlink", "2"},
        {"java_worker_2", "shadowfetch", "3"},
        {"java_worker_3", "ironforge", "4"},
        {"java_worker_4", "deepcrawl", "5"},
    };

    public static void main(String[] args) throws Exception {
        int workflowsPerSec = envInt("HARNESS_WORKFLOWS_PER_SEC", 2);
        int batchSize = envInt("HARNESS_BATCH_SIZE", 20);
        int pollIntervalMs = envInt("HARNESS_POLL_INTERVAL_MS", 100);
        int metricsPort = envInt("HARNESS_METRICS_PORT", 9991);
        String wiringMode = envString("METRICS_WIRING", "auto");
        // Opt-in: when > 0, WorkflowStatusProbe periodically calls
        // /api/workflow/<id> and /api/workflow/<id>/status so the canonical
        // http_api_client_request_seconds histogram picks up UUID-bearing
        // uri label values (the realistic high-cardinality case). Default 0
        // = disabled, which keeps harness behavior unchanged.
        int probeRatePerSec = envInt("HARNESS_PROBE_RATE_PER_SEC", 0);

        List<Worker> workers = new ArrayList<>();
        for (String[] entry : SIMULATED_WORKERS) {
            workers.add(new SimulatedTaskWorker(entry[0], entry[1], Integer.parseInt(entry[2]), batchSize,
                    pollIntervalMs));
        }
        Map<String, Integer> threadCounts =
                workers.stream().collect(Collectors.toMap(Worker::getTaskDefName, w -> batchSize));

        WiringResult wiring = switch (wiringMode.toLowerCase().trim()) {
            case "manual" -> wireManual(metricsPort, workers, threadCounts);
            default -> wireAuto(metricsPort, workers, threadCounts);
        };

        registerMetadata(wiring.client);

        wiring.configurer.init();

        WorkflowStatusProbe probe = new WorkflowStatusProbe(wiring.workflowClient, probeRatePerSec);
        WorkflowGovernor governor = new WorkflowGovernor(
                wiring.workflowClient, WORKFLOW_NAME, workflowsPerSec, probe::offer);
        governor.start();
        probe.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down harness...");
            governor.shutdown();
            probe.shutdown();
            wiring.configurer.shutdown();
        }));

        Thread.currentThread().join();
    }

    // -------------------------------------------------------------------------
    // METRICS_WIRING=auto — MetricsBundle + withMetricsCollector on builder;
    // all listener registration is automatic
    // -------------------------------------------------------------------------
    private static WiringResult wireAuto(int metricsPort, List<Worker> workers,
            Map<String, Integer> threadCounts) throws Exception {
        log.info("=== METRICS_WIRING=auto — automatic wiring via MetricsBundle ===");

        // MetricsBundle.create() defaults to port 9991 and /metrics if called with no args
        MetricsBundle bundle = MetricsBundle.create(metricsPort, "/metrics");
        log.info("Prometheus metrics server started on port {} ({} metrics)",
                bundle.getPort(), bundle.getCollector().collectorName());

        if (!bundle.getCollector().isAutoWiringEnabled()) {
            bundle.getCollector().setAutoWiringEnabled(true);
            log.info("Legacy collector does not auto-wire by default; "
                   + "explicitly enabling auto-wiring to honor METRICS_WIRING=auto");
        }

        ConductorClient client = ApiClient.builder()
                .useEnvVariables(true)
                .readTimeout(10_000)    // optional — OkHttp default applies if omitted
                .connectTimeout(10_000) // optional — OkHttp default applies if omitted
                .writeTimeout(10_000)   // optional — OkHttp default applies if omitted
                .withMetricsCollector(bundle.getCollector())
                .build();

        TaskClient taskClient = new TaskClient(client);
        WorkflowClient workflowClient = new WorkflowClient(client);

        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workers)
                        .withTaskThreadCount(threadCounts)
                        .build();

        return new WiringResult(client, workflowClient, configurer);
    }

    // -------------------------------------------------------------------------
    // METRICS_WIRING=manual — withHttpMetrics installs only the OkHttp
    // interceptor; all listener registration is explicit
    // -------------------------------------------------------------------------
    private static WiringResult wireManual(int metricsPort, List<Worker> workers,
            Map<String, Integer> threadCounts) throws Exception {
        log.info("=== METRICS_WIRING=manual — manual listener wiring ===");

        AbstractPrometheusMetricsCollector metricsCollector = MetricsCollectorFactory.create();
        metricsCollector.startServer(metricsPort, "/metrics");
        log.info("Prometheus metrics server started on port {} ({} metrics)", metricsPort, metricsCollector.collectorName());

        ConductorClient client = ApiClient.builder()
                .useEnvVariables(true)
                .readTimeout(10_000)
                .connectTimeout(10_000)
                .writeTimeout(10_000)
                .withHttpMetrics(metricsCollector)
                .build();

        TaskClient taskClient = new TaskClient(client);
        taskClient.registerListener(metricsCollector);
        taskClient.registerTaskRunnerListener(metricsCollector);

        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workers)
                        .withTaskThreadCount(threadCounts)
                        .withMetricsCollector(metricsCollector)
                        .build();

        WorkflowClient workflowClient = new WorkflowClient(client);
        workflowClient.registerListener(metricsCollector);

        return new WiringResult(client, workflowClient, configurer);
    }

    private record WiringResult(ConductorClient client, WorkflowClient workflowClient,
            TaskRunnerConfigurer configurer) { }

    // -------------------------------------------------------------------------

    private static void registerMetadata(ConductorClient client) {
        MetadataClient metadataClient = new MetadataClient(client);

        List<TaskDef> taskDefs = new ArrayList<>();
        for (String[] entry : SIMULATED_WORKERS) {
            String taskName = entry[0];
            String codename = entry[1];
            int sleepSeconds = Integer.parseInt(entry[2]);

            TaskDef td = new TaskDef(taskName);
            td.setDescription(
                    "Java SDK harness simulated task (" + codename + ", default delay " + sleepSeconds + "s)");
            td.setRetryCount(1);
            td.setTimeoutSeconds(300);
            td.setResponseTimeoutSeconds(300);
            taskDefs.add(td);
        }
        metadataClient.registerTaskDefs(taskDefs);
        log.info("Registered {} task definitions", taskDefs.size());

        WorkflowDef workflowDef = new WorkflowDef();
        workflowDef.setName(WORKFLOW_NAME);
        workflowDef.setVersion(1);
        workflowDef.setDescription("Java SDK harness simulated task workflow");
        workflowDef.setOwnerEmail("java-sdk-harness@conductor.io");

        List<WorkflowTask> wfTasks = new ArrayList<>();
        for (String[] entry : SIMULATED_WORKERS) {
            WorkflowTask wt = new WorkflowTask();
            wt.setName(entry[0]);
            wt.setTaskReferenceName(entry[1]);
            wt.setType(TaskType.SIMPLE.name());
            wfTasks.add(wt);
        }
        workflowDef.setTasks(wfTasks);

        metadataClient.updateWorkflowDefs(List.of(workflowDef));
        log.info("Registered workflow definition: {}", WORKFLOW_NAME);
    }

    private static int envInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String envString(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
