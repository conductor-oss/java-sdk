/*
 * Copyright 2022 Conductor Authors.
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
package com.netflix.conductor.sdk.testing;


import java.util.List;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;

import lombok.Getter;

public class WorkflowTestRunner {

    private LocalServerRunner localServerRunner;

    private final AnnotatedWorkerExecutor annotatedWorkerExecutor;

    @Getter
    private final WorkflowExecutor workflowExecutor;

    public WorkflowTestRunner(String serverApiUrl) {
        TaskClient taskClient = new TaskClient(new ConductorClient.Builder()
                .basePath(serverApiUrl)
                .build());
        this.annotatedWorkerExecutor = new AnnotatedWorkerExecutor(taskClient);
        this.workflowExecutor = new WorkflowExecutor(serverApiUrl);
    }

    public WorkflowTestRunner(int port, String conductorVersion) {
        localServerRunner = new LocalServerRunner(port, conductorVersion);

        String serverAPIUrl = localServerRunner.getServerAPIUrl();
        TaskClient taskClient = new TaskClient(new ConductorClient.Builder()
                .basePath(serverAPIUrl)
                .build());
        this.annotatedWorkerExecutor = new AnnotatedWorkerExecutor(taskClient);
        this.workflowExecutor = new WorkflowExecutor(serverAPIUrl);
    }

    public void init(String basePackages) {
        if (localServerRunner != null) {
            localServerRunner.startLocalServer();
        }
        annotatedWorkerExecutor.initWorkers(basePackages);
    }

    public void init(List<Object> workerBeans) {
        if (localServerRunner != null) {
            localServerRunner.startLocalServer();
        }
        if(workerBeans == null) {
            return;
        }
        for (Object workerBean : workerBeans) {
            annotatedWorkerExecutor.addBean(workerBean);
        }
    }

    public void shutdown() {
        localServerRunner.shutdown();
        annotatedWorkerExecutor.shutdown();
        workflowExecutor.shutdown();
    }
}
