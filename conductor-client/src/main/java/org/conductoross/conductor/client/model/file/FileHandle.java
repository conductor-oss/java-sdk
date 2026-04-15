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
package org.conductoross.conductor.client.model.file;

/**
 * Client-side mirror of the server's {@code FileHandle} — full file metadata returned by
 * {@code GET /api/files/{fileId}}. Does not expose the server-internal {@code storagePath}.
 */
public class FileHandle {

    /** Prefixed handle: {@code conductor://file/<fileId>}. */
    private String fileHandleId;
    private String fileName;
    private String contentType;
    private long fileSize;
    /**
     * Content hash from the storage provider (S3 ETag / Azure Content-MD5 / GCS md5Hash).
     * {@code null} for the local backend and before upload is confirmed.
     */
    private String contentHash;
    private StorageType storageType;
    private FileUploadStatus uploadStatus;
    private String workflowId;
    private String taskId;
    /** Epoch millis. */
    private long createdAt;
    /** Epoch millis. */
    private long updatedAt;

    public String getFileHandleId() { return fileHandleId; }
    public void setFileHandleId(String fileHandleId) { this.fileHandleId = fileHandleId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public StorageType getStorageType() { return storageType; }
    public void setStorageType(StorageType storageType) { this.storageType = storageType; }

    public FileUploadStatus getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(FileUploadStatus uploadStatus) { this.uploadStatus = uploadStatus; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
