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

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.AdaptiveConcurrencyConfig;
import com.netflix.conductor.client.automator.AdaptiveConcurrencyController;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;
import com.netflix.conductor.client.worker.Worker;

import io.orkes.conductor.client.ApiClient;

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

/**
 * One worker process of the end-to-end test. {@code E2E_MODE=adaptive} uses the adaptive controller;
 * {@code E2E_MODE=fixed} uses fixed thread counts from {@code E2E_FIXED_THREADS}.
 */
public class E2eWorkerMain {

    private static final Logger log = LoggerFactory.getLogger(E2eWorkerMain.class);

    public static void main(String[] args) throws Exception {
        String mode = E2eConfig.env("E2E_MODE", "adaptive");
        String identity = E2eConfig.env("HOSTNAME", UUID.randomUUID().toString().substring(0, 8));
        int pollingInterval = E2eConfig.envInt("E2E_POLL_INTERVAL_MS", 100);

        PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector();
        var registry = metricsCollector.getRegistry();
        new ProcessorMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);

        ConductorClient client = ApiClient.builder()
                .useEnvVariables(true)
                .readTimeout(10_000)
                .connectTimeout(10_000)
                .writeTimeout(10_000)
                .withMetricsCollector(metricsCollector)
                .build();

        ExecutionLog executionLog = new ExecutionLog(
                Path.of(E2eConfig.env("E2E_LOG_DIR", "build/e2e"), "executions-" + identity + ".csv"), identity,
                E2eConfig.TASK_TYPES, registry);
        var downstream = new E2eWorkers.Downstream(E2eConfig.env("SIM_URL", "http://localhost:8090"));
        List<Worker> workers = List.of(
                new E2eWorkers.FetchProfileWorker(identity, pollingInterval, executionLog, downstream,
                        E2eConfig.envDouble("E2E_FAILURE_RATE", 0.01)),
                new E2eWorkers.ScoreWorker(identity, pollingInterval, executionLog,
                        E2eConfig.envInt("E2E_SCORE_PASSES", 40)),
                new E2eWorkers.PersistWorker(identity, pollingInterval, executionLog, downstream,
                        E2eConfig.envInt("E2E_DB_POOL", 10)),
                new E2eWorkers.NotifyPartnerWorker(identity, pollingInterval, executionLog, downstream,
                        E2eConfig.envInt("E2E_CALLBACK_SECONDS", 5)));

        var builder = new TaskRunnerConfigurer.Builder(new TaskClient(client), workers)
                .withTaskToDomain(Map.of(E2eConfig.NOTIFY_PARTNER, E2eConfig.PARTNER_DOMAIN))
                .withUseVirtualThreads(Boolean.parseBoolean(E2eConfig.env("E2E_VIRTUAL_THREADS", "false")))
                .withUpdateV2(Boolean.parseBoolean(E2eConfig.env("E2E_UPDATE_V2", "false")));
        if ("adaptive".equals(mode)) {
            var config = new AdaptiveConcurrencyConfig.Builder()
                    .withMaxProcessConcurrency(E2eConfig.envInt("E2E_MAX_PROCESS_CONCURRENCY", 256))
                    .withInitialProcessConcurrency(E2eConfig.envInt("E2E_INITIAL_PROCESS_CONCURRENCY", 0))
                    .withMinProcessConcurrency(E2eConfig.envInt("E2E_MIN_PROCESS_CONCURRENCY", 1))
                    .withStandbyPollTimeout(Duration.ofMillis(E2eConfig.envInt("E2E_STANDBY_POLL_TIMEOUT_MS", 1000)))
                    .withDefaultMaxWorkerConcurrency(E2eConfig.envInt("E2E_MAX_WORKER_CONCURRENCY", 64))
                    .build();
            builder.withAdaptiveConcurrency(new AdaptiveConcurrencyController(config));
            log.info("Worker {} running adaptive: process ceiling {}, worker ceiling {}", identity,
                    config.getMaxProcessConcurrency(), config.getDefaultMaxWorkerConcurrency());
        } else {
            Map<String, Integer> threads = E2eConfig.envIntMap("E2E_FIXED_THREADS",
                    "e2e_fetch_profile=8,e2e_score=3,e2e_persist=10,e2e_notify_partner=2");
            builder.withTaskThreadCount(threads);
            log.info("Worker {} running fixed threads {}", identity, threads);
        }
        TaskRunnerConfigurer configurer = builder.build();

        metricsCollector.startServer(E2eConfig.envInt("E2E_METRICS_PORT", 9991), "/metrics");
        configurer.init();
        Runtime.getRuntime().addShutdownHook(new Thread(configurer::shutdown));
        Thread.currentThread().join();
    }
}
