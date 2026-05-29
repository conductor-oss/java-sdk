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

/** {@link FileStorageBackend} for Azure Blob Storage. Supports multipart via resumable block uploads. */
public class AzureFileStorageBackend implements FileStorageBackend {

    private static final String BLOB_TYPE_HEADER = "x-ms-blob-type";
    private static final String BLOB_TYPE_VALUE = "BlockBlob";
    private static final String X_MS_CONTENT_MD_5 = "x-ms-content-md5";

    @Override
    public StorageType getStorageType() { return StorageType.AZURE_BLOB; }

    @Override
    public void upload(String url, Path localFile) {
        Request request = new Request.Builder()
                .url(url)
                .header(BLOB_TYPE_HEADER, BLOB_TYPE_VALUE)
                .put(RequestBody.create(localFile.toFile(), null))
                .build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("Azure upload failed with status: " + response.code());
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Azure upload failed: " + url, e);
        }
    }

    @Override
    public void upload(String url, InputStream inputStream, long contentLength) {
        Request request = new Request.Builder()
                .url(url)
                .header(BLOB_TYPE_HEADER, BLOB_TYPE_VALUE)
                .put(HttpBodies.stream(inputStream, contentLength))
                .build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("Azure stream upload failed with status: " + response.code());
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Azure stream upload failed: " + url, e);
        }
    }

    @Override
    public void download(String url, Path destination) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("Azure download failed with status: " + response.code());
            }
            Files.createDirectories(destination.getParent());
            try (InputStream in = response.body().byteStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Azure download failed: " + url, e);
        }
    }

    @Override
    public String uploadPart(String url, Path localFile, long offset, long length) {
        Request request = new Request.Builder()
                .url(url)
                .header(BLOB_TYPE_HEADER, BLOB_TYPE_VALUE)
                .put(HttpBodies.range(localFile, offset, length))
                .build();
        try (Response response = HttpBodies.CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new FileStorageException("Azure part upload failed with status: " + response.code());
            }
            return response.header(X_MS_CONTENT_MD_5);
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Azure part upload failed: " + url, e);
        }
    }
}
