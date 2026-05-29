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

/**
 * Developer-facing upload API. Implemented by {@link org.conductoross.conductor.client.FileClient}.
 * Use for explicit uploads — e.g. before starting a workflow, or inside a task when you want to
 * control upload timing.
 *
 * <p>All overloads return a {@link FileHandler} whose {@link FileHandler#getFileHandleId()} is
 * populated with the prefixed handle {@code conductor://file/<fileId>}. The {@link InputStream}
 * overloads stream the content to a temporary file first, then delegate to the {@link Path}
 * overload.
 *
 * <p>When called from within a worker, {@code workflowId} and {@code taskId} are injected
 * automatically from the active task context. Use {@link FileUploadOptions} to override them or
 * to supply additional metadata when uploading outside a worker.
 */
public interface FileUploader {

    FileHandler upload(Path localFile);

    FileHandler upload(InputStream inputStream);

    /** Upload from a local file with the given options. */
    FileHandler upload(Path localFile, FileUploadOptions options);

    /** Upload from an {@link InputStream} with the given options. */
    FileHandler upload(InputStream inputStream, FileUploadOptions options);
}
