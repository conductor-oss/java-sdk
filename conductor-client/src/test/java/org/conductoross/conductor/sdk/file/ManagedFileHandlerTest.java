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

import org.conductoross.conductor.client.model.file.FileHandle;
import org.conductoross.conductor.client.model.file.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedFileHandlerTest {

    private static final String FILE_ID = "conductor://file/abc-123";
    private static final String RAW_ID = "abc-123";
    private static final byte[] CONTENT = {1, 2, 3, 4, 5};

    @TempDir
    Path cacheDir;

    private WorkflowFileClient workflowFileClient;

    @BeforeEach
    void setUp() {
        workflowFileClient = mock(WorkflowFileClient.class);
        when(workflowFileClient.getCacheDirectory()).thenReturn(cacheDir.toString());
        when(workflowFileClient.getRetryCount()).thenReturn(3);
    }

    @Test
    void metadataLazilyLoadedOnFirstAccessAndCached() {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());
        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);

        assertEquals("report.pdf", handler.getFileName());
        assertEquals("application/pdf", handler.getContentType());
        assertEquals(1234L, handler.getFileSize());

        // three getters, only one server call
        verify(workflowFileClient, times(1)).getMetadata(FILE_ID);
    }

    @Test
    void metadataNotFetchedIfPrePopulated() {
        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);
        handler.setFileName("pre.txt");
        handler.setContentType("text/plain");
        handler.setFileSize(99L);
        handler.setStorageType(StorageType.LOCAL);

        assertEquals("pre.txt", handler.getFileName());
        assertEquals("text/plain", handler.getContentType());
        assertEquals(99L, handler.getFileSize());

        verify(workflowFileClient, never()).getMetadata(anyString());
    }

    @Test
    void getFileHandleIdReturnsConstructorValue() {
        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);
        assertEquals(FILE_ID, handler.getFileHandleId());
    }

    @Test
    void getInputStreamDownloadsAndReadsContent() throws Exception {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());
        stubSuccessfulDownload();

        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);

        try (InputStream in = handler.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }

        verify(workflowFileClient, times(1)).download(eq(FILE_ID), eq(StorageType.LOCAL), any(Path.class));
    }

    @Test
    void downloadIsIdempotentAcrossCalls() throws Exception {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());
        stubSuccessfulDownload();

        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);
        handler.getInputStream().close();
        handler.getInputStream().close();
        handler.getInputStream().close();

        verify(workflowFileClient, times(1)).download(anyString(), any(), any(Path.class));
    }

    @Test
    void preExistingCacheFileSkipsDownload() throws Exception {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());
        // Seed the cache at the exact path ManagedFileHandler computes.
        Path expected = cacheDir.resolve(RAW_ID + "_report.pdf");
        Files.write(expected, CONTENT);

        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);
        try (InputStream in = handler.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }

        verify(workflowFileClient, never()).download(anyString(), any(), any(Path.class));
    }

    @Test
    void downloadRetriesUpToConfiguredCount() throws Exception {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());

        // Fail twice, then succeed on the 3rd attempt (retryCount = 3).
        int[] calls = {0};
        doAnswer(inv -> {
            calls[0]++;
            if (calls[0] < 3) {
                throw new FileStorageException("transient");
            }
            Path dest = inv.getArgument(2);
            Files.createDirectories(dest.getParent());
            Files.write(dest, CONTENT);
            return null;
        }).when(workflowFileClient).download(anyString(), any(), any(Path.class));

        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);
        try (InputStream in = handler.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }

        verify(workflowFileClient, times(3)).download(anyString(), any(), any(Path.class));
    }

    @Test
    void downloadFailsAfterExhaustingRetries() {
        when(workflowFileClient.getMetadata(FILE_ID)).thenReturn(metadataFixture());
        doThrow(new FileStorageException("boom"))
                .when(workflowFileClient).download(anyString(), any(), any(Path.class));

        ManagedFileHandler handler = new ManagedFileHandler(FILE_ID, workflowFileClient);

        FileStorageException ex = assertThrows(
                FileStorageException.class, handler::getInputStream);
        assertEquals(true, ex.getMessage().contains(FILE_ID));
        verify(workflowFileClient, times(3)).download(anyString(), any(), any(Path.class));
    }

    private FileHandle metadataFixture() {
        FileHandle h = new FileHandle();
        h.setFileHandleId(FILE_ID);
        h.setFileName("report.pdf");
        h.setContentType("application/pdf");
        h.setFileSize(1234L);
        h.setStorageType(StorageType.LOCAL);
        return h;
    }

    private void stubSuccessfulDownload() {
        doAnswer(inv -> {
            Path dest = inv.getArgument(2);
            Files.createDirectories(dest.getParent());
            Files.write(dest, CONTENT);
            return null;
        }).when(workflowFileClient).download(anyString(), any(), any(Path.class));
    }
}
