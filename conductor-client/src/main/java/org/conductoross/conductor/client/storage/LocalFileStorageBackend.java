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
package org.conductoross.conductor.client.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.conductoross.conductor.client.model.file.StorageType;
import org.conductoross.conductor.sdk.file.FileStorageBackend;
import org.conductoross.conductor.sdk.file.FileStorageException;

/**
 * {@link FileStorageBackend} for the server-local filesystem (development / zero-infra mode).
 * Does NOT support multipart — {@link #hasMultipartSupport()} returns {@code false}.
 */
public class LocalFileStorageBackend implements FileStorageBackend {

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public void upload(String url, Path localFile) {
        try {
            Path dest = Path.of(URI.create(url));
            Files.createDirectories(dest.getParent());
            Files.copy(localFile, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Local upload failed: " + localFile + " → " + url, e);
        }
    }

    @Override
    public void upload(String url, InputStream inputStream, long contentLength) {
        try {
            Path dest = Path.of(URI.create(url));
            Files.createDirectories(dest.getParent());
            Files.copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Local upload from stream failed: " + url, e);
        }
    }

    @Override
    public void download(String url, Path destination) {
        try {
            Path source = Path.of(URI.create(url));
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Local download failed: " + url, e);
        }
    }

    @Override
    public String uploadPart(String url, Path localFile, long offset, long length) {
        // Unsupported by design
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean hasMultipartSupport() {
        return false;
    }
}
