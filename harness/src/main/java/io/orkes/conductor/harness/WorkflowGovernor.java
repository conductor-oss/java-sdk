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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

public class WorkflowGovernor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowGovernor.class);

    private final WorkflowClient workflowClient;
    private final String workflowName;
    private final int workflowsPerSecond;
    private final ScheduledExecutorService scheduler;

    public WorkflowGovernor(WorkflowClient workflowClient, String workflowName, int workflowsPerSecond) {
        this.workflowClient = workflowClient;
        this.workflowName = workflowName;
        this.workflowsPerSecond = workflowsPerSecond;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "workflow-governor");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        log.info("WorkflowGovernor started: workflow={}, rate={}/sec", workflowName, workflowsPerSecond);
        scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
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
        try {
            for (int i = 0; i < workflowsPerSecond; i++) {
                StartWorkflowRequest request = new StartWorkflowRequest();
                request.setName(workflowName);
                workflowClient.startWorkflow(request);
            }
            log.info("Governor: started {} workflow(s)", workflowsPerSecond);
        } catch (Exception e) {
            log.error("Governor: error starting workflows", e);
        }
    }
}
