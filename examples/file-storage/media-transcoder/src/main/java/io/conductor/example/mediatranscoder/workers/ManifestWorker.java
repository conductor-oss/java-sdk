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

import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.WorkflowInstanceIdInputParam;
import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class ManifestWorker {

    private static final String MANIFEST = "{\"video\":\"%s\",\"videoName\":\"%s\",\"videoSize\":%d,\"thumbnail\":\"%s\",\"thumbName\":\"%s\",\"thumbSize\":%d}";
    private final FileClient fileClient;

    public ManifestWorker(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    public static class ManifestInput {
        public String transcoded_video;
        public String thumbnail;
    }

    @WorkerTask("create_manifest")
    public @OutputParam("output_file") String create(
            ManifestInput input,
            @WorkflowInstanceIdInputParam String workflowId) throws IOException {
        String video = input.transcoded_video;
        String thumb = input.thumbnail;
        Path manifestFile = null;

        try {
            System.out.println("[create_manifest] Inputs:");
            FileMetadata videoMetadata = describe(workflowId, video);
            FileMetadata thumbMetadata = describe(workflowId, thumb);

            String manifest = String.format(
                    MANIFEST,
                    video, videoMetadata.getFileName(), videoMetadata.getFileSize(),
                    thumb, thumbMetadata.getFileName(), thumbMetadata.getFileSize());

            manifestFile = Files.createTempFile("manifest-", ".json");
            Files.writeString(manifestFile, manifest);

            FileUploadOptions options = new FileUploadOptions().setContentType("application/json");
            String uploaded = fileClient.upload(workflowId, manifestFile, options);

            System.out.println("[create_manifest] Uploaded: " + uploaded);
            System.out.println("[create_manifest] Content: " + manifest);
            return uploaded;
        } finally {
            if (manifestFile != null) {
                try {
                    Files.deleteIfExists(manifestFile);
                } catch (IOException ignored) {
                }
            }
        }
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
