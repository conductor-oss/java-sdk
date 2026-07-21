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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class LocalFileTransferAdapter implements FileTransferAdapter {

    @Override
    public String storageType() { return "LOCAL"; }

    @Override
    public void upload(String signedUrl, Path source) throws IOException {
        Path destination = filePath(signedUrl, "upload");
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Local file upload failed");
        }
    }

    @Override
    public void download(String signedUrl, Path destination) throws IOException {
        Path source = filePath(signedUrl, "download");
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Local file download failed");
        }
    }

    @Override
    public boolean supportsMultipart() { return false; }

    @Override
    public String uploadPart(
            String signedUrl, Path source, long offset, long length, int partNumber)
            throws IOException {
        throw new NonRetryableFileTransferException("Local multipart upload is not supported");
    }

    private static Path filePath(String value, String operation) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                throw new NonRetryableFileTransferException(
                        "Local " + operation + " URL must use the file scheme");
            }
            return Path.of(uri);
        } catch (RuntimeException e) {
            throw new NonRetryableFileTransferException(
                    "Invalid local " + operation + " URL");
        }
    }
}
