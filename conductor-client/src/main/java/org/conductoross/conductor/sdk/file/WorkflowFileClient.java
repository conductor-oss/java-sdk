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

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileHandle;
import org.conductoross.conductor.client.model.file.StorageType;

/**
 * Decorator for {@link FileClient} that exposes the same file operations while keeping the
 * implementation behind a simpler SDK-facing type.
 */
public class WorkflowFileClient implements FileUploader  {

    private final FileClient delegate;
    private final String workflowId;

    public WorkflowFileClient(FileClient delegate, String workflowId) {
        this.delegate = delegate;
        this.workflowId = workflowId;
    }

    public FileHandler upload(Path localFile, FileUploadOptions options) {
        return delegate.upload(workflowId, localFile, options);
    }

    public FileHandler upload(InputStream inputStream, FileUploadOptions options) {
        return delegate.upload(workflowId, inputStream, options);
    }

    public FileHandler upload(Path localFile) {
        return delegate.upload(workflowId, localFile);
    }

    public FileHandler upload(InputStream inputStream) {
        return delegate.upload(workflowId, inputStream);
    }

    public void download(String fileHandleId, StorageType storageType, Path destination) {
        delegate.download(workflowId, fileHandleId, storageType, destination);
    }

    public FileHandle getMetadata(String fileHandleId) {
        return delegate.getMetadata(fileHandleId);
    }

    public int getRetryCount() {
        return delegate.getRetryCount();
    }

    public String getCacheDirectory() {
        return delegate.getCacheDirectory();
    }
}
