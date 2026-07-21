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
import java.nio.file.Files;
import java.nio.file.Path;

import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.WorkflowInstanceIdInputParam;
import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class ThumbnailWorker {

    private final FileClient fileClient;

    public ThumbnailWorker(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    public static class ThumbnailInput {
        public String primary_video;
    }

    @WorkerTask("extract_thumbnail")
    public @OutputParam("output_file") String extract(
            ThumbnailInput input,
            @WorkflowInstanceIdInputParam String workflowId) throws IOException {
        Path downloaded = null;
        Path thumbnail = null;

        try {
            downloaded = Files.createTempFile("primary-", ".mov");
            fileClient.download(workflowId, input.primary_video, downloaded);
            thumbnail = Files.createTempFile("thumb-", ".png");
            Files.write(thumbnail, "PNG_THUMBNAIL_DATA".getBytes());

            FileUploadOptions options = new FileUploadOptions().setContentType("image/png");
            String uploaded = fileClient.upload(workflowId, thumbnail, options);

            System.out.println("[extract_thumbnail] Uploaded: " + uploaded);
            return uploaded;
        } finally {
            delete(downloaded);
            delete(thumbnail);
        }
    }

    private static void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
