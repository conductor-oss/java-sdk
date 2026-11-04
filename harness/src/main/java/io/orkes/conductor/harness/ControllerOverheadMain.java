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
package io.orkes.conductor.harness;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.automator.AdaptiveConcurrencyConfig;
import com.netflix.conductor.client.automator.AdaptiveConcurrencyController;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.events.taskrunner.PollStarted;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import io.orkes.conductor.client.ApiClient;

/**
 * Measures adaptive-controller overhead with many registered queues: a handful of busy workers fed by a workflow
 * governor, plus hundreds of idle workers whose queues never receive tasks. Reports, per window, the latency of
 * acquiring the controller lock (probed with {@code getHeldSlots()}), CPU time by thread group, poll request rate
 * and the controller's process target and held slots.
 *
 * <p>Environment: {@code OVERHEAD_IDLE_QUEUES} (default 300), {@code OVERHEAD_BUSY_QUEUES} (5),
 * {@code OVERHEAD_WORKFLOWS_PER_SEC} (10), {@code OVERHEAD_TASK_SLEEP_MS} (200), {@code OVERHEAD_POLL_INTERVAL_MS}
 * (100), {@code OVERHEAD_STANDBY_POLL_TIMEOUT_MS} (0 = standby polls off, the heaviest waiter churn),
 * {@code OVERHEAD_DURATION_SEC} (180), {@code OVERHEAD_REPORT_SEC} (30), and the usual
 * {@code CONDUCTOR_SERVER_URL} and auth variables.
 */
public class ControllerOverheadMain {

    private static final Logger log = LoggerFactory.getLogger(ControllerOverheadMain.class);

    private static final String WORKFLOW_NAME = "overhead_controller_workflow";
    private static final String BUSY_PREFIX = "overhead_busy_";
    private static final String IDLE_PREFIX = "overhead_idle_";

    public static void main(String[] args) throws Exception {
        int idleQueues = envInt("OVERHEAD_IDLE_QUEUES", 300);
        int busyQueues = envInt("OVERHEAD_BUSY_QUEUES", 5);
        int workflowsPerSec = envInt("OVERHEAD_WORKFLOWS_PER_SEC", 10);
        int taskSleepMs = envInt("OVERHEAD_TASK_SLEEP_MS", 200);
        int pollIntervalMs = envInt("OVERHEAD_POLL_INTERVAL_MS", 100);
        int standbyMs = envInt("OVERHEAD_STANDBY_POLL_TIMEOUT_MS", 0);
        int durationSec = envInt("OVERHEAD_DURATION_SEC", 180);
        int reportSec = envInt("OVERHEAD_REPORT_SEC", 30);

        ConductorClient client = ApiClient.builder()
                .useEnvVariables(true)
                .readTimeout(10_000)
                .connectTimeout(10_000)
                .writeTimeout(10_000)
                .build();

        registerMetadata(client, busyQueues, taskSleepMs);

        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i < busyQueues; i++) {
            workers.add(new OverheadWorker(BUSY_PREFIX + i, pollIntervalMs, taskSleepMs));
        }
        for (int i = 0; i < idleQueues; i++) {
            workers.add(new OverheadWorker(IDLE_PREFIX + i, pollIntervalMs, 0));
        }

        var adaptiveConfig = new AdaptiveConcurrencyConfig.Builder()
                .withMaxProcessConcurrency(envInt("OVERHEAD_MAX_PROCESS_CONCURRENCY", 256))
                .withDefaultMaxWorkerConcurrency(envInt("OVERHEAD_MAX_WORKER_CONCURRENCY", 64))
                .withStandbyPollTimeout(Duration.ofMillis(standbyMs))
                .build();
        AdaptiveConcurrencyController controller = new AdaptiveConcurrencyController(adaptiveConfig);

        AtomicLong pollsStarted = new AtomicLong();
        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(new TaskClient(client), workers)
                .withAdaptiveConcurrency(controller)
                .withListener(PollStarted.class, e -> pollsStarted.incrementAndGet())
                .build();

        WorkflowGovernor governor = new WorkflowGovernor(new WorkflowClient(client), WORKFLOW_NAME,
                workflowsPerSec, null);

