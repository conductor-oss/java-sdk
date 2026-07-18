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

import java.io.InputStream;
import java.nio.file.Path;

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

/** Copyable examples for every public {@link FileClient} operation. */
public final class FileClientUsage {

    private FileClientUsage() {}

    /** Upload a readable path and infer its filename. */
    public static String uploadPath(FileClient files, String workflowId, Path source) {
        return files.upload(workflowId, source);
    }

    /** Upload a path with explicit workflow metadata. Multipart selection remains automatic. */
    public static String uploadPathWithMetadata(
            FileClient files,
            String workflowId,
            String taskId,
            Path source,
            String fileName,
            String contentType) {
        FileUploadOptions options = new FileUploadOptions()
                .setFileName(fileName)
                .setContentType(contentType)
                .setTaskId(taskId);
        return files.upload(workflowId, source, options);
    }

    /**
     * Upload a caller-owned stream. A filename is mandatory, and FileClient does not close the
     * stream.
     */
    public static String uploadStream(
            FileClient files,
            String workflowId,
            String taskId,
            InputStream source,
            String fileName,
            String contentType) {
        FileUploadOptions options = new FileUploadOptions()
                .setFileName(fileName)
                .setContentType(contentType)
                .setTaskId(taskId);
        return files.upload(workflowId, source, options);
    }

    /** Read workflow-authorized metadata for an opaque handle. */
    public static FileMetadata metadata(
            FileClient files, String workflowId, String fileHandleId) {
        return files.getMetadata(workflowId, fileHandleId);
    }

    /** Download to a new or existing destination using atomic replacement. */
    public static Path download(
            FileClient files, String workflowId, String fileHandleId, Path destination) {
        return files.download(workflowId, fileHandleId, destination);
    }
}
