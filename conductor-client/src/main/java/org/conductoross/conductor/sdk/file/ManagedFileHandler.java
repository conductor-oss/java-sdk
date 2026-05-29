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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileHandle;
import org.conductoross.conductor.client.model.file.StorageType;

/**
 * {@link FileHandler} for a file already registered on the server. Holds the prefixed
 * {@code fileHandleId} and a reference to a {@link FileClient}; fetches metadata lazily on
 * first accessor call, and downloads content lazily on first {@link #getInputStream()} call.
 *
 * <p>Thread-safe: concurrent {@code getInputStream()} calls serialize through a
 * {@link ReentrantLock} so only one download runs per instance. Downloaded content is cached
 * under {@link org.conductoross.conductor.client.FileClientProperties#getLocalCacheDirectory()}
 * at {@code <cacheDir>/<fileId>_<fileName>}. Two {@code ManagedFileHandler}s for the same file
 * on the same worker node will therefore skip the re-download.
 */
public class ManagedFileHandler implements FileHandler {

    private final String fileHandleId;
    private String fileName;
    private String contentType;
    private long fileSize;
    private StorageType storageType;
    private Path localPath;
    private FileDownloadStatus downloadStatus = FileDownloadStatus.NOT_STARTED;
    private final WorkflowFileClient workflowFileClient;
    private final ReentrantLock downloadLock = new ReentrantLock();

    public ManagedFileHandler(String fileHandleId, WorkflowFileClient workflowFileClient) {
        this.fileHandleId = fileHandleId;
        this.workflowFileClient = workflowFileClient;
    }

    @Override
    public String getFileHandleId() { return fileHandleId; }

    @Override
    public InputStream getInputStream() {
        ensureDownloaded();
        try {
            return Files.newInputStream(localPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to open cached file: " + localPath, e);
        }
    }

    @Override
    public String getFileName() {
        ensureMetadataLoaded();
        return fileName;
    }

    @Override
    public String getContentType() {
        ensureMetadataLoaded();
        return contentType;
    }

    @Override
    public long getFileSize() {
        ensureMetadataLoaded();
        return fileSize;
    }

    /** Fetches the {@link FileHandle} from the server on first call; a no-op thereafter. */
    private void ensureMetadataLoaded() {
        if (fileName != null) {
            return;
        }
        FileHandle metadata = workflowFileClient.getMetadata(fileHandleId);
        this.fileName = metadata.getFileName();
        this.contentType = metadata.getContentType();
        this.fileSize = metadata.getFileSize();
        this.storageType = metadata.getStorageType();
    }

    /**
     * Downloads content to the cache on first call. Concurrent callers block on
     * {@link #downloadLock}; only the first acquires the lock and performs the download. If
     * another {@code ManagedFileHandler} has already cached the file at the predictable path,
     * the download is skipped.
     */
    private void ensureDownloaded() {
        if (downloadStatus == FileDownloadStatus.DOWNLOADED && localPath != null
                && Files.exists(localPath)) {
            return;
        }

        downloadLock.lock();
        try {
            if (downloadStatus == FileDownloadStatus.DOWNLOADED && localPath != null
                    && Files.exists(localPath)) {
                return;
            }
            downloadStatus = FileDownloadStatus.DOWNLOADING;

            ensureMetadataLoaded();

            Path destination = getCachePath();
            if (Files.exists(destination)) {
                this.localPath = destination;
                this.downloadStatus = FileDownloadStatus.DOWNLOADED;
                return;
            }

            int maxRetries = workflowFileClient.getRetryCount();
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    workflowFileClient.download(fileHandleId, storageType, destination);
                    this.localPath = destination;
                    this.downloadStatus = FileDownloadStatus.DOWNLOADED;
                    return;
                } catch (Exception e) {
                    if (attempt == maxRetries) throw e;
                }
            }
        } catch (Exception e) {
            this.downloadStatus = FileDownloadStatus.FAILED;
            throw new FileStorageException("Download failed for " + fileHandleId, e);
        } finally {
            downloadLock.unlock();
        }
    }

    private Path getCachePath() {
        String fileId = FileHandler.toFileId(fileHandleId);
        return Path.of(workflowFileClient.getCacheDirectory(), fileId + "_" + fileName);
    }

    void setFileName(String fileName) { this.fileName = fileName; }
    void setContentType(String contentType) { this.contentType = contentType; }
    void setFileSize(long fileSize) { this.fileSize = fileSize; }
    void setStorageType(StorageType storageType) { this.storageType = storageType; }
    void setLocalPath(Path localPath) { this.localPath = localPath; }
}
