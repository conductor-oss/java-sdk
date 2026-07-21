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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.conductoross.conductor.client.model.file.FileDownloadUrlResponse;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.client.model.file.FileUploadCompleteResponse;
import org.conductoross.conductor.client.model.file.FileUploadRequest;
import org.conductoross.conductor.client.model.file.FileUploadResponse;
import org.conductoross.conductor.client.model.file.FileUploadStatus;
import org.conductoross.conductor.client.model.file.FileUploadUrlResponse;
import org.conductoross.conductor.client.model.file.MultipartCompleteRequest;
import org.conductoross.conductor.client.model.file.MultipartInitResponse;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientResponse;
import com.netflix.conductor.client.http.Param;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileClientTest {

    private static final String WORKFLOW_ID = "workflow-1";
    private static final String FILE_ID = "file-123";
    private static final String HANDLE = FileClient.FILE_HANDLE_PREFIX + FILE_ID;

    @TempDir Path tempDir;

    private final List<Stub> stubs = new ArrayList<>();
    private ConductorClient conductorClient;
    private MockWebServer storageServer;

    @BeforeEach
    void setUp() throws IOException {
        conductorClient = mock(ConductorClient.class);
        when(conductorClient.execute(any(ConductorClientRequest.class), any()))
                .thenAnswer(
                        invocation -> {
                            ConductorClientRequest request = invocation.getArgument(0);
                            for (Stub stub : stubs) {
                                if (stub.matcher().test(request)) {
                                    Object data = stub.responder().respond(request);
                                    return new ConductorClientResponse<>(200, Map.of(), data);
                                }
                            }
                            throw new AssertionError("No stub for " + request.getPath());
                        });
        storageServer = new MockWebServer();
        storageServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        Thread.interrupted();
        storageServer.shutdown();
    }

    @Test
    void uploadReturnsOpaqueHandleAndScopesCompletionToOwningWorkflow() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("S3", storageServer.url("/upload?signature=secret").toString());
        stubCompletion();

        Path source = Files.writeString(tempDir.resolve("report.txt"), "hello");
        String handle = new FileClient(conductorClient).upload(
                WORKFLOW_ID,
                source,
                new FileUploadOptions().setContentType("text/plain").setTaskId("task-1"));

        assertEquals(HANDLE, handle);
        RecordedRequest transfer = storageServer.takeRequest();
        assertEquals("PUT", transfer.getMethod());
        assertEquals("hello", transfer.getBody().readUtf8());

        ConductorClientRequest create = findRequest("/files");
        FileUploadRequest body = (FileUploadRequest) create.getBody();
        assertEquals(WORKFLOW_ID, body.getWorkflowId());
        assertEquals("report.txt", body.getFileName());
        assertEquals("text/plain", body.getContentType());
        assertEquals("task-1", body.getTaskId());
        assertScopedRequest(
                "/files/{workflowId}/{fileId}/upload-complete", WORKFLOW_ID, FILE_ID);
    }

    @Test
    void multipartIsAutomaticAboveThresholdAndCompletionTokensStayOrdered() throws Exception {
        for (int part = 1; part <= 3; part++) {
            storageServer.enqueue(
                    new MockResponse().setResponseCode(200).addHeader("ETag", "etag-" + part));
        }
        stubCreate("S3", storageServer.url("/unused").toString());
        stubMultipart(storageServer.url("/part").toString());
        stub(
                path("/files/{workflowId}/{fileId}/multipart/{uploadId}/complete"),
                request -> new FileUploadCompleteResponse());

        FileClientProperties properties = new FileClientProperties();
        properties.setMultipartThreshold(5);
        properties.setMultipartPartSize(2);
        Path source = Files.write(tempDir.resolve("six.bin"), new byte[] {0, 1, 2, 3, 4, 5});

        assertEquals(HANDLE, new FileClient(conductorClient, properties).upload(WORKFLOW_ID, source));

        assertArrayEquals(new byte[] {0, 1}, storageServer.takeRequest().getBody().readByteArray());
        assertArrayEquals(new byte[] {2, 3}, storageServer.takeRequest().getBody().readByteArray());
        assertArrayEquals(new byte[] {4, 5}, storageServer.takeRequest().getBody().readByteArray());
        MultipartCompleteRequest completion =
                (MultipartCompleteRequest)
                        findRequest("/files/{workflowId}/{fileId}/multipart/{uploadId}/complete")
                                .getBody();
        assertEquals(List.of("etag-1", "etag-2", "etag-3"), completion.getPartETags());
    }

    @Test
    void gcsUsesSingleRequestAboveMultipartThreshold() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("GCS", storageServer.url("/upload").toString());
        stubCompletion();
        FileClientProperties properties = new FileClientProperties();
        properties.setMultipartThreshold(5);
        properties.setMultipartPartSize(2);

        Path source = Files.write(tempDir.resolve("large.bin"), new byte[6]);
        new FileClient(conductorClient, properties).upload(WORKFLOW_ID, source);

        assertEquals(1, storageServer.getRequestCount());
        verify(conductorClient, never())
                .execute(
                        org.mockito.ArgumentMatchers.argThat(
                                request -> request != null
                                        && request.getPath().contains("/multipart")),
                        any());
    }

    @Test
    void fileAtMultipartThresholdUsesSingleRequest() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("S3", storageServer.url("/upload").toString());
        stubCompletion();
        FileClientProperties properties = new FileClientProperties();
        properties.setMultipartThreshold(5);
        properties.setMultipartPartSize(2);

        new FileClient(conductorClient, properties).upload(
                WORKFLOW_ID, Files.write(tempDir.resolve("boundary.bin"), new byte[5]));

        assertEquals(1, storageServer.getRequestCount());
        assertEquals("/upload", storageServer.takeRequest().getPath());
    }

    @Test
    void failedMultipartUploadIsAbortedWithOwningWorkflow() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(400));
        stubCreate("S3", storageServer.url("/unused").toString());
        stubMultipart(storageServer.url("/part").toString());
        FileClientProperties properties = new FileClientProperties();
        properties.setMultipartThreshold(0);
        properties.setMultipartPartSize(2);

        assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient, properties).upload(
                        WORKFLOW_ID,
                        Files.write(tempDir.resolve("multipart.bin"), new byte[] {1, 2, 3})));

        ArgumentCaptor<ConductorClientRequest> captor =
                ArgumentCaptor.forClass(ConductorClientRequest.class);
        verify(conductorClient).execute(captor.capture());
        ConductorClientRequest abort = captor.getValue();
        assertEquals(
                "/files/{workflowId}/{fileId}/multipart/{uploadId}", abort.getPath());
        assertTrue(hasParam(abort.getPathParams(), "workflowId", WORKFLOW_ID));
        assertTrue(hasParam(abort.getPathParams(), "fileId", FILE_ID));
        assertTrue(hasParam(abort.getPathParams(), "uploadId", "upload-1"));
    }

    @Test
    void unknownStorageTypeFallsBackForHttpSignedUrl() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("NEW_PROVIDER", storageServer.url("/upload").toString());
        stubCompletion();

        String handle = new FileClient(conductorClient).upload(
                WORKFLOW_ID, Files.writeString(tempDir.resolve("a.txt"), "a"));

        assertEquals(HANDLE, handle);
        assertEquals("PUT", storageServer.takeRequest().getMethod());
    }

    @Test
    void expiredSignedUrlIsRefreshedBeforeRetry() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(403));
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("S3", storageServer.url("/expired").toString());
        FileUploadUrlResponse refreshed = new FileUploadUrlResponse();
        refreshed.setUploadUrl(storageServer.url("/fresh").toString());
        stub(path("/files/{workflowId}/{fileId}/upload-url"), request -> refreshed);
        stubCompletion();
        FileClientProperties properties = new FileClientProperties();
        properties.setRetryCount(1);

        new FileClient(conductorClient, properties).upload(
                WORKFLOW_ID, Files.writeString(tempDir.resolve("retry.txt"), "retry"));

        assertEquals("/expired", storageServer.takeRequest().getPath());
        assertEquals("/fresh", storageServer.takeRequest().getPath());
        assertScopedRequest(
                "/files/{workflowId}/{fileId}/upload-url", WORKFLOW_ID, FILE_ID);
    }

    @Test
    void nonTransientSignedUrlFailureIsNotRetriedAndDoesNotExposeUrl() throws Exception {
        String signedUrl = storageServer.url("/upload?signature=do-not-leak").toString();
        storageServer.enqueue(new MockResponse().setResponseCode(400));
        stubCreate("S3", signedUrl);
        FileClientProperties properties = new FileClientProperties();
        properties.setRetryCount(3);

        FileStorageException error = assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient, properties).upload(
                        WORKFLOW_ID, Files.writeString(tempDir.resolve("bad.txt"), "bad")));

        assertEquals(1, storageServer.getRequestCount());
        assertFalse(stackTraceMessages(error).contains("do-not-leak"));
    }

    @Test
    void ambiguousCompletionIsReconciledWithWorkflowScopedMetadata() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("S3", storageServer.url("/upload").toString());
        stub(
                path("/files/{workflowId}/{fileId}/upload-complete"),
                request -> {
                    throw new ConductorClientException(503, "temporarily unavailable");
                });
        FileMetadata metadata = metadata("S3");
        metadata.setUploadStatus(FileUploadStatus.UPLOADED);
        stub(path("/files/{workflowId}/{fileId}"), request -> metadata);

        String handle = new FileClient(conductorClient).upload(
                WORKFLOW_ID, Files.writeString(tempDir.resolve("complete.txt"), "complete"));

        assertEquals(HANDLE, handle);
        assertScopedRequest("/files/{workflowId}/{fileId}", WORKFLOW_ID, FILE_ID);
    }

    @Test
    void downloadAtomicallyReplacesDestinationAndLeavesNoPartFile() throws Exception {
        storageServer.enqueue(new MockResponse().setResponseCode(200).setBody("new-content"));
        stubMetadata("S3");
        stubDownloadUrl(storageServer.url("/download").toString());
        Path destination = Files.writeString(tempDir.resolve("result.bin"), "old-content");

        Path downloaded = new FileClient(conductorClient).download(
                WORKFLOW_ID, HANDLE, destination);

        assertEquals(destination.toAbsolutePath(), downloaded);
        assertEquals("new-content", Files.readString(destination));
        assertFalse(hasPartFile(tempDir));
        assertScopedRequest(
                "/files/{workflowId}/{fileId}/download-url", WORKFLOW_ID, FILE_ID);
    }

    @Test
    void failedDownloadPreservesExistingDestinationAndCleansTemporaryFile() throws Exception {
        storageServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("partial-response-body")
                        .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        stubMetadata("S3");
        stubDownloadUrl(storageServer.url("/download").toString());
        FileClientProperties properties = new FileClientProperties();
        properties.setRetryCount(0);
        Path destination = Files.writeString(tempDir.resolve("result.bin"), "keep-me");

        assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient, properties)
                        .download(WORKFLOW_ID, HANDLE, destination));

        assertEquals("keep-me", Files.readString(destination));
        assertFalse(hasPartFile(tempDir));
    }

    @Test
    void streamUploadRequiresFilenameDoesNotCloseCallerStreamAndCleansBuffer() {
        byte[] bytes = "stream".getBytes(StandardCharsets.UTF_8);
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream input = new ByteArrayInputStream(bytes) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        storageServer.enqueue(new MockResponse().setResponseCode(200));
        stubCreate("S3", storageServer.url("/stream").toString());
        stubCompletion();

        String handle = new FileClient(conductorClient).upload(
                WORKFLOW_ID,
                input,
                new FileUploadOptions().setFileName("stream.txt"));

        assertEquals(HANDLE, handle);
        assertFalse(closed.get());
    }

    @Test
    void failedStreamBufferingDeletesTemporaryFileBeforeAnyServerRequest() throws Exception {
        Set<Path> before = uploadTemporaryFiles();
        InputStream failing = new InputStream() {
            private int reads;

            @Override
            public int read() throws IOException {
                if (reads++ < 4) {
                    return 'x';
                }
                throw new IOException("simulated source failure");
            }
        };

        assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient).upload(
                        WORKFLOW_ID,
                        failing,
                        new FileUploadOptions().setFileName("failed.bin")));

        assertEquals(before, uploadTemporaryFiles());
        verify(conductorClient, never()).execute(any(ConductorClientRequest.class), any());
    }

    @Test
    void validationHappensBeforeServerRequests() throws Exception {
        FileClient client = new FileClient(conductorClient);
        Path source = Files.writeString(tempDir.resolve("source.txt"), "x");

        assertThrows(FileStorageException.class, () -> client.upload(" ", source));
        assertThrows(
                FileStorageException.class,
                () -> client.upload(
                        WORKFLOW_ID,
                        source,
                        new FileUploadOptions().setFileName("../unsafe.txt")));
        assertThrows(
                FileStorageException.class,
                () -> client.download(WORKFLOW_ID, "not-a-handle", tempDir.resolve("out")));
        assertThrows(
                FileStorageException.class,
                () -> client.download(WORKFLOW_ID, HANDLE, tempDir));
        assertThrows(
                FileStorageException.class,
                () -> client.upload(WORKFLOW_ID, new ByteArrayInputStream(new byte[0]), null));

        verify(conductorClient, never()).execute(any(ConductorClientRequest.class), any());
    }

    @Test
    void interruptionIsPreservedAndStopsTransferRetries() throws Exception {
        Thread.currentThread().interrupt();

        assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient).upload(
                        WORKFLOW_ID, Files.writeString(tempDir.resolve("interrupt.txt"), "x")));

        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(0, storageServer.getRequestCount());
        verify(conductorClient, never()).execute(any(ConductorClientRequest.class), any());
    }

    @Test
    void invalidPartConfigurationFailsBeforeServerRequest() {
        FileClientProperties properties = new FileClientProperties();
        properties.setMultipartPartSize(0);

        assertThrows(
                FileStorageException.class,
                () -> new FileClient(conductorClient, properties));
        verify(conductorClient, never()).execute(any(ConductorClientRequest.class), any());
    }

    private void stubCreate(String storageType, String uploadUrl) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileHandleId(HANDLE);
        response.setWorkflowId(WORKFLOW_ID);
        response.setStorageType(storageType);
        response.setUploadStatus(FileUploadStatus.UPLOADING);
        response.setUploadUrl(uploadUrl);
        stub(path("/files"), request -> response);
    }

    private void stubCompletion() {
        stub(
                path("/files/{workflowId}/{fileId}/upload-complete"),
                request -> new FileUploadCompleteResponse());
    }

    private void stubMultipart(String partUrl) {
        MultipartInitResponse initiated = new MultipartInitResponse();
        initiated.setFileHandleId(HANDLE);
        initiated.setUploadId("upload-1");
        stub(path("/files/{workflowId}/{fileId}/multipart"), request -> initiated);

        FileUploadUrlResponse response = new FileUploadUrlResponse();
        response.setFileHandleId(HANDLE);
        response.setUploadUrl(partUrl);
        stub(
                path("/files/{workflowId}/{fileId}/multipart/{uploadId}/part/{partNumber}"),
                request -> response);
    }

    private void stubMetadata(String storageType) {
        FileMetadata metadata = metadata(storageType);
        stub(path("/files/{workflowId}/{fileId}"), request -> metadata);
    }

    private static FileMetadata metadata(String storageType) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileHandleId(HANDLE);
        metadata.setStorageType(storageType);
        metadata.setUploadStatus(FileUploadStatus.UPLOADED);
        return metadata;
    }

    private void stubDownloadUrl(String url) {
        FileDownloadUrlResponse response = new FileDownloadUrlResponse();
        response.setFileHandleId(HANDLE);
        response.setDownloadUrl(url);
        stub(path("/files/{workflowId}/{fileId}/download-url"), request -> response);
    }

    private void stub(Predicate<ConductorClientRequest> matcher, Responder responder) {
        stubs.add(new Stub(matcher, responder));
    }

    private static Predicate<ConductorClientRequest> path(String expected) {
        return request -> expected.equals(request.getPath());
    }

    private ConductorClientRequest findRequest(String path) {
        ArgumentCaptor<ConductorClientRequest> captor =
                ArgumentCaptor.forClass(ConductorClientRequest.class);
        verify(conductorClient, atLeastOnce()).execute(captor.capture(), any());
        return captor.getAllValues().stream()
                .filter(request -> path.equals(request.getPath()))
                .findFirst()
                .orElseThrow();
    }

    private void assertScopedRequest(String path, String workflowId, String fileId) {
        ConductorClientRequest request = findRequest(path);
        assertTrue(hasParam(request.getPathParams(), "workflowId", workflowId));
        assertTrue(hasParam(request.getPathParams(), "fileId", fileId));
    }

    private static boolean hasParam(List<Param> params, String name, String value) {
        return params.stream().anyMatch(param -> name.equals(param.name()) && value.equals(param.value()));
    }

    private static boolean hasPartFile(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            return paths.anyMatch(path -> path.getFileName().toString().endsWith(".part"));
        }
    }

    private static Set<Path> uploadTemporaryFiles() throws IOException {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths
                    .filter(path -> path.getFileName().toString().startsWith("conductor-upload-"))
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private static String stackTraceMessages(Throwable error) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }

    private record Stub(Predicate<ConductorClientRequest> matcher, Responder responder) {}

    @FunctionalInterface
    private interface Responder {
        Object respond(ConductorClientRequest request);
    }
}
