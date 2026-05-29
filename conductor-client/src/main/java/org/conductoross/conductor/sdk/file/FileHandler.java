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
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Abstraction over a file passed into or out of a Conductor worker.
 *
 * <p>A {@code FileHandler} is used in two directions:
 * <ul>
 *   <li><b>Input</b> — a file referenced by a workflow task. The SDK resolves the incoming
 *       {@link #getFileHandleId() fileHandleId} string (see format below) into a
 *       {@link ManagedFileHandler} that downloads the content lazily on first read.</li>
 *   <li><b>Output</b> — a local file a worker wants to publish. Create one via
 *       {@link #fromLocalFile(Path)}; when the worker returns, the task runner uploads the
 *       file and substitutes the resulting {@code fileHandleId} into the task output so
 *       downstream tasks can consume it.</li>
 * </ul>
 */
@JsonSerialize(using = FileHandlerSerializer.class)
@JsonDeserialize(using = FileHandlerDeserializer.class)
public interface FileHandler {

    /** The {@code conductor://file/} prefix. */
    public static final String PREFIX = "conductor://file/";

    /**
     * Returns the Conductor-assigned identifier for this file, or {@code null} if the file
     * has not been uploaded yet.
     *
     * <p>The id is an opaque string in the form
     * {@code conductor://file/<fileId>} — the {@code conductor://file/} prefix (see {@link #PREFIX})
     * is what lets the SDK distinguish a file reference from any other string value in task
     * input/output maps and auto-convert it to a {@code FileHandler} for worker parameters.
     *
     * <p>Lifecycle:
     * <ul>
     *   <li>A {@link LocalFileHandler} built via {@link #fromLocalFile(Path)} returns
     *       {@code null} here until the task runner uploads it. Do not read this value before
     *       the worker returns — read it from the uploaded handler the runner produces (e.g.
     *       in tests) or rely on the runner substituting it into the task output automatically.</li>
     *   <li>A {@link ManagedFileHandler} (produced when a worker receives a file input, or
     *       returned by {@code FileClient.upload(...)}) always has a non-null id.</li>
     * </ul>
     *
     * <p>The returned value is safe to store in workflow input/output maps; any task that
     * consumes it will see a {@code FileHandler} injected by the SDK.
     *
     * @return the {@code conductor://file/<fileId>} identifier, or {@code null} for a local file
     * that has not yet been uploaded
     */
    String getFileHandleId();

    InputStream getInputStream();

    String getFileName();

    String getContentType();

    long getFileSize();

    default String getFileId() {
        return toFileId(getFileHandleId());
    }

    /**
     * Creates a handler for a local file to be uploaded when the worker returns.
     * Content type defaults to {@code application/octet-stream}.
     */
    static FileHandler fromLocalFile(Path path) {
        return new LocalFileHandler(path, "application/octet-stream");
    }

    /**
     * Creates a handler for a local file to be uploaded when the worker returns, using the
     * given content type.
     */
    static FileHandler fromLocalFile(Path path, String contentType) {
        return new LocalFileHandler(path, contentType);
    }

    /** Wraps a bare {@code fileId} with the prefix. Returns the input unchanged if already prefixed. */
    static String toFileHandleId(String fileId) {
        return fileId.startsWith(PREFIX) ? fileId : PREFIX + fileId;
    }

    /** Strips the prefix from a {@code fileHandleId}. Returns the input unchanged if the prefix is absent. */
    static String toFileId(String fileHandleId) {
        return fileHandleId.startsWith(PREFIX) ? fileHandleId.substring(PREFIX.length()) : fileHandleId;
    }

    /** {@code true} if {@code value} is a non-null String starting with the prefix. */
    static boolean isFileHandleId(Object value) {
        return value instanceof String s && s.startsWith(PREFIX);
    }

    /**
     * Extracts a {@code fileHandleId} from a task input value. Accepts:
     * <ul>
     *   <li>a {@link String} — returned unchanged;</li>
     *   <li>a {@link Map} carrying a {@code fileHandleId} entry — that entry's value is returned
     *       (the serialized JSON form produced by {@link FileHandlerSerializer});</li>
     * </ul>
     * and returns {@code null} for anything else. The returned string is not validated against
     * {@link #PREFIX}; callers should pass it to {@link #isFileHandleId} to confirm.
     */
    static String extractFileHandleId(Object value) {
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Map<?, ?> m && m.get("fileHandleId") instanceof String s) {
            return s;
        }
        return null;
    }
}
