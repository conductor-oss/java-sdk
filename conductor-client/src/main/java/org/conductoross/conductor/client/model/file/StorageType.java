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
 * Storage backend identifier. Shared vocabulary between server and SDK — the server stamps its
 * configured type onto every file in {@code FileUploadResponse} and {@code FileHandle}; the SDK
 * selects a matching {@code FileStorageBackend} for byte transfer.
 */
public enum StorageType {
    /** AWS S3 (and S3-compatible services such as MinIO). */
    S3,
    /** Azure Blob Storage. */
    AZURE_BLOB,
    /** Google Cloud Storage. */
    GCS,
    /** Server-local filesystem. Does not support multipart. */
    LOCAL
}
