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

    private static final String[][] SIMULATED_WORKERS = {
        {"java_worker_0", "quickpulse", "1"},
        {"java_worker_1", "whisperlink", "2"},
        {"java_worker_2", "shadowfetch", "3"},
        {"java_worker_3", "ironforge", "4"},
        {"java_worker_4", "deepcrawl", "5"},
    };

    private final ConductorClient client;
    private final WorkflowClient workflowClient;
    private final HarnessProperties props;

    private WorkflowGovernor governor;
    private WorkflowStatusProbe probe;

    public HarnessBootstrap(ConductorClient client, WorkflowClient workflowClient, HarnessProperties props) {
        this.client = client;
        this.workflowClient = workflowClient;
        this.props = props;
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
}
