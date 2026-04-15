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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.conductoross.conductor.client.model.file.FileDownloadUrlResponse;
import org.conductoross.conductor.client.model.file.FileHandle;
import org.conductoross.conductor.client.model.file.FileUploadCompleteResponse;
import org.conductoross.conductor.client.model.file.FileUploadResponse;
import org.conductoross.conductor.client.model.file.FileUploadStatus;
import org.conductoross.conductor.client.model.file.FileUploadUrlResponse;
import org.conductoross.conductor.client.model.file.MultipartInitResponse;
import org.conductoross.conductor.client.model.file.StorageType;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileStorageBackend;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.conductoross.conductor.sdk.file.StubFileStorageBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientResponse;
import com.netflix.conductor.client.http.Param;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileClientTest {

    private static final String RAW_ID = UUID.randomUUID().toString();
    private static final String HANDLE_ID = FileHandler.PREFIX + RAW_ID;
    private static final String UPLOAD_URL = "stub://upload/" + RAW_ID;
    private static final String DOWNLOAD_URL = "stub://download/" + RAW_ID;

    @TempDir
    Path tempDir;

    private ConductorClient conductorClient;
    private StubFileStorageBackend backend;
    private final List<Stub> stubs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        conductorClient = mock(ConductorClient.class);
        backend = new StubFileStorageBackend();
        stubs.clear();
        // Single dispatcher — registering a matcher later just appends to the list.
        when(conductorClient.execute(any(ConductorClientRequest.class), any()))
                .thenAnswer(invocation -> {
                    ConductorClientRequest req = invocation.getArgument(0);
                    for (Stub stub : stubs) {
                        if (stub.matcher.test(req)) {
                            return new ConductorClientResponse<>(200, Map.of(), stub.data);
                        }
                    }
                    throw new AssertionError("No stubbed response for path=" + req.getPath());
                });
    }

    @Test
    void uploadSinglePartByDefault() throws Exception {
        Path source = writeFile("small.txt", "hello world".getBytes());
        stubCreateFile(StorageType.LOCAL);
        stubConfirmUpload();

        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        FileHandler handler = client.upload(
                "wf-1", source,
                new FileUploadOptions().setContentType("text/plain"));

        assertEquals(HANDLE_ID, handler.getFileHandleId());
        assertEquals("hello world", new String(backend.getUploaded(UPLOAD_URL)));
        verifyPathCalled("/files/{fileId}/upload-complete", RAW_ID);
        verifyPathNotCalled("/files/{fileId}/multipart");
    }

    @Test
    void uploadUsesMultipartWhenFlagTrueAndBackendSupports() throws Exception {
        Path source = writeFile("big.bin", new byte[16]);
        stubCreateFile(StorageType.S3);
        stubMultipartInit();
        stubMultipartComplete();

        FileClientProperties props = new FileClientProperties();
        props.setMultipartPartSize(5); // partSize 5 + 16 bytes → 4 parts
        MultipartBackend mp = new MultipartBackend(StorageType.S3);
        FileClient client = clientBuilder()
                .properties(props)
                .addStorageBackend(mp)
                .build();

        FileHandler handler = client.upload(
                "wf-1", source,
                new FileUploadOptions().setMultipart(true));

        assertEquals(HANDLE_ID, handler.getFileHandleId());
        assertEquals(4, mp.parts.get());
        verifyPathCalled("/files/{fileId}/multipart", RAW_ID);
        verifyPathCalled("/files/{fileId}/multipart/{uploadId}/complete", RAW_ID);
    }

    @Test
    void uploadFallsBackToSinglePartWhenBackendLacksMultipart() throws Exception {
        Path source = writeFile("big.bin", new byte[32]);
        stubCreateFile(StorageType.LOCAL);
        stubConfirmUpload();

        StubFileStorageBackend noMultipart = new StubFileStorageBackend() {
            @Override public boolean hasMultipartSupport() { return false; }
        };
        FileClient client = clientBuilder()
                .addStorageBackend(noMultipart)
                .build();

        client.upload("wf-1", source, new FileUploadOptions().setMultipart(true));

        verifyPathCalled("/files/{fileId}/upload-complete", RAW_ID);
        verifyPathNotCalled("/files/{fileId}/multipart");
    }

    @Test
    void uploadThrowsWhenBackendMissingForServerStorageType() throws Exception {
        Path source = writeFile("x.bin", new byte[] {1});
        stubCreateFile(StorageType.AZURE_BLOB);

        // Register only LOCAL; server reports AZURE_BLOB.
        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        FileStorageException ex = assertThrows(
                FileStorageException.class, () -> client.upload("wf-1", source));
        assertTrue(ex.getMessage().contains("AZURE_BLOB"));
        assertTrue(ex.getMessage().contains("LOCAL"));
    }

    @Test
    void uploadFromInputStreamStreamsThroughTempFile() {
        byte[] payload = "from-stream".getBytes();
        stubCreateFile(StorageType.LOCAL);
        stubConfirmUpload();

        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        FileHandler handler = client.upload(
                "wf-1", new ByteArrayInputStream(payload),
                new FileUploadOptions().setContentType("text/plain"));

        assertEquals(HANDLE_ID, handler.getFileHandleId());
        assertEquals("from-stream", new String(backend.getUploaded(UPLOAD_URL)));
    }

    @Test
    void downloadFetchesUrlAndDelegatesToBackend() throws Exception {
        // Pre-seed the stub so its download() has something to serve at DOWNLOAD_URL.
        Path src = writeFile("remote.bin", new byte[] {9, 8, 7});
        backend.upload(DOWNLOAD_URL, src);
        stubDownloadUrl();

        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        Path dest = tempDir.resolve("out/remote.bin");
        client.download("wf-1", HANDLE_ID, StorageType.LOCAL, dest);

        assertTrue(Files.exists(dest));
        verifyPathCalled("/files/{workflowId}/{fileId}/download-url", RAW_ID);
    }

    @Test
    void getMetadataSendsStrippedFileIdAndReturnsHandle() {
        FileHandle fixture = new FileHandle();
        fixture.setFileHandleId(HANDLE_ID);
        fixture.setFileName("r.pdf");
        registerStub(req -> "/files/{fileId}".equals(req.getPath()), fixture);

        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        FileHandle got = client.getMetadata(HANDLE_ID);
        assertEquals("r.pdf", got.getFileName());
        verifyPathCalled("/files/{fileId}", RAW_ID);
    }

    @Test
    void confirmUploadSendsStrippedFileId() {
        stubConfirmUpload();
        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        client.confirmUpload(HANDLE_ID);

        verifyPathCalled("/files/{fileId}/upload-complete", RAW_ID);
    }

    @Test
    void propertiesDefaultsAppliedWhenNull() {
        FileClient client = new FileClient(conductorClient, null, null);

        assertEquals(3, client.getRetryCount());
        assertNotNull(client.getCacheDirectory());
    }

    @Test
    void uploadThrowsWhenWorkflowIdNull() throws Exception {
        Path source = writeFile("wf.bin", new byte[] {1});
        FileClient client = clientWithBackends(Map.of(StorageType.LOCAL, backend));

        FileStorageException ex = assertThrows(
                FileStorageException.class, () -> client.upload(null, source));
        assertTrue(ex.getMessage().contains("workflowId"));
    }

    @Test
    void builderCustomBackendOverridesBuiltIn() throws Exception {
        Path source = writeFile("overridden.txt", "x".getBytes());
        stubCreateFile(StorageType.LOCAL);
        stubConfirmUpload();

        StubFileStorageBackend override = new StubFileStorageBackend();
        FileClient client = FileClient.builder(conductorClient)
                .addStorageBackend(override)
                .build();

        client.upload("wf-1", source);

        assertNotNull(override.getUploaded(UPLOAD_URL),
                "override backend should have received the upload");
    }

    // --- helpers ---

    private Path writeFile(String name, byte[] bytes) throws Exception {
        return Files.write(tempDir.resolve(name), bytes);
    }

    private FileClient clientWithBackends(Map<StorageType, FileStorageBackend> backends) {
        return new FileClient(conductorClient, new FileClientProperties(), backends);
    }

    private FileClient.Builder clientBuilder() {
        return FileClient.builder(conductorClient);
    }

    private void stubCreateFile(StorageType storageType) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileHandleId(HANDLE_ID);
        response.setStorageType(storageType);
        response.setUploadStatus(FileUploadStatus.UPLOADING);
        response.setUploadUrl(UPLOAD_URL);
        registerStub(req -> "/files".equals(req.getPath()), response);
    }

    private void stubConfirmUpload() {
        registerStub(req -> "/files/{fileId}/upload-complete".equals(req.getPath()),
                new FileUploadCompleteResponse());
    }

    private void stubDownloadUrl() {
        FileDownloadUrlResponse resp = new FileDownloadUrlResponse();
        resp.setFileHandleId(HANDLE_ID);
        resp.setDownloadUrl(DOWNLOAD_URL);
        registerStub(req -> "/files/{workflowId}/{fileId}/download-url".equals(req.getPath()), resp);
    }

    private void stubMultipartInit() {
        MultipartInitResponse init = new MultipartInitResponse();
        init.setFileHandleId(HANDLE_ID);
        init.setUploadId("up-1");
        registerStub(req -> "/files/{fileId}/multipart".equals(req.getPath()), init);

        FileUploadUrlResponse partUrl = new FileUploadUrlResponse();
        partUrl.setFileHandleId(HANDLE_ID);
        partUrl.setUploadUrl(UPLOAD_URL + "/part");
        registerStub(
                req -> "/files/{fileId}/multipart/{uploadId}/part/{partNumber}".equals(req.getPath()),
                partUrl);
    }

    private void stubMultipartComplete() {
        registerStub(
                req -> "/files/{fileId}/multipart/{uploadId}/complete".equals(req.getPath()),
                new FileUploadCompleteResponse());
    }

    private void registerStub(Predicate<ConductorClientRequest> matcher, Object data) {
        stubs.add(new Stub(matcher, data));
    }

    private void verifyPathCalled(String path, String expectedFileId) {
        ArgumentCaptor<ConductorClientRequest> captor = ArgumentCaptor.forClass(ConductorClientRequest.class);
        verify(conductorClient, atLeastOnce()).execute(captor.capture(), any());
        boolean matched = captor.getAllValues().stream()
                .anyMatch(req -> path.equals(req.getPath())
                        && pathParamMatches(req.getPathParams(), "fileId", expectedFileId));
        assertTrue(matched, "expected " + path + " with fileId=" + expectedFileId);
    }

    private void verifyPathNotCalled(String path) {
        ArgumentCaptor<ConductorClientRequest> captor = ArgumentCaptor.forClass(ConductorClientRequest.class);
        verify(conductorClient, atLeastOnce()).execute(captor.capture(), any());
        assertTrue(captor.getAllValues().stream().noneMatch(req -> path.equals(req.getPath())),
                "did not expect request to " + path);
    }

    private static boolean pathParamMatches(List<Param> params, String name, String value) {
        return params != null && params.stream()
                .anyMatch(p -> name.equals(p.name()) && value.equals(p.value()));
    }

    private record Stub(Predicate<ConductorClientRequest> matcher, Object data) {}

    // Backend stub that claims multipart support and records parts.
    private static final class MultipartBackend implements FileStorageBackend {
        private final StorageType type;
        final AtomicInteger parts = new AtomicInteger();

        MultipartBackend(StorageType type) { this.type = type; }

        @Override public StorageType getStorageType() { return type; }
        @Override public void upload(String url, Path localFile) { throw new AssertionError("single-part not expected"); }
        @Override public void upload(String url, InputStream in, long len) { throw new AssertionError(); }
        @Override public void download(String url, Path dest) { throw new AssertionError(); }
        @Override public String uploadPart(String url, Path localFile, long offset, long length) {
            return "etag-" + parts.incrementAndGet();
        }
        @Override public boolean hasMultipartSupport() { return true; }
    }
}
