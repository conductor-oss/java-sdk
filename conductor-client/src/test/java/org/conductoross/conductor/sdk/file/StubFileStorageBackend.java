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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.conductoross.conductor.client.model.file.StorageType;

public class StubFileStorageBackend implements FileStorageBackend {

    private final Map<String, byte[]> uploads = new ConcurrentHashMap<>();

    @Override
    public StorageType getStorageType() { return StorageType.LOCAL; }

    @Override
    public void upload(String url, Path localFile) {
        try {
            uploads.put(url, Files.readAllBytes(localFile));
        } catch (Exception e) {
            throw new FileStorageException("Stub upload failed", e);
        }
    }

    @Override
    public void upload(String url, InputStream inputStream, long contentLength) {
        try {
            uploads.put(url, inputStream.readAllBytes());
        } catch (Exception e) {
            throw new FileStorageException("Stub stream upload failed", e);
        }
    }

    @Override
    public void download(String url, Path destination) {
        try {
            byte[] data = uploads.get(url);
            if (data == null) {
                throw new FileStorageException("No data at: " + url);
            }
            Files.createDirectories(destination.getParent());
            Files.write(destination, data);
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Stub download failed", e);
        }
    }

    @Override
    public String uploadPart(String url, Path localFile, long offset, long length) {
        try {
            byte[] data = Files.readAllBytes(localFile);
            uploads.put(url + ":" + offset, data);
            return "stub-etag-" + offset;
        } catch (Exception e) {
            throw new FileStorageException("Stub part upload failed", e);
        }
    }

    public byte[] getUploaded(String url) { return uploads.get(url); }
}
