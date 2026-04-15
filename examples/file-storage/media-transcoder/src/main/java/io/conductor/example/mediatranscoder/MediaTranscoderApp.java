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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorker;
import io.conductor.example.mediatranscoder.workers.ManifestWorker;
import io.conductor.example.mediatranscoder.workers.ThumbnailWorker;
import io.conductor.example.mediatranscoder.workers.TranscodeWorker;
import io.conductor.example.mediatranscoder.workers.UploadPrimaryVideoWorker;
import org.conductoross.conductor.client.FileClient;

public class MediaTranscoderApp {

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

        // 1. Register (or update) the workflow definition.
        WorkflowDef workflowDef = register(metadataClient);

        // 2. Start workers. upload_primary_video runs first inside the workflow and publishes
        // the primary video handle; downstream tasks consume it via
        // ${upload_primary_video_ref.output.primary_video}.
        // TranscodeWorker is an @WorkerTask-annotated bean — wrap it as an AnnotatedWorker so
        // the TaskRunner treats it like any Worker. Its input is bound to a TranscodeInput POJO
        // by the SDK's FileHandlerDeserializer, demonstrating FileHandler as a POJO field.
        TranscodeWorker transcodeBean = new TranscodeWorker();
        AnnotatedWorker transcodeWorker = new AnnotatedWorker(
                "transcode_video",
                TranscodeWorker.class.getMethod("transcode", TranscodeWorker.TranscodeInput.class),
                transcodeBean);

        List<Worker> workers = List.of(
                new UploadPrimaryVideoWorker(),
                transcodeWorker,
                new ThumbnailWorker(),
                new ManifestWorker());

        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withFileClient(fileClient)
                .withThreadCount(4)
                .build();
        configurer.init();
        System.out.println("Workers started: upload_primary_video, transcode_video, extract_thumbnail, create_manifest");

        // 3. Start workflow — no inputs; upload_primary_video publishes primaryVideo.
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflowDef.getName());
        request.setVersion(workflowDef.getVersion());
        request.setInput(Map.of());

        String workflowId = workflowClient.startWorkflow(request);
        System.out.println("Workflow started: " + workflowId);

        // 5. Poll for completion
        System.out.println("Waiting for workflow to complete...");
        for (int i = 0; i < 30; i++) {
            Thread.sleep(2000);
            Workflow workflow = workflowClient.getWorkflow(workflowId, true);
            System.out.println("  Status: " + workflow.getStatus());
            if (workflow.getStatus().isTerminal()) {
                System.out.println("Workflow " + workflow.getStatus() + "!");
                System.out.println("Output: " + workflow.getOutput());
                configurer.shutdown();
                System.exit(workflow.getStatus().isSuccessful() ? 0 : 1);
            }
        }

        System.err.println("Workflow did not complete in 60s");
        configurer.shutdown();
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
}
