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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTransferAdaptersTest {

    @TempDir Path tempDir;

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        Thread.interrupted();
    }

    @Test
    void azureWholeUploadUsesBlockBlobHeader() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201));
        Path source = Files.writeString(tempDir.resolve("blob.bin"), "blob");

        new AzureFileTransferAdapter(new OkHttpClient())
                .upload(server.url("/blob?sig=secret").toString(), source);

        RecordedRequest request = server.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertEquals("BlockBlob", request.getHeader("x-ms-blob-type"));
        assertEquals("blob", request.getBody().readUtf8());
    }

    @Test
    void azureMultipartUsesStableBlockIdBlockQueryAndExactRange() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201));
        Path source = Files.write(
                tempDir.resolve("blob.bin"), new byte[] {0, 1, 2, 3, 4, 5, 6});
        AzureFileTransferAdapter adapter = new AzureFileTransferAdapter(new OkHttpClient());

        String blockId = adapter.uploadPart(
                server.url("/blob?sig=secret&comp=old").toString(), source, 2, 3, 7);

        String expected = Base64.getEncoder().encodeToString(
                "block-00000007".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, blockId);
        RecordedRequest request = server.takeRequest();
        assertEquals("block", request.getRequestUrl().queryParameter("comp"));
        assertEquals(expected, request.getRequestUrl().queryParameter("blockid"));
        assertEquals("secret", request.getRequestUrl().queryParameter("sig"));
        assertEquals(null, request.getHeader("x-ms-blob-type"));
        assertArrayEquals(new byte[] {2, 3, 4}, request.getBody().readByteArray());
    }

    @Test
    void s3MultipartRequiresAndReturnsEtag() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("ETag", "\"etag-1\""));
        Path source = Files.write(tempDir.resolve("object.bin"), new byte[] {1, 2, 3, 4});
        S3FileTransferAdapter adapter = new S3FileTransferAdapter(new OkHttpClient());

        String etag = adapter.uploadPart(server.url("/part").toString(), source, 1, 2, 1);

        assertEquals("\"etag-1\"", etag);
        assertArrayEquals(new byte[] {2, 3}, server.takeRequest().getBody().readByteArray());

        server.enqueue(new MockResponse().setResponseCode(200));
        IOException error = assertThrows(
                IOException.class,
                () -> adapter.uploadPart(server.url("/part-2").toString(), source, 0, 1, 2));
        assertTrue(error.getMessage().contains("ETag"));
    }

    @Test
    void rangeUploadFailsInsteadOfSilentlySendingTooFewBytes() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("ETag", "etag"));
        Path source = Files.write(tempDir.resolve("short.bin"), new byte[] {1, 2});

        IOException error = assertThrows(
                IOException.class,
                () -> new S3FileTransferAdapter(new OkHttpClient())
                        .uploadPart(server.url("/part").toString(), source, 1, 5, 1));

        assertFalse(error.getMessage().contains(server.getHostName()));
    }

    @Test
    void gcsAndLocalRemainSingleRequestOnly() throws Exception {
        GcsFileTransferAdapter gcs = new GcsFileTransferAdapter(new OkHttpClient());
        assertFalse(gcs.supportsMultipart());

        LocalFileTransferAdapter local = new LocalFileTransferAdapter();
        assertFalse(local.supportsMultipart());
        Path source = Files.write(tempDir.resolve("source.bin"), new byte[] {7, 8});
        Path stored = tempDir.resolve("storage/stored.bin");
        local.upload(stored.toUri().toString(), source);
        Path downloaded = tempDir.resolve("download.bin");
        local.download(stored.toUri().toString(), downloaded);
        assertArrayEquals(new byte[] {7, 8}, Files.readAllBytes(downloaded));

        assertThrows(
                IOException.class,
                () -> local.upload("https://example.invalid/file", source));
    }

    @Test
    void signedTransfersDisableRedirectsAndRedactSignedUrl() throws Exception {
        String target = server.url("/target").toString();
        String signedUrl = server.url("/redirect?signature=do-not-leak").toString();
        server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location", target));
        Path source = Files.writeString(tempDir.resolve("source.txt"), "content");

        IOException error = assertThrows(
                IOException.class,
                () -> new S3FileTransferAdapter(new OkHttpClient()).upload(signedUrl, source));

        assertEquals(1, server.getRequestCount());
        assertFalse(allMessages(error).contains("do-not-leak"));
    }

    @Test
    void injectedHttpClientInterceptorsAreNotUsedForSignedRequests() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        OkHttpClient configured = new OkHttpClient.Builder()
                .addInterceptor(
                        chain -> chain.proceed(
                                chain.request().newBuilder()
                                        .header("Authorization", "Bearer conductor-secret")
                                        .build()))
                .build();
        Path source = Files.writeString(tempDir.resolve("source.txt"), "content");

        new GcsFileTransferAdapter(configured)
                .upload(server.url("/upload").toString(), source);

        assertEquals(null, server.takeRequest().getHeader("Authorization"));
    }

    private static String allMessages(Throwable error) {
        StringBuilder value = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            value.append(current.getMessage()).append('\n');
        }
        return value.toString();
    }
}
