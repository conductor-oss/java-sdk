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
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

public class ManifestWorker implements Worker {

    public static final java.lang.String STRING = "{\"video\":\"%%s\",\"videoName\":\"%%s\",\"videoSize\":%%d,\"thumbnail\":\"%%s\",\"thumbName\":\"%%s\",\"thumbSize\":%%d}";

    @Override
    public String getTaskDefName() {
        return "create_manifest";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        FileHandler video = task.getInputFileHandler("transcoded_video");
        FileHandler thumb = task.getInputFileHandler("thumbnail");

        try {
            System.out.println("[create_manifest] Inputs:");
            describe(video);
            describe(thumb);

            String manifest = String.format(
                    STRING.formatted(),
                    video.getFileHandleId(), video.getFileName(), video.getFileSize(),
                    thumb.getFileHandleId(), thumb.getFileName(), thumb.getFileSize());

            Path manifestFile = Files.createTempFile("manifest-", ".json");
            Files.writeString(manifestFile, manifest);

            FileUploadOptions options = new FileUploadOptions().setContentType("application/json").setTaskId(task.getTaskId());
            FileHandler uploaded = task.getFileUploader().upload(manifestFile, options);
            result.getOutputData().put("output_file", uploaded.getFileHandleId());
            result.setStatus(TaskResult.Status.COMPLETED);

            System.out.println("[create_manifest] Uploaded: " + uploaded.getFileHandleId());
            System.out.println("[create_manifest] Content: " + manifest);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private static void describe(FileHandler fileHandler) throws IOException {
        String content = new String(fileHandler.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("=================");
        System.out.printf("fileHandleId=%s  fileName=%s  contentType=%s  size=%d bytes%n             content=%s%n",
                fileHandler.getFileHandleId(),
                fileHandler.getFileName(),
                fileHandler.getContentType(),
                fileHandler.getFileSize(),
                content);
        System.out.println("=================");
    }
}
