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
package org.conductoross.conductor.sdk.file;

import java.nio.file.Path;

import org.conductoross.conductor.client.model.file.FileUploadRequest;
import org.conductoross.conductor.client.model.file.FileUploadResponse;

/** Conversion helpers between the file-storage DTOs and the internal {@link ManagedFileHandler}. */
public class FileHandlerConverter {

    /**
     * Builds a {@link ManagedFileHandler} from a completed upload response. The handler already
     * has its metadata and {@code localPath} populated — no server round-trip needed on first
     * access.
     */
    public static ManagedFileHandler toManagedFileHandler(FileUploadResponse response, WorkflowFileClient workflowFileClient, Path localPath, long fileSize) {
        ManagedFileHandler handler = new ManagedFileHandler(response.getFileHandleId(), workflowFileClient);
        handler.setFileName(response.getFileName());
        handler.setContentType(response.getContentType());
        handler.setFileSize(fileSize);
        handler.setStorageType(response.getStorageType());
        handler.setLocalPath(localPath);
        return handler;
    }

    /** Builds the {@link FileUploadRequest} payload for {@code POST /api/files}. */
    public static FileUploadRequest toFileUploadRequest(String workflowId, FileUploadOptions options) {
        FileUploadRequest request = new FileUploadRequest();
        request.setFileName(options.getFileName());
        request.setContentType(options.getContentType());
        request.setWorkflowId(workflowId);
        request.setTaskId(options.getTaskId());
        return request;
    }
}
