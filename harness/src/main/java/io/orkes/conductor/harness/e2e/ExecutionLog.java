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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.common.metadata.tasks.Task;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Records every execution independently of the SDK: live and peak concurrency as gauges, and one CSV line per
 * execution so the driver can detect duplicate executions and recompute concurrency after the run.
 */
class ExecutionLog {

    static final String HEADER = "taskId,taskType,workflowId,retryCount,pollCount,startMillis,endMillis,status,worker";

    private static final Logger log = LoggerFactory.getLogger(ExecutionLog.class);

    private final String worker;
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> maxInFlight = new ConcurrentHashMap<>();
    private final AtomicInteger totalInFlight = new AtomicInteger();
    private final AtomicInteger totalMaxInFlight = new AtomicInteger();

    ExecutionLog(Path file, String worker, List<String> taskTypes, MeterRegistry registry) {
        this.worker = worker;
        for (String taskType : taskTypes) {
            AtomicInteger current = inFlight.computeIfAbsent(taskType, k -> new AtomicInteger());
            AtomicInteger max = maxInFlight.computeIfAbsent(taskType, k -> new AtomicInteger());
            Gauge.builder("e2e_inflight", current, AtomicInteger::doubleValue).tag("taskType", taskType)
                    .register(registry);
            Gauge.builder("e2e_inflight_max", max, AtomicInteger::doubleValue).tag("taskType", taskType)
                    .register(registry);
        }
        Gauge.builder("e2e_process_inflight", totalInFlight, AtomicInteger::doubleValue).register(registry);
        Gauge.builder("e2e_process_inflight_max", totalMaxInFlight, AtomicInteger::doubleValue).register(registry);
        Thread writer = new Thread(() -> write(file), "e2e-execution-log");
        writer.setDaemon(true);
        writer.start();
    }

    long start(Task task) {
        int current = inFlight.get(task.getTaskDefName()).incrementAndGet();
        maxInFlight.get(task.getTaskDefName()).accumulateAndGet(current, Math::max);
        totalMaxInFlight.accumulateAndGet(totalInFlight.incrementAndGet(), Math::max);
        return System.currentTimeMillis();
    }

    void finish(Task task, long startMillis, String status) {
        long endMillis = System.currentTimeMillis();
        inFlight.get(task.getTaskDefName()).decrementAndGet();
        totalInFlight.decrementAndGet();
        lines.add(String.join(",", task.getTaskId(), task.getTaskDefName(), task.getWorkflowInstanceId(),
                Integer.toString(task.getRetryCount()), Integer.toString(task.getPollCount()),
                Long.toString(startMillis), Long.toString(endMillis), status, worker));
    }

    private void write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write(HEADER);
                out.newLine();
                List<String> batch = new ArrayList<>();
                while (true) {
                    String line = lines.poll(1, TimeUnit.SECONDS);
                    if (line != null) {
                        batch.add(line);
                        lines.drainTo(batch);
                        for (String entry : batch) {
                            out.write(entry);
                            out.newLine();
                        }
                        batch.clear();
                    }
                    out.flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Execution log writer failed", e);
            throw new UncheckedIOException(e);
        }
    }
}
