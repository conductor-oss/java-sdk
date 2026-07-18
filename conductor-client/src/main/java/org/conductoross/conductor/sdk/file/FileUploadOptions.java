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

/**
 * Optional metadata to attach to a file upload. The owning workflow is always supplied directly
 * to {@code FileClient}; multipart selection is automatic and is not part of this API.
 *
 * <pre>{@code
 * FileUploadOptions options = new FileUploadOptions()
 *         .setContentType("application/pdf")
 *         .setTaskId(taskId);
 * String fileHandleId = fileClient.upload(workflowId, path, options);
 * }</pre>
 */
public class FileUploadOptions {

    private String taskId;
    private String fileName;
    private String contentType;

    public String getTaskId() {
        return taskId;
    }

    public FileUploadOptions setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public FileUploadOptions setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public FileUploadOptions setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

}
