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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.WorkflowClient;

/**
 * Opt-in control-plane probe that exercises the UUID-bearing workflow lookup
 * endpoints, so {@code http_api_client_request_seconds} on the canonical
 * Prometheus surface picks up entries with {@code uri=/api/workflow/&lt;uuid&gt;}
 * and {@code uri=/api/workflow/&lt;uuid&gt;/status}. The default
 * {@link HarnessMain} traffic only ever hits bounded, no-path-param URLs
 * ({@code /api/tasks/poll/batch/&lt;taskType&gt;}, {@code /api/tasks},
 * {@code /api/workflow}, etc.), so the high-cardinality concern on the
 * {@code uri} label is invisible without something like this probe.
 *
 * <p><b>Default off.</b> The probe runs only when
 * {@code HARNESS_PROBE_RATE_PER_SEC} is set to a positive integer. Zero (the
 * default) means the harness behaves exactly as it did before.
 *
 * <p><b>Side-effect-free.</b> The probe only issues read calls
 * ({@link WorkflowClient#getWorkflow(String, boolean) getWorkflow} and
 * {@link WorkflowClient#getWorkflowStatusSummary getWorkflowStatusSummary}) and
 * does not pause, resume, terminate, or otherwise perturb the workflows the
 * task workers are processing.
 *
 * <p><b>Self-bounded.</b> Recently-started workflow IDs are kept in a
 * fixed-size FIFO; the probe rotates through them, so memory usage is
 * constant regardless of how long the harness runs.
 *
 * <p><b>Failure-tolerant.</b> Workflows can be archived or swept while the
 * probe still has their IDs; {@link Exception}s are logged at {@code debug}
 * and otherwise ignored so the probe never crashes the harness.
 *
 * <hr>
 *
 * <p><b>Stretch goals not implemented yet</b> — these are documented here so
 * the next iteration has a single place to look:
 *
 * <ol>
 *   <li><b>Per-task probe.</b> Add a {@code TaskStatusProbe} that calls
 *       {@code TaskClient.getTaskDetails(taskId)} (which hits
 *       {@code /api/tasks/&lt;taskId&gt;}). Plumb a
 *       {@code Consumer<String> taskIdSink} through
 *       {@link SimulatedTaskWorker}'s constructor and feed it from
 *       {@code execute(...)} (the {@code task.getTaskId()} value is already
 *       in scope there). This is the cardinality vector on the <i>task</i>
 *       path that the v4 batch-poll-as-ack pattern normally hides.</li>
 *   <li><b>Unique-correlationId probe.</b> Have {@link WorkflowGovernor} set a
 *       fresh {@code correlationId} (e.g. {@code UUID.randomUUID().toString()})
 *       on each {@code StartWorkflowRequest}, and have this probe
 *       occasionally call {@code WorkflowClient.getWorkflows(name,
 *       correlationId, ...)} which maps to
 *       {@code /api/workflow/&lt;name&gt;/correlated/&lt;correlationId&gt;}. That
 *       is a third UUID-bearing path and a useful "does the interceptor
 *       template <i>any</i> path segment?" test.</li>
 * </ol>
 */
public final class WorkflowStatusProbe {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusProbe.class);

    /** Cap on retained workflow IDs. Keeps memory constant regardless of harness uptime. */
    private static final int MAX_TRACKED_IDS = 256;

    private final WorkflowClient workflowClient;
    private final int callsPerSecond;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentLinkedDeque<String> recentIds = new ConcurrentLinkedDeque<>();

    public WorkflowStatusProbe(WorkflowClient workflowClient, int callsPerSecond) {
        this.workflowClient = workflowClient;
        this.callsPerSecond = callsPerSecond;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "workflow-status-probe");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Capture a workflow ID for later probing. Safe to call from any thread;
     * intended to be wired as the {@code idSink} of {@link WorkflowGovernor}.
     */
    public void offer(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            return;
        }
        recentIds.addFirst(workflowId);
        while (recentIds.size() > MAX_TRACKED_IDS) {
            recentIds.pollLast();
        }
    }

    public void start() {
        if (callsPerSecond <= 0) {
            log.info("WorkflowStatusProbe disabled (HARNESS_PROBE_RATE_PER_SEC<=0)");
            return;
        }
        log.info("WorkflowStatusProbe started: rate={}/sec, retainedIds<={}", callsPerSecond, MAX_TRACKED_IDS);
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void tick() {
        int budget = Math.min(callsPerSecond, recentIds.size());
        for (int i = 0; i < budget; i++) {
            String id = recentIds.pollFirst();
            if (id == null) {
                return;
            }
            recentIds.addLast(id);
            try {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    workflowClient.getWorkflow(id, false);
                } else {
                    workflowClient.getWorkflowStatusSummary(id, false, false);
                }
            } catch (Exception e) {
                log.debug("Probe: lookup failed for {}: {}", id, e.toString());
            }
        }
    }
}
