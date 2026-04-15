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
 * Response to {@code GET /api/files/{fileId}/upload-url} — a freshly issued presigned upload
 * URL, used on retry when the original URL from {@code FileUploadResponse} has expired.
 */
public class FileUploadUrlResponse {

    /** Prefixed handle: {@code conductor://file/<fileId>}. */
    private String fileHandleId;
    private String uploadUrl;
    /** Epoch millis. */
    private long expiresAt;

    public String getFileHandleId() { return fileHandleId; }
    public void setFileHandleId(String fileHandleId) { this.fileHandleId = fileHandleId; }

    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
