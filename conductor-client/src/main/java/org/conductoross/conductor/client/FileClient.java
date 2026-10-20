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
import java.io.InterruptedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.conductoross.conductor.client.model.file.FileDownloadUrlResponse;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.client.model.file.FileUploadCompleteResponse;
import org.conductoross.conductor.client.model.file.FileUploadRequest;
import org.conductoross.conductor.client.model.file.FileUploadResponse;
import org.conductoross.conductor.client.model.file.FileUploadStatus;
import org.conductoross.conductor.client.model.file.FileUploadUrlResponse;
import org.conductoross.conductor.client.model.file.MultipartCompleteRequest;
import org.conductoross.conductor.client.model.file.MultipartInitResponse;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.FileUploadOptions;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;

import io.orkes.conductor.client.http.ApiException;

import okhttp3.OkHttpClient;
import tools.jackson.core.type.TypeReference;

/**
 * Workflow-scoped client for Conductor file storage.
 *
 * <p>File content is represented publicly only by an opaque
 * {@code conductor://file/<id>} string. This class owns the complete lifecycle: server records,
 * signed URL refresh, retries, multipart orchestration, completion, and safe downloads. Internal
 * transfer adapters perform a single provider-specific request and never retry independently.
 */
public final class FileClient {

    static final String FILE_HANDLE_PREFIX = "conductor://file/";

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_MULTIPART_PARTS = 10_000;
    private static final Pattern FILE_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,255}");

    private static final TypeReference<FileUploadResponse> FILE_UPLOAD_RESPONSE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<FileUploadUrlResponse> FILE_UPLOAD_URL_RESPONSE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<FileUploadCompleteResponse> FILE_UPLOAD_COMPLETE_RESPONSE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<FileDownloadUrlResponse> FILE_DOWNLOAD_URL_RESPONSE_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<FileMetadata> FILE_METADATA_TYPE = new TypeReference<>() {};
    private static final TypeReference<MultipartInitResponse> MULTIPART_INIT_RESPONSE_TYPE =
            new TypeReference<>() {};

    private final ConductorClient client;
    private final FileClientProperties properties;
    private final Map<String, FileTransferAdapter> adapters;
    private final FileTransferAdapter genericHttpAdapter;

    public FileClient(ConductorClient client) {
        this(client, new FileClientProperties(), new OkHttpClient());
    }

    public FileClient(ConductorClient client, FileClientProperties properties) {
        this(client, properties, new OkHttpClient());
    }

    /**
     * Creates a client with a separately configured HTTP transport for signed URLs.
     *
     * <p>Application and network interceptors, authenticators, cookies, cache, event listeners,
     * and redirects are removed from the supplied transport before it is used. Timeouts, proxy,
     * DNS, TLS, and connection-pool settings are retained.
     */
    public FileClient(
            ConductorClient client,
            FileClientProperties properties,
            OkHttpClient transferHttpClient) {
        this.client = Objects.requireNonNull(client, "client is required");
        this.properties = properties == null ? new FileClientProperties() : properties;
        OkHttpClient rawClient = transferHttpClient == null ? new OkHttpClient() : transferHttpClient;

        List<FileTransferAdapter> builtIns = List.of(
                new S3FileTransferAdapter(rawClient),
                new AzureFileTransferAdapter(rawClient),
                new GcsFileTransferAdapter(rawClient),
                new LocalFileTransferAdapter());
        Map<String, FileTransferAdapter> byType = new HashMap<>();
        builtIns.forEach(adapter -> byType.put(adapter.storageType(), adapter));
        this.adapters = Map.copyOf(byType);
        this.genericHttpAdapter = new GenericHttpFileTransferAdapter(rawClient);
        validateProperties();
    }

    public String upload(String workflowId, Path source) {
        return upload(workflowId, source, new FileUploadOptions());
    }

    public String upload(String workflowId, Path source, FileUploadOptions options) {
        validateWorkflowId(workflowId);
        validateProperties();
        validateSource(source);
        stopIfInterrupted("File upload");

        FileUploadOptions resolved = resolveOptions(source, options);
        long fileSize;
        try {
            fileSize = Files.size(source);
        } catch (IOException e) {
            throw new FileStorageException("Unable to read upload source size", e);
        }

        FileUploadResponse created;
        try {
            created = createFileOnServer(workflowId, resolved);
        } catch (RuntimeException e) {
            preserveInterruption(e);
            throw e;
        }
        String fileHandleId = requireHandle(created.getFileHandleId());
        if (created.getWorkflowId() != null && !workflowId.equals(created.getWorkflowId())) {
            throw new FileStorageException("The server returned a file owned by a different workflow");
        }

        FileTransferAdapter adapter = selectAdapter(created.getStorageType(), created.getUploadUrl());
        if (fileSize > properties.getMultipartThreshold() && adapter.supportsMultipart()) {
            uploadMultipart(workflowId, fileHandleId, source, fileSize, adapter);
        } else {
            uploadSingle(workflowId, fileHandleId, created.getUploadUrl(), source, adapter);
            completeUpload(workflowId, fileHandleId);
        }
        return fileHandleId;
    }

    public String upload(String workflowId, InputStream source, FileUploadOptions options) {
        validateWorkflowId(workflowId);
        validateProperties();
        stopIfInterrupted("File upload");
        if (source == null) {
            throw new FileStorageException("Upload source stream is required");
        }
        if (options == null || options.getFileName() == null) {
            throw new FileStorageException("A filename is required when uploading a stream");
        }
        validateFileName(options.getFileName());

        Path temporary = null;
        try {
            temporary = Files.createTempFile("conductor-upload-", ".tmp");
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            return upload(workflowId, temporary, copyOptions(options));
        } catch (IOException e) {
            preserveInterruption(e);
            throw new FileStorageException("Unable to buffer upload stream", e);
        } finally {
            deleteQuietly(temporary);
        }
    }

    public Path download(String workflowId, String fileHandleId, Path destination) {
        validateWorkflowId(workflowId);
        String handle = requireHandle(fileHandleId);
        validateProperties();
        Path target = validateDestination(destination);
        stopIfInterrupted("File download");

        Path temporary = null;
        try {
            Path parent = target.getParent();
            Files.createDirectories(parent);
            if (!Files.isWritable(parent)) {
                throw new FileStorageException("Download destination directory is not writable");
            }
            temporary = Files.createTempFile(parent, temporaryPrefix(target), ".part");

            FileMetadata metadata = getMetadata(workflowId, handle);
            Path transferTarget = temporary;
            runTransferWithRetries(
                    "File download",
                    attempt -> {
                        FileDownloadUrlResponse response = getDownloadUrl(workflowId, handle);
                        FileTransferAdapter adapter =
                                selectAdapter(metadata.getStorageType(), response.getDownloadUrl());
                        adapter.download(response.getDownloadUrl(), transferTarget);
                    });

            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new FileStorageException(
                        "Destination filesystem does not support atomic replacement", e);
            }
            temporary = null;
            return target;
        } catch (FileStorageException e) {
            throw e;
        } catch (IOException e) {
            preserveInterruption(e);
            throw new FileStorageException("File download failed", e);
        } finally {
            deleteQuietly(temporary);
        }
    }

    public FileMetadata getMetadata(String workflowId, String fileHandleId) {
        validateWorkflowId(workflowId);
        String handle = requireHandle(fileHandleId);
        stopIfInterrupted("File metadata request");
        return runServerReadWithRetries(
                "File metadata request", () -> getMetadataOnce(workflowId, handle));
    }

    private void uploadSingle(
            String workflowId,
            String fileHandleId,
            String initialUrl,
            Path source,
            FileTransferAdapter initialAdapter) {
        runTransferWithRetries(
                "File upload",
                attempt -> {
                    String signedUrl = initialUrl;
                    FileTransferAdapter adapter = initialAdapter;
                    if (attempt > 0) {
                        signedUrl = getUploadUrl(workflowId, fileHandleId).getUploadUrl();
                        adapter = selectAdapter(initialAdapter.storageType(), signedUrl);
                    }
                    adapter.upload(signedUrl, source);
                });
    }

    private void uploadMultipart(
            String workflowId,
            String fileHandleId,
            Path source,
            long fileSize,
            FileTransferAdapter adapter) {
        MultipartInitResponse initiated;
        try {
            initiated = initiateMultipartUpload(workflowId, fileHandleId);
        } catch (RuntimeException e) {
            preserveInterruption(e);
            throw e;
        }
        if (initiated == null || initiated.getUploadId() == null || initiated.getUploadId().isBlank()) {
            throw new FileStorageException("Server did not return a multipart upload ID");
        }
        String uploadId = initiated.getUploadId();
        boolean completed = false;
        try {
            long partSize = properties.getMultipartPartSize();
            long partCount = ((fileSize - 1) / partSize) + 1;
            if (partCount > MAX_MULTIPART_PARTS) {
                throw new FileStorageException(
                        "Multipart upload exceeds the maximum of " + MAX_MULTIPART_PARTS + " parts");
            }

            List<String> completionTokens = new ArrayList<>((int) partCount);
            for (int partNumber = 1; partNumber <= partCount; partNumber++) {
                long offset = (partNumber - 1L) * partSize;
                long length = Math.min(partSize, fileSize - offset);
                int currentPart = partNumber;
                completionTokens.add(runPartTransferWithRetries(
                        attempt -> {
                            String signedUrl = getPartUploadUrl(
                                    workflowId, fileHandleId, uploadId, currentPart);
                            return adapter.uploadPart(
                                    signedUrl, source, offset, length, currentPart);
                        }));
            }

            completeMultipartUpload(
                    workflowId, fileHandleId, uploadId, completionTokens);
            completed = true;
        } finally {
            if (!completed) {
                abortMultipartUpload(workflowId, fileHandleId, uploadId);
            }
        }
    }

    private FileUploadResponse createFileOnServer(
            String workflowId, FileUploadOptions options) {
        FileUploadRequest body = new FileUploadRequest();
        body.setWorkflowId(workflowId);
        body.setFileName(options.getFileName());
        body.setContentType(options.getContentType());
        body.setTaskId(options.getTaskId());
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/files")
                .body(body)
                .build();
        FileUploadResponse response = client.execute(request, FILE_UPLOAD_RESPONSE_TYPE).getData();
        if (response == null) {
            throw new FileStorageException("Server returned an empty file creation response");
        }
        return response;
    }

    private FileUploadUrlResponse getUploadUrl(String workflowId, String fileHandleId) {
        ConductorClientRequest request = scopedRequest(
                Method.GET, "/files/{workflowId}/{fileId}/upload-url", workflowId, fileHandleId)
                .build();
        FileUploadUrlResponse response =
                client.execute(request, FILE_UPLOAD_URL_RESPONSE_TYPE).getData();
        if (response == null || response.getUploadUrl() == null) {
            throw new FileStorageException("Server returned an empty upload URL response");
        }
        return response;
    }

    private FileDownloadUrlResponse getDownloadUrl(String workflowId, String fileHandleId) {
        ConductorClientRequest request = scopedRequest(
                Method.GET, "/files/{workflowId}/{fileId}/download-url", workflowId, fileHandleId)
                .build();
        FileDownloadUrlResponse response =
                client.execute(request, FILE_DOWNLOAD_URL_RESPONSE_TYPE).getData();
        if (response == null || response.getDownloadUrl() == null) {
            throw new FileStorageException("Server returned an empty download URL response");
        }
        return response;
    }

    private FileMetadata getMetadataOnce(String workflowId, String fileHandleId) {
        ConductorClientRequest request = scopedRequest(
                Method.GET, "/files/{workflowId}/{fileId}", workflowId, fileHandleId)
                .build();
        FileMetadata metadata = client.execute(request, FILE_METADATA_TYPE).getData();
        if (metadata == null) {
            throw new FileStorageException("Server returned empty file metadata");
        }
        return metadata;
    }

    private void completeUpload(String workflowId, String fileHandleId) {
        completeWithReconciliation(
                workflowId,
                fileHandleId,
                () -> {
                    ConductorClientRequest request = scopedRequest(
                            Method.POST,
                            "/files/{workflowId}/{fileId}/upload-complete",
                            workflowId,
                            fileHandleId)
                            .build();
                    client.execute(request, FILE_UPLOAD_COMPLETE_RESPONSE_TYPE);
                });
    }

    private MultipartInitResponse initiateMultipartUpload(
            String workflowId, String fileHandleId) {
        ConductorClientRequest request = scopedRequest(
                Method.POST,
                "/files/{workflowId}/{fileId}/multipart",
                workflowId,
                fileHandleId)
                .build();
        return client.execute(request, MULTIPART_INIT_RESPONSE_TYPE).getData();
    }

    private String getPartUploadUrl(
            String workflowId, String fileHandleId, String uploadId, int partNumber) {
        ConductorClientRequest request = scopedRequest(
                Method.GET,
                "/files/{workflowId}/{fileId}/multipart/{uploadId}/part/{partNumber}",
                workflowId,
                fileHandleId)
                .addPathParam("uploadId", uploadId)
                .addPathParam("partNumber", partNumber)
                .build();
        FileUploadUrlResponse response =
                client.execute(request, FILE_UPLOAD_URL_RESPONSE_TYPE).getData();
        if (response == null || response.getUploadUrl() == null) {
            throw new FileStorageException("Server returned an empty multipart upload URL response");
        }
        return response.getUploadUrl();
    }

    private void completeMultipartUpload(
            String workflowId,
            String fileHandleId,
            String uploadId,
            List<String> completionTokens) {
        MultipartCompleteRequest body = new MultipartCompleteRequest();
        body.setPartETags(completionTokens);
        completeWithReconciliation(
                workflowId,
                fileHandleId,
                () -> {
                    ConductorClientRequest request = scopedRequest(
                            Method.POST,
                            "/files/{workflowId}/{fileId}/multipart/{uploadId}/complete",
                            workflowId,
                            fileHandleId)
                            .addPathParam("uploadId", uploadId)
                            .body(body)
                            .build();
                    client.execute(request, FILE_UPLOAD_COMPLETE_RESPONSE_TYPE);
                });
    }

    private void abortMultipartUpload(
            String workflowId, String fileHandleId, String uploadId) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        try {
            ConductorClientRequest request = scopedRequest(
                    Method.DELETE,
                    "/files/{workflowId}/{fileId}/multipart/{uploadId}",
                    workflowId,
                    fileHandleId)
                    .addPathParam("uploadId", uploadId)
                    .build();
            client.execute(request);
        } catch (RuntimeException ignored) {
            preserveInterruption(ignored);
            // Best effort: retain the original multipart failure.
        }
    }

    private void completeWithReconciliation(
            String workflowId, String fileHandleId, ServerMutation mutation) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= properties.getRetryCount(); attempt++) {
            stopIfInterrupted("File upload completion");
            try {
                mutation.run();
                return;
            } catch (RuntimeException e) {
                if (preserveInterruption(e)) {
                    throw new FileStorageException("File upload completion interrupted", e);
                }
                if (!isRetryableServerFailure(e)) {
                    throw e;
                }
                last = e;
                if (isAlreadyUploaded(workflowId, fileHandleId)) {
                    return;
                }
            }
        }
        throw new FileStorageException("Unable to confirm file upload completion", last);
    }

    private boolean isAlreadyUploaded(String workflowId, String fileHandleId) {
        try {
            FileMetadata metadata = getMetadataOnce(workflowId, fileHandleId);
            return metadata.getUploadStatus() == FileUploadStatus.UPLOADED;
        } catch (RuntimeException ignored) {
            if (preserveInterruption(ignored)) {
                throw ignored;
            }
            return false;
        }
    }

    private void runTransferWithRetries(String operation, TransferAttempt transfer) {
        Exception last = null;
        for (int attempt = 0; attempt <= properties.getRetryCount(); attempt++) {
            stopIfInterrupted(operation);
            try {
                transfer.run(attempt);
                return;
            } catch (IOException e) {
                if (preserveInterruption(e)
                        || !isRetryableTransferFailure(e)
                        || attempt == properties.getRetryCount()) {
                    throw new FileStorageException(operation + " failed", e);
                }
                last = e;
            } catch (RuntimeException e) {
                if (preserveInterruption(e)) {
                    throw new FileStorageException(operation + " interrupted", e);
                }
                if (!isRetryableServerFailure(e) || attempt == properties.getRetryCount()) {
                    throw e;
                }
                last = e;
            }
        }
        throw new FileStorageException(operation + " failed", last);
    }

    private String runPartTransferWithRetries(PartTransferAttempt transfer) {
        Exception last = null;
        for (int attempt = 0; attempt <= properties.getRetryCount(); attempt++) {
            stopIfInterrupted("Multipart upload");
            try {
                String token = transfer.run(attempt);
                if (token == null || token.isBlank()) {
                    throw new IOException("Multipart part did not return a completion token");
                }
                return token;
            } catch (IOException e) {
                if (preserveInterruption(e)
                        || !isRetryableTransferFailure(e)
                        || attempt == properties.getRetryCount()) {
                    throw new FileStorageException("Multipart part upload failed", e);
                }
                last = e;
            } catch (RuntimeException e) {
                if (preserveInterruption(e)) {
                    throw new FileStorageException("Multipart upload interrupted", e);
                }
                if (!isRetryableServerFailure(e) || attempt == properties.getRetryCount()) {
                    throw e;
                }
                last = e;
            }
        }
        throw new FileStorageException("Multipart part upload failed", last);
    }

    private <T> T runServerReadWithRetries(String operation, ServerRead<T> read) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= properties.getRetryCount(); attempt++) {
            stopIfInterrupted(operation);
            try {
                return read.run();
            } catch (RuntimeException e) {
                if (preserveInterruption(e)) {
                    throw new FileStorageException(operation + " interrupted", e);
                }
                if (!isRetryableServerFailure(e) || attempt == properties.getRetryCount()) {
                    throw e;
                }
                last = e;
            }
        }
        throw new FileStorageException(operation + " failed", last);
    }

    private FileTransferAdapter selectAdapter(String storageType, String signedUrl) {
        if (storageType != null) {
            FileTransferAdapter adapter =
                    adapters.get(storageType.trim().toUpperCase(Locale.ROOT));
            if (adapter != null) {
                return adapter;
            }
        }
        if (SignedUrlHttpTransfer.isHttpUrl(signedUrl)) {
            return genericHttpAdapter;
        }
        throw new FileStorageException(
                "No transfer adapter is available for storage type " + safeStorageType(storageType));
    }

    private static String safeStorageType(String storageType) {
        return storageType == null || storageType.isBlank() ? "<missing>" : storageType;
    }

    private static boolean isRetryableTransferFailure(IOException error) {
        if (error instanceof NonRetryableFileTransferException) {
            return false;
        }
        if (error instanceof FileTransferException transferError) {
            int status = transferError.statusCode();
            return status == 401
                    || status == 403
                    || status == 408
                    || status == 425
                    || status == 429
                    || status >= 500;
        }
        return true;
    }

    private static boolean isRetryableServerFailure(RuntimeException error) {
        if (error instanceof FileStorageException) {
            return false;
        }
        if (error instanceof ApiException apiException) {
            int status = apiException.getStatusCode();
            return status == 0 || status == 408 || status == 425 || status == 429 || status >= 500;
        }
        return false;
    }

    private static boolean preserveInterruption(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException
                    || current instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
                return true;
            }
        }
        return Thread.currentThread().isInterrupted();
    }

    private static void stopIfInterrupted(String operation) {
        if (Thread.currentThread().isInterrupted()) {
            throw new FileStorageException(operation + " interrupted", new InterruptedIOException());
        }
    }

    private static ConductorClientRequest.Builder scopedRequest(
            Method method, String path, String workflowId, String fileHandleId) {
        return ConductorClientRequest.builder()
                .method(method)
                .path(path)
                .addPathParam("workflowId", workflowId)
                .addPathParam("fileId", toFileId(fileHandleId));
    }

    private FileUploadOptions resolveOptions(Path source, FileUploadOptions options) {
        FileUploadOptions resolved = copyOptions(options == null ? new FileUploadOptions() : options);
        if (resolved.getFileName() == null) {
            Path fileName = source.getFileName();
            if (fileName == null) {
                throw new FileStorageException("Upload source must have a filename");
            }
            resolved.setFileName(fileName.toString());
        }
        validateFileName(resolved.getFileName());
        if (resolved.getContentType() == null || resolved.getContentType().isBlank()) {
            resolved.setContentType("application/octet-stream");
        }
        return resolved;
    }

    private static FileUploadOptions copyOptions(FileUploadOptions source) {
        return new FileUploadOptions()
                .setFileName(source.getFileName())
                .setContentType(source.getContentType())
                .setTaskId(source.getTaskId());
    }

    private void validateProperties() {
        if (properties.getRetryCount() < 0) {
            throw new FileStorageException("retryCount must not be negative");
        }
        if (properties.getMultipartPartSize() <= 0) {
            throw new FileStorageException("multipartPartSize must be greater than zero");
        }
        if (properties.getMultipartThreshold() < 0) {
            throw new FileStorageException("multipartThreshold must not be negative");
        }
    }

    private static void validateWorkflowId(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            throw new FileStorageException("workflowId is required");
        }
    }

    private static void validateSource(Path source) {
        if (source == null) {
            throw new FileStorageException("Upload source is required");
        }
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new FileStorageException("Upload source must be a readable regular file");
        }
    }

    private static void validateFileName(String fileName) {
        if (fileName == null
                || fileName.isBlank()
                || fileName.length() > MAX_FILENAME_LENGTH
                || ".".equals(fileName)
                || "..".equals(fileName)
                || fileName.indexOf('/') >= 0
                || fileName.indexOf('\\') >= 0
                || fileName.matches(".*[\\x00-\\x1f:*?\"<>|].*")) {
            throw new FileStorageException("Filename is unsafe");
        }
    }

    private static Path validateDestination(Path destination) {
        if (destination == null) {
            throw new FileStorageException("Download destination is required");
        }
        Path target = destination.toAbsolutePath().normalize();
        if (target.getFileName() == null || target.getParent() == null) {
            throw new FileStorageException("Download destination must name a file");
        }
        if (Files.exists(target)
                && (Files.isDirectory(target)
                        || Files.isSymbolicLink(target)
                        || !Files.isRegularFile(target))) {
            throw new FileStorageException("Download destination must name a regular file");
        }
        return target;
    }

    private static String temporaryPrefix(Path destination) {
        String prefix = "." + destination.getFileName() + ".";
        return prefix.length() >= 3 ? prefix : ".file.";
    }

    private static String requireHandle(String fileHandleId) {
        toFileId(fileHandleId);
        return fileHandleId;
    }

    private static String toFileId(String fileHandleId) {
        if (fileHandleId == null || !fileHandleId.startsWith(FILE_HANDLE_PREFIX)) {
            throw new FileStorageException("Malformed file handle");
        }
        String fileId = fileHandleId.substring(FILE_HANDLE_PREFIX.length());
        if (!FILE_ID_PATTERN.matcher(fileId).matches()) {
            throw new FileStorageException("Malformed file handle");
        }
        return fileId;
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup must not hide the primary failure.
        }
    }

    @FunctionalInterface
    private interface TransferAttempt {
        void run(int attempt) throws IOException;
    }

    @FunctionalInterface
    private interface PartTransferAttempt {
        String run(int attempt) throws IOException;
    }

    @FunctionalInterface
    private interface ServerMutation {
        void run();
    }

    @FunctionalInterface
    private interface ServerRead<T> {
        T run();
    }
}
