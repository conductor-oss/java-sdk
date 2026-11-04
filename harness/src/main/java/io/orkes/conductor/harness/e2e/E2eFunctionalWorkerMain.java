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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.AdaptiveConcurrencyConfig;
import com.netflix.conductor.client.automator.AdaptiveConcurrencyController;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import io.orkes.conductor.client.ApiClient;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

/**
 * Worker process for the functional scenarios. Each task type gets its own {@link TaskRunnerConfigurer} sharing one
 * adaptive controller, so every queue's execution threads carry the task type in their name and can be counted.
 * Polling starts once the driver has loaded the queues and created {@code E2E_LOG_DIR/go}.
 */
public class E2eFunctionalWorkerMain {

    private static final Logger log = LoggerFactory.getLogger(E2eFunctionalWorkerMain.class);

    public static void main(String[] args) throws Exception {
        String identity = E2eConfig.env("HOSTNAME", "fn-worker");
        List<String> taskTypes = E2eConfig.envList("E2E_TASKS", "fn_a,fn_b,fn_c,fn_d");
        Map<String, String> domains = E2eConfig.envStringMap("E2E_TASK_DOMAINS", "");
        Path logDir = Path.of(E2eConfig.env("E2E_LOG_DIR", "build/e2e"));

        PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector();
        var registry = metricsCollector.getRegistry();
        new ProcessorMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        for (String taskType : taskTypes) {
            String prefix = threadPrefix(taskType);
            Gauge.builder("e2e_pool_threads", threads, bean -> countThreads(bean, prefix))
                    .tag("taskType", taskType)
                    .register(registry);
        }

        ConductorClient client = ApiClient.builder()
                .useEnvVariables(true)
                .readTimeout(10_000)
                .connectTimeout(10_000)
                .writeTimeout(10_000)
                .withMetricsCollector(metricsCollector)
                .build();
        ExecutionLog executionLog = new ExecutionLog(logDir.resolve("executions-" + identity + ".csv"), identity,
                taskTypes, registry);

        var config = new AdaptiveConcurrencyConfig.Builder()
                .withMaxProcessConcurrency(E2eConfig.envInt("E2E_MAX_PROCESS_CONCURRENCY", 256))
                .withInitialProcessConcurrency(E2eConfig.envInt("E2E_INITIAL_PROCESS_CONCURRENCY", 0))
                .withMinProcessConcurrency(E2eConfig.envInt("E2E_MIN_PROCESS_CONCURRENCY", 1))
                .withStandbyPollTimeout(Duration.ofMillis(E2eConfig.envInt("E2E_STANDBY_POLL_TIMEOUT_MS", 1000)))
                .withDefaultMaxWorkerConcurrency(E2eConfig.envInt("E2E_MAX_WORKER_CONCURRENCY", 64))
                .build();
        var controller = new AdaptiveConcurrencyController(config);
        metricsCollector.startServer(E2eConfig.envInt("E2E_METRICS_PORT", 9991), "/metrics");

        Path go = logDir.resolve("go");
        log.info("Worker {} waiting for {} (task types {}, domains {}, process ceiling {}, initial {}, worker ceiling {})",
                identity, go, taskTypes, domains, config.getMaxProcessConcurrency(),
                config.getInitialProcessConcurrency(), config.getDefaultMaxWorkerConcurrency());
        while (!Files.exists(go)) {
            Thread.sleep(200);
        }

        TaskClient taskClient = new TaskClient(client);
        List<TaskRunnerConfigurer> configurers = new ArrayList<>();
        for (String taskType : taskTypes) {
            Worker worker = new LoadWorker(taskType, identity, executionLog);
            var builder = new TaskRunnerConfigurer.Builder(taskClient, List.of(worker))
                    .withWorkerNamePrefix(threadPrefix(taskType) + "%d")
                    .withAdaptiveConcurrency(controller);
            if (domains.containsKey(taskType)) {
                builder.withTaskToDomain(Map.of(taskType, domains.get(taskType)));
            }
            TaskRunnerConfigurer configurer = builder.build();
            configurer.init();
            configurers.add(configurer);
        }
        log.info("Worker {} polling", identity);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> configurers.forEach(TaskRunnerConfigurer::shutdown)));
        Thread.currentThread().join();
    }

    private static String threadPrefix(String taskType) {
        return taskType + "-exec-";
    }

    private static double countThreads(ThreadMXBean bean, String prefix) {
        int count = 0;
        for (var info : bean.getThreadInfo(bean.getAllThreadIds())) {
            if (info != null && info.getThreadName().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Does what the task input asks: sleep {@code sleepMs}, burn {@code cpuMs} of CPU, and hold {@code allocMb} of
     * heap for the whole execution.
     */
    static class LoadWorker extends E2eWorkers.InstrumentedWorker {

        LoadWorker(String taskType, String identity, ExecutionLog executionLog) {
            super(taskType, identity, 100, executionLog);
        }

        @Override
        TaskResult doExecute(Task task) {
            Map<String, Object> input = task.getInputData();
            int allocMb = (int) number(input, "allocMb");
            byte[][] payload = new byte[allocMb][];
            for (int i = 0; i < allocMb; i++) {
                payload[i] = new byte[1024 * 1024];
                payload[i][i % 1024] = 1;
            }
            burnCpu(number(input, "cpuMs"));
            long sleepMs = number(input, "sleepMs");
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("held", payload.length);
            return result;
        }

        /** Unset workflow inputs arrive as null values. */
        private static long number(Map<String, Object> input, String key) {
            return input.get(key) instanceof Number value ? value.longValue() : 0;
        }

        private static void burnCpu(long cpuMs) {
            if (cpuMs <= 0) {
                return;
            }
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long end = bean.getCurrentThreadCpuTime() + cpuMs * 1_000_000;
            long x = 0;
            while (bean.getCurrentThreadCpuTime() < end) {
                for (int i = 0; i < 10_000; i++) {
                    x += i * 31L ^ x;
                }
            }
            if (x == 42) {
                log.trace("unlikely");
            }
        }
    }
}
