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
 * Response to {@code POST /api/files/{fileId}/upload-complete}. Status is
 * {@link FileUploadStatus#UPLOADED} on success; {@code contentHash} is the backend-reported
 * hash (or {@code null} for backends that do not expose one).
 */
public class FileUploadCompleteResponse {

    /** Prefixed handle: {@code conductor://file/<fileId>}. */
    private String fileHandleId;
    private FileUploadStatus uploadStatus;
    /** Content hash from the storage provider; {@code null} for local backend. */
    private String contentHash;

    public String getFileHandleId() { return fileHandleId; }
    public void setFileHandleId(String fileHandleId) { this.fileHandleId = fileHandleId; }

    public FileUploadStatus getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(FileUploadStatus uploadStatus) { this.uploadStatus = uploadStatus; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
