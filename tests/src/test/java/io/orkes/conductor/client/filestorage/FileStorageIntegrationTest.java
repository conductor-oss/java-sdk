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
package io.orkes.conductor.client.filestorage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.conductoross.conductor.sdk.file.FileUploader;
import org.conductoross.conductor.sdk.file.ManagedFileHandler;
import org.conductoross.conductor.sdk.file.WorkflowFileClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.Wait;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.http.OrkesMetadataClient;
import io.orkes.conductor.client.http.OrkesWorkflowClient;
import io.orkes.conductor.client.util.ClientTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class FileStorageIntegrationTest {

    private static final String WAIT_WF_NAME = "file_storage_integration_wait_wf";

    private static final ObjectMapper MAPPER = new ObjectMapperProvider().getObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @TempDir
    static Path tempDir;

    private static FileClient fileClient;
    private static OrkesWorkflowClient workflowClient;
    private static OrkesMetadataClient metadataClient;
    private static String workflowId;

    @BeforeAll
    static void setUp() {
        fileClient = ClientTestUtil.getOrkesClients().getFileClient();
        workflowClient = ClientTestUtil.getOrkesClients().getWorkflowClient();
        metadataClient = ClientTestUtil.getOrkesClients().getMetadataClient();

        WorkflowExecutor executor = new WorkflowExecutor(ClientTestUtil.getClient(), 10);
        ConductorWorkflow<Object> wf = new ConductorWorkflow<>(executor);
        wf.setName(WAIT_WF_NAME);
        wf.setVersion(1);
        wf.add(new Wait("wait_task", Duration.ofSeconds(300)));
        wf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        wf.setTimeoutSeconds(600);
        wf.registerWorkflow(true, true);

        StartWorkflowRequest req = new StartWorkflowRequest();
        req.setName(WAIT_WF_NAME);
        req.setVersion(1);
        req.setInput(Map.of());
        workflowId = workflowClient.startWorkflow(req);
        assertNotNull(workflowId);
    }

    @AfterAll
    static void cleanUp() {
        try {
            workflowClient.terminateWorkflow(workflowId, "integration test cleanup");
        } catch (Exception ignored) {
        }
        try {
            metadataClient.unregisterWorkflowDef(WAIT_WF_NAME, 1);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("upload(Path) returns prefixed handle and preserves filename + content type")
    void uploadFromPath() throws Exception {
        Path file = writeFile("hello.txt", "hello world");

        FileHandler handler = fileClient.upload(workflowId, file,
                new FileUploadOptions().setContentType("text/plain"));

        assertNotNull(handler.getFileHandleId());
        assertTrue(handler.getFileHandleId().startsWith(FileHandler.PREFIX));
        assertEquals("hello.txt", handler.getFileName());
        assertEquals("text/plain", handler.getContentType());
    }

    @Test
    @DisplayName("upload(InputStream) buffers to temp file and uploads")
    void uploadFromInputStream() {
        byte[] payload = "stream payload".getBytes(StandardCharsets.UTF_8);

        FileHandler handler = fileClient.upload(workflowId, new ByteArrayInputStream(payload),
                new FileUploadOptions().setFileName("stream.txt").setContentType("text/plain"));

        assertNotNull(handler.getFileHandleId());
        assertTrue(handler.getFileHandleId().startsWith(FileHandler.PREFIX));
    }

    @Test
    @DisplayName("a fresh ManagedFileHandler downloads the bytes uploaded under the same handle id")
    void downloadRoundTripsContent() throws Exception {
        Path file = writeFile("data.bin", "round-trip content 12345");

        FileHandler uploaded = fileClient.upload(workflowId, file,
                new FileUploadOptions().setContentType("application/octet-stream"));

        ManagedFileHandler downloaded = new ManagedFileHandler(
                uploaded.getFileHandleId(), new WorkflowFileClient(fileClient, workflowId));

        try (InputStream in = downloaded.getInputStream()) {
            assertEquals("round-trip content 12345", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("multiple uploads produce distinct handle ids")
    void multipleUploadsProduceDistinctHandleIds() throws Exception {
        FileHandler h1 = fileClient.upload(workflowId, writeFile("doc1.pdf", "pdf 1"),
                new FileUploadOptions().setContentType("application/pdf"));
        FileHandler h2 = fileClient.upload(workflowId, writeFile("doc2.pdf", "pdf 2"),
                new FileUploadOptions().setContentType("application/pdf"));

        assertNotEquals(h1.getFileHandleId(), h2.getFileHandleId());
    }

    @Test
    @DisplayName("FileUploader interface (WorkflowFileClient) yields the same handle shape as FileClient")
    void uploadViaFileUploaderInterface() throws Exception {
        FileUploader uploader = new WorkflowFileClient(fileClient, workflowId);

        FileHandler handler = uploader.upload(writeFile("via-interface.txt", "interface test"),
                new FileUploadOptions().setContentType("text/plain"));

        assertNotNull(handler.getFileHandleId());
        assertTrue(handler.getFileHandleId().startsWith(FileHandler.PREFIX));
    }

    @Test
    @DisplayName("multipart=false (default) uploads via single-request path")
    void singleRequestUploadIsDefault() throws Exception {
        FileHandler handler = fileClient.upload(workflowId, writeFile("default.txt", "single"),
                new FileUploadOptions().setContentType("text/plain"));

        assertNotNull(handler.getFileHandleId());
    }

    @Test
    @DisplayName("multipart=true uploads succeed (multipart path on capable backends; falls back otherwise)")
    void multipartFlagTrueDoesNotFail() throws Exception {
        FileHandler handler = fileClient.upload(workflowId, writeFile("mp.bin", "multipart payload"),
                new FileUploadOptions().setContentType("application/octet-stream").setMultipart(true));

        assertNotNull(handler.getFileHandleId());
        assertTrue(handler.getFileHandleId().startsWith(FileHandler.PREFIX));
    }

    @Test
    @DisplayName("FileHandler serializes to the fixed three-field shape")
    void fileHandlerSerializesToThreeFields() throws Exception {
        FileHandler uploaded = fileClient.upload(workflowId, writeFile("ser.txt", "json shape"),
                new FileUploadOptions().setContentType("text/plain"));

        Map<String, Object> json = MAPPER.convertValue(uploaded, MAP_TYPE);

        assertEquals(Set.of("fileHandleId", "contentType", "fileName"), json.keySet());
        assertEquals(uploaded.getFileHandleId(), json.get("fileHandleId"));
        assertEquals("text/plain", json.get("contentType"));
        assertEquals("ser.txt", json.get("fileName"));
    }

    private static Path writeFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
