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

import java.io.InputStream;
import java.nio.file.Path;

import org.conductoross.conductor.client.model.file.StorageType;

/**
 * SDK-side counterpart to the server's {@code FileStorage}. Handles the actual byte transfer
 * between the worker and the configured storage backend (S3, Azure Blob, GCS, Local).
 *
 * <p>One implementation per backend. {@link org.conductoross.conductor.client.FileClient}
 * selects the backend that matches the server-reported {@link StorageType} on each upload or
 * download.
 */
public interface FileStorageBackend {

    /** Returns the {@link StorageType} this implementation handles. */
    StorageType getStorageType();

    /**
     * Uploads the full content of {@code localFile} to a server-issued URL (presigned PUT for
     * S3, SAS URL for Azure, signed URL for GCS, or a direct path for local).
     */
    void upload(String url, Path localFile);

    /**
     * Uploads the content of {@code inputStream} to the server-issued URL. {@code contentLength}
     * is required by backends that cannot accept chunked transfer.
     */
    void upload(String url, InputStream inputStream, long contentLength);

    /**
     * Downloads the content at the server-issued URL to {@code destination}. Creates parent
     * directories as needed.
     */
    void download(String url, Path destination);

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param url        per-part presigned URL (S3) or resumable session URL (GCS/Azure)
     * @param localFile  source file
     * @param offset     byte offset into {@code localFile}
     * @param length     number of bytes to upload for this part
     * @return the part's ETag (or backend-equivalent identifier), to be supplied to the
     *         server's complete-multipart call
     */
    String uploadPart(String url, Path localFile, long offset, long length);

    /**
     * Whether this backend supports multipart upload. Return {@code false} for backends that do
     * not (e.g. local); {@link org.conductoross.conductor.client.FileClient} will then fall back
     * to single-part upload regardless of the configured multipart threshold.
     */
    default boolean hasMultipartSupport() {
        return true;
    }
}
