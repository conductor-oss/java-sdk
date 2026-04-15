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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileHandlerTest {

    @TempDir
    Path temp;

    @Test
    void testGetFileHandleIdReturnsNull() throws Exception {
        Path file = temp.resolve("test.txt");
        Files.writeString(file, "hello");
        FileHandler handler = FileHandler.fromLocalFile(file);
        assertNull(handler.getFileHandleId());
    }

    @Test
    void testGetInputStream() throws Exception {
        Path file = temp.resolve("test.txt");
        Files.writeString(file, "hello");
        FileHandler handler = FileHandler.fromLocalFile(file);
        try (InputStream in = handler.getInputStream()) {
            assertEquals("hello", new String(in.readAllBytes()));
        }
    }

    @Test
    void testGetFileName() throws Exception {
        Path file = temp.resolve("report.pdf");
        Files.createFile(file);
        FileHandler handler = FileHandler.fromLocalFile(file);
        assertEquals("report.pdf", handler.getFileName());
    }

    @Test
    void testGetContentType() throws Exception {
        Path file = temp.resolve("doc.pdf");
        Files.createFile(file);
        FileHandler handler = FileHandler.fromLocalFile(file, "application/pdf");
        assertEquals("application/pdf", handler.getContentType());
    }

    @Test
    void testGetContentTypeDefault() throws Exception {
        Path file = temp.resolve("data.bin");
        Files.createFile(file);
        FileHandler handler = FileHandler.fromLocalFile(file);
        assertEquals("application/octet-stream", handler.getContentType());
    }

    @Test
    void testGetFileSize() throws Exception {
        Path file = temp.resolve("data.bin");
        Files.write(file, new byte[]{1, 2, 3, 4, 5});
        FileHandler handler = FileHandler.fromLocalFile(file);
        assertEquals(5, handler.getFileSize());
    }

    @Test
    void testGetInputStreamMissingFile() {
        FileHandler handler = FileHandler.fromLocalFile(Path.of("/nonexistent/file.txt"));
        assertThrows(FileStorageException.class, handler::getInputStream);
    }
}
