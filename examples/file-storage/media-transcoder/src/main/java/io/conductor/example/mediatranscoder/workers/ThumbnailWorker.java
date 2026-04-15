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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.conductoross.conductor.sdk.file.FileUploader;

public class ThumbnailWorker implements Worker {

    @Override
    public String getTaskDefName() { return "extract_thumbnail"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        FileHandler primary = task.getInputFileHandler("primary_video");

        try (InputStream in = primary.getInputStream()) {
            Path thumbnail = Files.createTempFile("thumb-", ".png");
            Files.write(thumbnail, "PNG_THUMBNAIL_DATA".getBytes());

            FileUploadOptions options = new FileUploadOptions().setContentType("image/png").setTaskId(task.getTaskId());
            FileHandler uploaded = task.getFileUploader().upload(thumbnail, options);
            result.getOutputData().put("output_file", uploaded.getFileHandleId());
            result.setStatus(TaskResult.Status.COMPLETED);

            System.out.println("[extract_thumbnail] Uploaded: " + uploaded.getFileHandleId());
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
