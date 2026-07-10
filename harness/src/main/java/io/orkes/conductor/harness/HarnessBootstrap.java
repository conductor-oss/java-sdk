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
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import jakarta.annotation.PreDestroy;

/**
 * Drives the self-feeding harness once the Spring context (and the
 * auto-configured, already-polling {@code TaskRunnerConfigurer}) is up:
 * registers metadata, then starts the workflow governor and the optional
 * status probe.
 */
@Component
public class HarnessBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HarnessBootstrap.class);

    private static final String WORKFLOW_NAME = "java_simulated_tasks_workflow";

    private final ConductorClient client;
    private final WorkflowClient workflowClient;
    private final HarnessProperties props;
    private final List<SimulatedTaskWorker> workers;

    private WorkflowGovernor governor;
    private WorkflowStatusProbe probe;

    public HarnessBootstrap(ConductorClient client,
                            WorkflowClient workflowClient,
                            HarnessProperties props,
                            List<SimulatedTaskWorker> workers) {
        this.client = client;
        this.workflowClient = workflowClient;
        this.props = props;
        // Sort by task name (java_worker_0..N) so the workflow chain order is
        // deterministic regardless of bean-injection order.
        this.workers = workers.stream()
                .sorted(Comparator.comparing(SimulatedTaskWorker::getTaskDefName))
                .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.isWorkersOnly()) {
            log.info("Running in workers-only mode (no registration, governor, or probe)");
            return;
        }

        registerMetadata(new MetadataClient(client));

        probe = new WorkflowStatusProbe(workflowClient, props.getProbeRatePerSec());
        governor = new WorkflowGovernor(workflowClient, WORKFLOW_NAME, props.getWorkflowsPerSec(), probe::offer);
        governor.start();
        probe.start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down harness...");
        if (governor != null) {
            governor.shutdown();
        }
        if (probe != null) {
            probe.shutdown();
        }
    }

    private void registerMetadata(MetadataClient metadataClient) {
        List<TaskDef> taskDefs = new ArrayList<>();
        for (SimulatedTaskWorker worker : workers) {
            TaskDef td = new TaskDef(worker.getTaskDefName());
            td.setDescription("Java SDK harness simulated task (" + worker.getCodename()
                    + ", default delay " + worker.getDelaySeconds() + "s)");
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
        for (SimulatedTaskWorker worker : workers) {
            WorkflowTask wt = new WorkflowTask();
            wt.setName(worker.getTaskDefName());
            wt.setTaskReferenceName(worker.getCodename());
            wt.setType(TaskType.SIMPLE.name());
            wfTasks.add(wt);
        }
        workflowDef.setTasks(wfTasks);

        metadataClient.updateWorkflowDefs(List.of(workflowDef));
        log.info("Registered workflow definition: {}", WORKFLOW_NAME);
    }
}
