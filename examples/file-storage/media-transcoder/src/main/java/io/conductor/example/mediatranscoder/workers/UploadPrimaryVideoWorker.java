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

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class UploadPrimaryVideoWorker implements Worker {

    private static final String RESOURCE_PATH = "/data/primary_video.mov";

    @Override
    public String getTaskDefName() {
        return "upload_primary_video";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try (InputStream in = UploadPrimaryVideoWorker.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Primary video not found on classpath at " + RESOURCE_PATH);
            }

            FileUploadOptions options = new FileUploadOptions()
                    .setFileName("primary_video.mov")
                    .setContentType("video/quicktime");
            FileHandler uploaded = task.getFileUploader().upload(in, options);

            result.getOutputData().put("primary_video", uploaded);
            result.setStatus(TaskResult.Status.COMPLETED);

            System.out.println("[upload_primary_video] Uploaded: " + uploaded.getFileHandleId());
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
