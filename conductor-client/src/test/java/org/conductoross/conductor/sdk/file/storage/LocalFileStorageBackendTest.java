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
package org.conductoross.conductor.sdk.file.storage;

import java.io.ByteArrayInputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.conductoross.conductor.client.model.file.StorageType;
import org.conductoross.conductor.client.storage.LocalFileStorageBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageBackendTest {

    private final LocalFileStorageBackend backend = new LocalFileStorageBackend();

    @Test
    void storageTypeIsLocal() {
        assertEquals(StorageType.LOCAL, backend.getStorageType());
    }

    @Test
    void uploadFromPathWritesToFileUri(@TempDir Path tempDir) throws Exception {
        Path source = Files.write(tempDir.resolve("src.bin"), new byte[] {1, 2, 3});
        Path dest = tempDir.resolve("uploads/out.bin");
        String url = dest.toUri().toString();

        backend.upload(url, source);

        assertTrue(Files.exists(dest));
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(dest));
    }

    @Test
    void uploadFromStreamWritesToFileUri(@TempDir Path tempDir) throws Exception {
        byte[] payload = "hello".getBytes();
        Path dest = tempDir.resolve("uploads/stream.bin");
        String url = dest.toUri().toString();

        backend.upload(url, new ByteArrayInputStream(payload), payload.length);

        assertTrue(Files.exists(dest));
        assertArrayEquals(payload, Files.readAllBytes(dest));
    }

    @Test
    void downloadReadsFromFileUri(@TempDir Path tempDir) throws Exception {
        Path source = Files.write(tempDir.resolve("src.bin"), new byte[] {9, 8, 7});
        Path dest = tempDir.resolve("cache/out.bin");
        String url = source.toUri().toString();

        backend.download(url, dest);

        assertTrue(Files.exists(dest));
        assertArrayEquals(new byte[] {9, 8, 7}, Files.readAllBytes(dest));
    }

    @Test
    void httpSchemeRaisesFileSystemNotFound(@TempDir Path tempDir) throws Exception {
        Path source = Files.write(tempDir.resolve("src.bin"), new byte[] {0});
        String httpUrl = "https://example.com/uploads/foo";

        assertThrows(
                FileSystemNotFoundException.class, () -> backend.upload(httpUrl, source));
    }

    @Test
    void relativeUrlRaisesIllegalArgument(@TempDir Path tempDir) throws Exception {
        Path source = Files.write(tempDir.resolve("src.bin"), new byte[] {0});
        String relativeUrl = "uploads/foo";

        assertThrows(IllegalArgumentException.class, () -> backend.upload(relativeUrl, source));
    }
}
