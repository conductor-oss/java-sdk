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
package org.conductoross.conductor.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.client.model.file.FileDownloadUrlResponse;
import org.conductoross.conductor.client.model.file.FileHandle;
import org.conductoross.conductor.client.model.file.FileUploadCompleteResponse;
import org.conductoross.conductor.client.model.file.FileUploadRequest;
import org.conductoross.conductor.client.model.file.FileUploadResponse;
import org.conductoross.conductor.client.model.file.FileUploadUrlResponse;
import org.conductoross.conductor.client.model.file.MultipartCompleteRequest;
import org.conductoross.conductor.client.model.file.MultipartInitResponse;
import org.conductoross.conductor.client.model.file.StorageType;
import org.conductoross.conductor.client.storage.AzureFileStorageBackend;
import org.conductoross.conductor.client.storage.GcsFileStorageBackend;
import org.conductoross.conductor.client.storage.LocalFileStorageBackend;
import org.conductoross.conductor.client.storage.S3FileStorageBackend;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileHandlerConverter;
import org.conductoross.conductor.sdk.file.FileStorageBackend;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.conductoross.conductor.sdk.file.FileUploader;
import org.conductoross.conductor.sdk.file.ManagedFileHandler;
import org.conductoross.conductor.sdk.file.WorkflowFileClient;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.sdk.workflow.executor.task.TaskContext;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for the Conductor file-storage REST API.
 *
 * <p>Implements {@link FileUploader} for developer-facing uploads, and exposes SDK-internal
 * methods used by {@link ManagedFileHandler} for lazy metadata fetch and download. Composes a
 * {@code ConductorClient} for REST calls and a {@code Map<StorageType, FileStorageBackend>} for
 * byte transfer to the underlying storage backend.
 *
 * <p>On upload, the server reports which {@link StorageType} it is configured for; the client
 * looks up the matching {@link FileStorageBackend} in the map. If no backend is registered for
 * the server's type the upload fails fast with a {@link FileStorageException}.
 *
 * <p>REST paths use the bare {@code fileId} (no prefix) as the path variable. JSON bodies carry
 * the prefixed {@code fileHandleId} ({@code conductor://file/<fileId>}); conversion is handled
 * via {@link FileHandler}.
 */
public class FileClient {

    private static final TypeReference<FileUploadResponse> FILE_UPLOAD_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<FileUploadUrlResponse> FILE_UPLOAD_URL_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<FileUploadCompleteResponse> FILE_UPLOAD_COMPLETE_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<FileDownloadUrlResponse> FILE_DOWNLOAD_URL_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<FileHandle> FILE_HANDLE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MultipartInitResponse> MULTIPART_INIT_RESPONSE_TYPE = new TypeReference<>() {
    };

    private final ConductorClient client;
    private final FileClientProperties properties;
    private final Map<StorageType, FileStorageBackend> fileStorageBackendsByStorageType;

    /**
     * Creates a client with default {@link FileClientProperties} and the full set of built-in
     * backends (LOCAL, S3, AZURE_BLOB, GCS).
     */
    public FileClient(ConductorClient client) {
        this(client, new FileClientProperties(), createDefaultFileStorageBackends());
    }

    /**
     * Full-args constructor.
     *
     * @param properties                       {@code null} to use defaults
     * @param fileStorageBackendsByStorageType {@code null} to use the built-in backends
     */
    public FileClient(ConductorClient client, FileClientProperties properties, Map<StorageType, FileStorageBackend> fileStorageBackendsByStorageType) {
        this.client = client;
        this.properties = properties == null ? new FileClientProperties() : properties;
        this.fileStorageBackendsByStorageType = fileStorageBackendsByStorageType == null ? createDefaultFileStorageBackends() : fileStorageBackendsByStorageType;
    }

    // --- FileUploader (developer-facing) ---

    /**
     * Uploads a local file with the given {@link FileUploadOptions}.
     *
     * <p>{@code workflowId} is required; pass {@code null} only if you are prepared to see a
     * {@link FileStorageException}. When called from inside a worker and {@code options.taskId}
     * is null, {@code taskId} is auto-filled from the active {@link TaskContext}.
     *
     * @throws FileStorageException if {@code workflowId} is null, if no backend is registered for
     *                              the server's storage type, or if any step of the upload fails
     */
    public FileHandler upload(String workflowId, Path localFile, FileUploadOptions options) {
        if (workflowId == null) {
            throw new FileStorageException("workflowId is required");
        }
        try {
            fillDefaults(options, localFile);
            long fileSize = Files.size(localFile);
            FileUploadRequest request = FileHandlerConverter.toFileUploadRequest(workflowId, options);

            FileUploadResponse response = createFileOnServer(request);

            FileStorageBackend storageBackend = fileStorageBackendsByStorageType.get(response.getStorageType());
            if (storageBackend == null) {
                throw new FileStorageException("Server uses " + response.getStorageType()
                        + " but SDK only supports: " + fileStorageBackendsByStorageType.keySet());
            }

            if (options.isMultipart() && storageBackend.hasMultipartSupport()) {
                uploadMultipart(response.getFileHandleId(), response.getStorageType(), localFile);
            } else {
                storageBackend.upload(response.getUploadUrl(), localFile);
                confirmUpload(response.getFileHandleId());
            }

            return FileHandlerConverter.toManagedFileHandler(response, new WorkflowFileClient(this, workflowId), localFile, fileSize);
        } catch (IOException e) {
            throw new FileStorageException("Upload failed for: " + localFile, e);
        }
    }

    /**
     * Buffers the stream to a temp file, then delegates to {@link #upload(String, Path, FileUploadOptions)}.
     */
    public FileHandler upload(String workflowId, InputStream inputStream, FileUploadOptions options) {
        try {
            Path temp = Files.createTempFile("conductor-upload-", ".tmp");
            Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            return upload(workflowId, temp, options);
        } catch (IOException e) {
            throw new FileStorageException("Failed to write InputStream to temp file", e);
        }
    }

    public FileHandler upload(String workflowId, Path localFile) {
        return upload(workflowId, localFile, new FileUploadOptions());
    }

    public FileHandler upload(String workflowId, InputStream inputStream) {
        return upload(workflowId, inputStream, new FileUploadOptions());
    }

    /**
     * Populates unset options from the local file and active {@link TaskContext}. Mutates
     * {@code options} in place — SDK-internal helper only.
     */
    private static void fillDefaults(FileUploadOptions options, Path localFile) {
        if (options.getFileName() == null) {
            options.setFileName(localFile.getFileName().toString());
        }
        if (options.getContentType() == null) {
            options.setContentType("application/octet-stream");
        }
        if (options.getTaskId() == null) {
            TaskContext ctx = TaskContext.get();
            if (ctx != null) {
                options.setTaskId(ctx.getTaskId());
            }
        }
    }

    // --- SDK-internal (public for cross-package access, not developer-facing) ---

    /**
     * Downloads the content for a file to {@code destination}. Fetches a fresh presigned
     * download URL from the server on each call and delegates the byte transfer to the
     * {@link FileStorageBackend} registered for {@code storageType}.
     */
    public void download(String workflowId, String fileHandleId, StorageType storageType, Path destination) {
        FileDownloadUrlResponse urlResponse = getDownloadUrl(workflowId, fileHandleId);
        try {
            Files.createDirectories(destination.getParent());
        } catch (IOException e) {
            throw new FileStorageException("Failed to create cache directory", e);
        }
        fileStorageBackendsByStorageType.get(storageType).download(urlResponse.getDownloadUrl(), destination);
    }

    /**
     * Fetches the {@link FileHandle} metadata via {@code GET /api/files/{fileId}}.
     */
    public FileHandle getMetadata(String fileHandleId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/files/{fileId}")
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .build();
        return client.execute(request, FILE_HANDLE_TYPE).getData();
    }

    public int getRetryCount() {
        return properties.getRetryCount();
    }

    public String getCacheDirectory() {
        return properties.getLocalCacheDirectory();
    }

    /**
     * Confirms that byte transfer is complete via
     * {@code POST /api/files/{fileId}/upload-complete}. The server verifies the object on the
     * backend, reads {@code contentHash} and actual size, and transitions the record to
     * {@code UPLOADED}.
     */
    void confirmUpload(String fileHandleId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/files/{fileId}/upload-complete")
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .build();
        client.execute(request, FILE_UPLOAD_COMPLETE_RESPONSE_TYPE);
    }

    // --- Private ---

    private FileUploadResponse createFileOnServer(FileUploadRequest uploadRequest) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/files")
                .body(uploadRequest)
                .build();
        return client.execute(request, FILE_UPLOAD_RESPONSE_TYPE).getData();
    }

    private FileDownloadUrlResponse getDownloadUrl(String workflowId, String fileHandleId) {
        if (workflowId == null) {
            throw new FileStorageException("WorkflowInstanceId is null");
        }
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/files/{workflowId}/{fileId}/download-url")
                .addPathParam("workflowId", workflowId)
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .build();
        return client.execute(request, FILE_DOWNLOAD_URL_RESPONSE_TYPE).getData();
    }

    private void uploadMultipart(String fileHandleId, StorageType storageType, Path localFile) {
        try {
            MultipartInitResponse init = initiateMultipartUpload(fileHandleId);
            String uploadId = init.getUploadId();
            long partSize = properties.getMultipartPartSize();
            long fileSize = Files.size(localFile);
            int totalParts = (int) Math.ceil((double) fileSize / partSize);

            List<String> partETags = new ArrayList<>();

            for (int part = 1; part <= totalParts; part++) {
                long offset = (long) (part - 1) * partSize;
                long length = Math.min(partSize, fileSize - offset);

                String url = getPartUploadUrl(fileHandleId, uploadId, part);
                String etag = fileStorageBackendsByStorageType.get(storageType).uploadPart(url, localFile, offset, length);
                partETags.add(etag);
            }

            completeMultipartUpload(fileHandleId, uploadId, partETags);
        } catch (IOException e) {
            throw new FileStorageException("Multipart upload failed for: " + fileHandleId, e);
        }
    }

    private MultipartInitResponse initiateMultipartUpload(String fileHandleId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/files/{fileId}/multipart")
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .build();
        return client.execute(request, MULTIPART_INIT_RESPONSE_TYPE).getData();
    }

    private String getPartUploadUrl(String fileHandleId, String uploadId, int partNumber) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/files/{fileId}/multipart/{uploadId}/part/{partNumber}")
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .addPathParam("uploadId", uploadId)
                .addPathParam("partNumber", String.valueOf(partNumber))
                .build();
        return client.execute(request, FILE_UPLOAD_URL_RESPONSE_TYPE).getData().getUploadUrl();
    }

    private void completeMultipartUpload(String fileHandleId, String uploadId,
                                         List<String> partETags) {
        MultipartCompleteRequest body = new MultipartCompleteRequest();
        body.setPartETags(partETags);
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/files/{fileId}/multipart/{uploadId}/complete")
                .addPathParam("fileId", FileHandler.toFileId(fileHandleId))
                .addPathParam("uploadId", uploadId)
                .body(body)
                .build();
        client.execute(request, FILE_UPLOAD_COMPLETE_RESPONSE_TYPE);
    }

    private static Map<StorageType, FileStorageBackend> createDefaultFileStorageBackends() {
        return Map.ofEntries(
                Map.entry(StorageType.LOCAL, new LocalFileStorageBackend()),
                Map.entry(StorageType.S3, new S3FileStorageBackend()),
                Map.entry(StorageType.AZURE_BLOB, new AzureFileStorageBackend()),
                Map.entry(StorageType.GCS, new GcsFileStorageBackend())
        );
    }

    public static Builder builder(ConductorClient client) {
        return new Builder(client);
    }

    /**
     * Builder for configuring a {@link FileClient}. The built-in backends (LOCAL, S3, AZURE_BLOB,
     * GCS) are always included; a backend registered via {@link #addStorageBackend} overrides
     * the built-in backend for the same {@link StorageType}.
     */
    public static class Builder {

        private final ConductorClient client;
        private FileClientProperties properties;
        private final Map<StorageType, FileStorageBackend> backends = new EnumMap<>(StorageType.class);

        private Builder(ConductorClient client) {
            this.client = client;
        }

        public Builder properties(FileClientProperties properties) {
            this.properties = properties;
            return this;
        }

        public Builder addStorageBackend(FileStorageBackend backend) {
            this.backends.put(backend.getStorageType(), backend);
            return this;
        }

        public FileClient build() {
            Map<StorageType, FileStorageBackend> resolved = new EnumMap<>(createDefaultFileStorageBackends());
            resolved.putAll(backends);
            return new FileClient(client, properties, Map.copyOf(resolved));
        }
    }
}
