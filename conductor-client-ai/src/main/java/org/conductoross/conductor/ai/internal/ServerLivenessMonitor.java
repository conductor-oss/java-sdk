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
package org.conductoross.conductor.ai.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

/**
 * Watches a stateful run for worker stalls (spec R11).
 *
 * <p>A background thread polls the workflow every
 * {@code livenessCheckIntervalSeconds}; a {@code SCHEDULED} task with
 * {@code pollCount == 0} older than {@code livenessStallSeconds} means no
 * worker is polling its queue (stateful runs enqueue under a per-execution
 * domain — if the owning process dies, nothing ever polls). The stall is
 * recorded for the handle's wait loop to surface as
 * {@link org.conductoross.conductor.ai.exceptions.WorkerStallError}; monitoring
 * stops on the first stall, on terminal workflow status, or on {@link #close()}.
 * Transient poll errors are ignored — liveness must never fail a healthy run.
 */
public final class ServerLivenessMonitor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ServerLivenessMonitor.class);

    private final WorkflowClient workflowClient;
    private final String executionId;
    private final long stallMillis;
    private final long checkIntervalMillis;
    private final boolean daemon;
    private final AtomicReference<String> stalledTaskRef = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Thread thread;

    public ServerLivenessMonitor(
            WorkflowClient workflowClient,
            String executionId,
            double stallSeconds,
            double checkIntervalSeconds,
            boolean daemon) {
        this.workflowClient = workflowClient;
        this.executionId = executionId;
        this.stallMillis = (long) (stallSeconds * 1000);
        this.checkIntervalMillis = Math.max(1, (long) (checkIntervalSeconds * 1000));
        this.daemon = daemon;
    }

    /** Start the background check thread (no-op transport errors, see class doc). */
    public void start() {
        Thread t = new Thread(this::loop, "agentspan-liveness-" + executionId);
        t.setDaemon(daemon);
        t.start();
        thread = t;
    }

    /** The stalled task's reference name, or {@code null} while healthy. */
    public String stalledTask() {
        return stalledTaskRef.get();
    }

    @Override
    public void close() {
        closed.set(true);
        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
    }

    private void loop() {
        while (!closed.get()) {
            try {
                Thread.sleep(checkIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (closed.get()) {
                return;
            }
            try {
                Workflow workflow = workflowClient.getWorkflow(executionId, true);
                if (workflow == null) {
                    continue;
                }
                if (workflow.getStatus() != null && workflow.getStatus().isTerminal()) {
                    return; // run finished — nothing left to watch
                }
                long now = System.currentTimeMillis();
                List<Task> tasks = workflow.getTasks() != null ? workflow.getTasks() : List.of();
                for (Task task : tasks) {
                    if (task.getStatus() == Task.Status.SCHEDULED
                            && task.getPollCount() == 0
                            && task.getScheduledTime() > 0
                            && now - task.getScheduledTime() > stallMillis) {
                        String ref = task.getReferenceTaskName() != null
                                ? task.getReferenceTaskName()
                                : task.getTaskDefName();
                        if (stalledTaskRef.compareAndSet(null, ref)) {
                            logger.warn(
                                    "Liveness: task '{}' in execution {} SCHEDULED with zero polls for > {}ms",
                                    ref,
                                    executionId,
                                    stallMillis);
                        }
                        return; // stall recorded — monitoring done
                    }
                }
            } catch (Exception e) {
                logger.debug("Liveness check failed for {} (ignored): {}", executionId, e.getMessage());
            }
        }
    }
}
