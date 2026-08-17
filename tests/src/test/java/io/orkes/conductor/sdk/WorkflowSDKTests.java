/*
 * Copyright 2023 Conductor Authors.
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
package io.orkes.conductor.sdk;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;
import com.netflix.conductor.sdk.workflow.executor.task.WorkerConfiguration;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import io.orkes.conductor.client.util.ClientTestUtil;


public class WorkflowSDKTests {

    @Test
    public void testCreateWorkflow() throws Exception {
        ConductorClient client = ClientTestUtil.getClient();

        AnnotatedWorkerExecutor workerExecutor = new AnnotatedWorkerExecutor(new TaskClient(client), new WorkerConfiguration());
        // initWorkers() already starts polling; a redundant extra startPolling() call here used to race with AnnotatedWorkerExecutor's own double-start (likely a real bug there) and could drop the first polled task.
        workerExecutor.initWorkers("io.orkes.conductor.sdk");

        WorkflowExecutor executor = new WorkflowExecutor(client, workerExecutor);

        ConductorWorkflow<Map<String, String>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("sdk_workflow");
        workflow.setVersion(1);
        workflow.add(new SimpleTask("sdk_task", "sdk_task"));

        workflow.registerWorkflow(true, true);

        CompletableFuture<Workflow> result = workflow.execute(Map.of("name", "orkes"));
        Assertions.assertNotNull(result);
        try {
            // WorkflowExecutor's monitor thread polls every 100ms (see initMonitor()), so 10s is a generous margin.
            Workflow executedWorkflow = result.get(10, TimeUnit.SECONDS);
            Assertions.assertNotNull(executedWorkflow);
            Assertions.assertEquals(Workflow.WorkflowStatus.COMPLETED, executedWorkflow.getStatus());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @WorkerTask(value = "sdk_task", pollingInterval = 10)
    public String sdkTask(@InputParam("name") String name) {
        return "Hello " + name;
    }
}
