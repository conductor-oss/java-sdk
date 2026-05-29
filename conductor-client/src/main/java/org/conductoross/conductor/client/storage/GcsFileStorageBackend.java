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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.conductoross.conductor.client.model.file.StorageType;
import org.conductoross.conductor.sdk.file.FileStorageBackend;
import org.conductoross.conductor.sdk.file.FileStorageException;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** {@link FileStorageBackend} for Google Cloud Storage. Supports multipart via resumable sessions. */
public class GcsFileStorageBackend implements FileStorageBackend {

    @Override
    public StorageType getStorageType() { return StorageType.GCS; }

    @Override
    public void upload(String url, Path localFile) {
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(localFile.toFile(), null))
                .build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("GCS upload failed with status: " + response.code());
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("GCS upload failed: " + url, e);
        }
    }

    @Override
    public void upload(String url, InputStream inputStream, long contentLength) {
        Request request = new Request.Builder()
                .url(url)
                .put(HttpBodies.stream(inputStream, contentLength))
                .build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("GCS upload failed with status: " + response.code());
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("GCS stream upload failed: " + url, e);
        }
    }

    @Override
    public void download(String url, Path destination) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("GCS download failed with status: " + response.code());
            }
            Files.createDirectories(destination.getParent());
            try (InputStream in = response.body().byteStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("GCS download failed: " + url, e);
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
