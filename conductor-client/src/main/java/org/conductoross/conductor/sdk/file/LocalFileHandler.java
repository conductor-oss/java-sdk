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

import org.conductoross.conductor.client.FileClient;

/**
 * {@link FileHandler} wrapping a local file that has not been uploaded.
 * {@link #getFileHandleId()} returns {@code null}. Returned by
 * {@link FileHandler#fromLocalFile(Path)}; consumed by
 * {@link FileClient#upload(String, Path)} or by
 * {@code TaskRunner}'s auto-upload interception on task completion.
 */
public class LocalFileHandler implements FileHandler {

    private final Path path;
    private final String contentType;

    LocalFileHandler(Path path, String contentType) {
        this.path = path;
        this.contentType = contentType;
    }

    @Override
    public String getFileHandleId() { return null; }

    @Override
    public InputStream getInputStream() {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new FileStorageException("Failed to open local file: " + path, e);
        }
    }

    @Override
    public String getFileName() { return path.getFileName().toString(); }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public long getFileSize() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new FileStorageException("Failed to get file size: " + path, e);
        }
    }

    public Path getPath() { return path; }
}