        LockProbe lockProbe = new LockProbe(controller);
        lockProbe.start();
        configurer.init();
        governor.start();
        log.info("[overhead] running: {} idle + {} busy queues, {} wf/s x {} tasks of {} ms, standby={} ms, {} s",
                idleQueues, busyQueues, workflowsPerSec, busyQueues, taskSleepMs, standbyMs, durationSec);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            governor.shutdown();
            configurer.shutdown();
        }));

        CpuByGroup cpu = new CpuByGroup();
        long endAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSec);
        List<long[]> allWindows = new ArrayList<>();
        cpu.snapshot(); // establish the baseline
        long lastPolls = pollsStarted.get();
        while (System.nanoTime() < endAt) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(reportSec));
            long[] probe = lockProbe.drainSorted();
            allWindows.add(probe);
            long polls = pollsStarted.get();
            double pollRate = (double) (polls - lastPolls) / reportSec;
            lastPolls = polls;
            Map<String, Long> cpuMs = cpu.snapshot();
            log.info("[overhead] target={} held={} polls/s={} lockProbe us p50={} p99={} max={} (n={}) "
                            + "cpu ms/s: pollers={} controller-timers={} exec={} other={} process={}",
                    controller.getTargetConcurrency(), controller.getHeldSlots(),
                    String.format("%.1f", pollRate),
                    percentileUs(probe, 0.50), percentileUs(probe, 0.99), percentileUs(probe, 1.0), probe.length,
                    perSec(cpuMs.get("pollers"), reportSec), perSec(cpuMs.get("controller"), reportSec),
                    perSec(cpuMs.get("exec"), reportSec), perSec(cpuMs.get("other"), reportSec),
                    perSec(cpuMs.get("process"), reportSec));
        }

        long[] all = allWindows.stream().flatMapToLong(Arrays::stream).sorted().toArray();
        log.info("[overhead] SUMMARY queues={} lock probe us: p50={} p90={} p99={} p99.9={} max={} (n={})",
                idleQueues + busyQueues, percentileUs(all, 0.50), percentileUs(all, 0.90),
                percentileUs(all, 0.99), percentileUs(all, 0.999), percentileUs(all, 1.0), all.length);
        System.exit(0);
    }

    /** Times the acquisition of the controller lock every 10 ms via a public getter that takes it. */
    private static final class LockProbe extends Thread {
        private final AdaptiveConcurrencyController controller;
        private final List<Long> samples = new ArrayList<>();

        LockProbe(AdaptiveConcurrencyController controller) {
            super("overhead-lock-probe");
            this.controller = controller;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                long t0 = System.nanoTime();
                controller.getHeldSlots();
                long dt = System.nanoTime() - t0;
                synchronized (samples) {
                    samples.add(dt);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        long[] drainSorted() {
            long[] out;
            synchronized (samples) {
                out = samples.stream().mapToLong(Long::longValue).toArray();
                samples.clear();
            }
            Arrays.sort(out);
            return out;
        }
    }

    /** CPU time deltas bucketed by thread group plus whole-process CPU. */
    private static final class CpuByGroup {
        private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        private final com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        private final Map<Long, Long> lastCpuByThread = new HashMap<>();
        private long lastProcessCpu;

        Map<String, Long> snapshot() {
            Map<String, Long> deltaMs = new LinkedHashMap<>(
                    Map.of("pollers", 0L, "controller", 0L, "exec", 0L, "other", 0L));
            for (long id : threads.getAllThreadIds()) {
                ThreadInfo info = threads.getThreadInfo(id);
                long cpu = threads.getThreadCpuTime(id);
                if (info == null || cpu < 0) {
                    continue;
                }
                long delta = cpu - lastCpuByThread.getOrDefault(id, 0L);
                lastCpuByThread.put(id, cpu);
                String name = info.getThreadName();
                String group = name.startsWith("TaskRunner") ? "pollers"
                        : name.startsWith("adaptive-concurrency") ? "controller"
                        : name.startsWith("workflow-worker") ? "exec"
                        : "other";
                deltaMs.merge(group, TimeUnit.NANOSECONDS.toMillis(delta), Long::sum);
            }
            long processCpu = os.getProcessCpuTime();
            deltaMs.put("process", TimeUnit.NANOSECONDS.toMillis(processCpu - lastProcessCpu));
            lastProcessCpu = processCpu;
            return deltaMs;
        }
    }

    private static String perSec(Long totalMs, int windowSec) {
        return String.format("%.1f", totalMs == null ? 0 : (double) totalMs / windowSec);
    }

    private static long percentileUs(long[] sorted, double q) {
        if (sorted.length == 0) {
            return -1;
        }
        int index = Math.min(sorted.length - 1, (int) Math.ceil(q * sorted.length) - 1);
        return TimeUnit.NANOSECONDS.toMicros(sorted[Math.max(0, index)]);
    }

    private static final class OverheadWorker implements Worker {
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();
        private final String taskName;
        private final int pollIntervalMs;
        private final int sleepMs;

        OverheadWorker(String taskName, int pollIntervalMs, int sleepMs) {
            this.taskName = taskName;
            this.pollIntervalMs = pollIntervalMs;
            this.sleepMs = sleepMs;
        }

        @Override
        public String getTaskDefName() {
            return taskName;
        }

        @Override
        public int getPollingInterval() {
            return pollIntervalMs;
        }

        @Override
        public TaskResult execute(Task task) {
            if (sleepMs > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            EXECUTIONS.incrementAndGet();
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
    }

    private static void registerMetadata(ConductorClient client, int busyQueues, int taskSleepMs) {
        MetadataClient metadataClient = new MetadataClient(client);
        List<TaskDef> taskDefs = new ArrayList<>();
        List<WorkflowTask> workflowTasks = new ArrayList<>();
        for (int i = 0; i < busyQueues; i++) {
            String name = BUSY_PREFIX + i;
            TaskDef taskDef = new TaskDef(name);
            taskDef.setDescription("Controller overhead busy task (" + taskSleepMs + " ms)");
            taskDef.setRetryCount(0);
            taskDef.setTimeoutSeconds(300);
            taskDef.setResponseTimeoutSeconds(120);
            taskDefs.add(taskDef);

            WorkflowTask workflowTask = new WorkflowTask();
            workflowTask.setName(name);
            workflowTask.setTaskReferenceName("ref_" + name);
            workflowTask.setType(TaskType.SIMPLE.name());
            workflowTasks.add(workflowTask);
        }
        metadataClient.registerTaskDefs(taskDefs);

        WorkflowDef workflowDef = new WorkflowDef();
        workflowDef.setName(WORKFLOW_NAME);
        workflowDef.setVersion(1);
        workflowDef.setDescription("Controller overhead measurement workflow");
        workflowDef.setOwnerEmail("java-sdk-harness@conductor.io");
        workflowDef.setTasks(workflowTasks);
        metadataClient.updateWorkflowDefs(List.of(workflowDef));
        log.info("[overhead] registered {} busy task defs and workflow {}", busyQueues, WORKFLOW_NAME);
    }

    private static int envInt(String name, int defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }
}
