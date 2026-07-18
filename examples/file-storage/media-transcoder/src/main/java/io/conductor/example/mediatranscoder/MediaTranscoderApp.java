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
package io.conductor.example.mediatranscoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskDef.RetryLogic;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;
import io.conductor.example.mediatranscoder.workers.ManifestWorker;
import io.conductor.example.mediatranscoder.workers.ThumbnailWorker;
import io.conductor.example.mediatranscoder.workers.TranscodeWorker;
import io.conductor.example.mediatranscoder.workers.UploadPrimaryVideoWorker;
import org.conductoross.conductor.client.FileClient;

public class MediaTranscoderApp {

    private static final List<String> TASK_TYPES = List.of(
            "upload_primary_video",
            "transcode_video",
            "extract_thumbnail",
            "create_manifest");

    public static void main(String[] args) throws Exception {
        String serverUrl = System.getenv().getOrDefault(
                "CONDUCTOR_SERVER_URL", "http://localhost:8080/api");

        System.out.println("Connecting to Conductor at: " + serverUrl);

        ConductorClient client = ConductorClient.builder()
                .basePath(serverUrl)
                .build();

        TaskClient taskClient = new TaskClient(client);
        WorkflowClient workflowClient = new WorkflowClient(client);
        MetadataClient metadataClient = new MetadataClient(client);

        FileClient fileClient = new FileClient(client);

        // 1. Register every SIMPLE task definition, then register (or update) the workflow.
        registerMissingTaskDefinitions(metadataClient);
        WorkflowDef workflowDef = register(metadataClient);

        // 2. Start workers. upload_primary_video runs first inside the workflow and publishes
        // the primary video handle; downstream tasks consume it via
        // ${upload_primary_video_ref.output.primary_video}.
        // Every worker is an ordinary dependency-injected object whose work method is annotated
        // with @WorkerTask. The executor discovers those methods and handles input/output binding.
        AnnotatedWorkerExecutor workerExecutor = new AnnotatedWorkerExecutor(taskClient);
        workerExecutor.initWorkersFromInstances(List.of(
                new UploadPrimaryVideoWorker(fileClient),
                new TranscodeWorker(fileClient),
                new ThumbnailWorker(fileClient),
                new ManifestWorker(fileClient)));
        System.out.println("Workers started: upload_primary_video, transcode_video, extract_thumbnail, create_manifest");

        // 3. Start workflow — no inputs; upload_primary_video publishes primaryVideo.
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflowDef.getName());
        request.setVersion(workflowDef.getVersion());
        request.setInput(Map.of());

        String workflowId = workflowClient.startWorkflow(request);
        System.out.println("Workflow started: " + workflowId);

        // 4. Poll for completion.
        System.out.println("Waiting for workflow to complete...");
        for (int i = 0; i < 30; i++) {
            Thread.sleep(2000);
            Workflow workflow = workflowClient.getWorkflow(workflowId, true);
            System.out.println("  Status: " + workflow.getStatus());
            if (workflow.getStatus().isTerminal()) {
                System.out.println("Workflow " + workflow.getStatus() + "!");
                System.out.println("Output: " + workflow.getOutput());
                workerExecutor.shutdown();
                System.exit(workflow.getStatus().isSuccessful() ? 0 : 1);
            }
        }

        System.err.println("Workflow did not complete in 60s");
        workerExecutor.shutdown();
        System.exit(1);
    }

    public static WorkflowDef register(MetadataClient metadataClient) throws IOException {
        try (InputStream is = MediaTranscoderApp.class.getResourceAsStream("/workflow/media_transcode.json")) {
            WorkflowDef def = new ObjectMapper().readValue(is, WorkflowDef.class);
            metadataClient.updateWorkflowDefs(List.of(def));
            System.out.println("Registered workflow: " + def.getName() + " v" + def.getVersion());
            return def;
        }
    }

    private static void registerMissingTaskDefinitions(MetadataClient metadataClient) {
        Set<String> existing = new HashSet<>();
        for (TaskDef taskDef : metadataClient.getAllTaskDefs()) {
            existing.add(taskDef.getName());
        }

        List<TaskDef> missing = TASK_TYPES.stream()
                .filter(name -> !existing.contains(name))
                .map(MediaTranscoderApp::taskDefinition)
                .toList();
        if (!missing.isEmpty()) {
            metadataClient.registerTaskDefs(missing);
            System.out.println("Registered SIMPLE task definitions: "
                    + missing.stream().map(TaskDef::getName).toList());
        }
    }

    private static TaskDef taskDefinition(String name) {
        TaskDef taskDef = new TaskDef();
        taskDef.setName(name);
        taskDef.setDescription("FileClient media-transcoder example task");
        taskDef.setOwnerEmail("examples@conductor-oss.org");
        taskDef.setRetryCount(3);
        taskDef.setRetryLogic(RetryLogic.EXPONENTIAL_BACKOFF);
        taskDef.setRetryDelaySeconds(1);
        taskDef.setPollTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setTimeoutSeconds(300);
        return taskDef;
    }
}
