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

import java.nio.file.Files;
import java.nio.file.Path;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class ThumbnailWorker implements Worker {

    private final FileClient fileClient;

    public ThumbnailWorker(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    @Override
    public String getTaskDefName() { return "extract_thumbnail"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String primary = (String) task.getInputData().get("primary_video");
        Path downloaded = null;
        Path thumbnail = null;

        try {
            downloaded = Files.createTempFile("primary-", ".mov");
            fileClient.download(task.getWorkflowInstanceId(), primary, downloaded);
            thumbnail = Files.createTempFile("thumb-", ".png");
            Files.write(thumbnail, "PNG_THUMBNAIL_DATA".getBytes());

            FileUploadOptions options = new FileUploadOptions().setContentType("image/png").setTaskId(task.getTaskId());
            String uploaded = fileClient.upload(task.getWorkflowInstanceId(), thumbnail, options);
            result.getOutputData().put("output_file", uploaded);
            result.setStatus(TaskResult.Status.COMPLETED);

            System.out.println("[extract_thumbnail] Uploaded: " + uploaded);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            e.printStackTrace();
        } finally {
            delete(downloaded);
            delete(thumbnail);
        }
        return result;
    }

    private static void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
