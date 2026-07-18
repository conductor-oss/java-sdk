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
package io.conductor.example.mediatranscoder.workers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class ManifestWorker implements Worker {

    private static final String MANIFEST = "{\"video\":\"%s\",\"videoName\":\"%s\",\"videoSize\":%d,\"thumbnail\":\"%s\",\"thumbName\":\"%s\",\"thumbSize\":%d}";
    private final FileClient fileClient;

    public ManifestWorker(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    @Override
    public String getTaskDefName() {
        return "create_manifest";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String video = (String) task.getInputData().get("transcoded_video");
        String thumb = (String) task.getInputData().get("thumbnail");
        Path manifestFile = null;

        try {
            String workflowId = task.getWorkflowInstanceId();
            System.out.println("[create_manifest] Inputs:");
            FileMetadata videoMetadata = describe(workflowId, video);
            FileMetadata thumbMetadata = describe(workflowId, thumb);

            String manifest = String.format(
                    MANIFEST,
                    video, videoMetadata.getFileName(), videoMetadata.getFileSize(),
                    thumb, thumbMetadata.getFileName(), thumbMetadata.getFileSize());

            manifestFile = Files.createTempFile("manifest-", ".json");
            Files.writeString(manifestFile, manifest);

            FileUploadOptions options = new FileUploadOptions().setContentType("application/json").setTaskId(task.getTaskId());
            String uploaded = fileClient.upload(workflowId, manifestFile, options);
            result.getOutputData().put("output_file", uploaded);
            result.setStatus(TaskResult.Status.COMPLETED);

            System.out.println("[create_manifest] Uploaded: " + uploaded);
            System.out.println("[create_manifest] Content: " + manifest);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            e.printStackTrace();
        } finally {
            if (manifestFile != null) {
                try {
                    Files.deleteIfExists(manifestFile);
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    private FileMetadata describe(String workflowId, String fileHandleId) throws IOException {
        FileMetadata metadata = fileClient.getMetadata(workflowId, fileHandleId);
        Path downloaded = Files.createTempFile("manifest-input-", ".bin");
        try {
            fileClient.download(workflowId, fileHandleId, downloaded);
            String content = Files.readString(downloaded, StandardCharsets.UTF_8);
            System.out.println("=================");
            System.out.printf("fileHandleId=%s  fileName=%s  contentType=%s  size=%d bytes%n             content=%s%n",
                    fileHandleId,
                    metadata.getFileName(),
                    metadata.getContentType(),
                    metadata.getFileSize(),
                    content);
            System.out.println("=================");
            return metadata;
        } finally {
            Files.deleteIfExists(downloaded);
        }
    }
}
