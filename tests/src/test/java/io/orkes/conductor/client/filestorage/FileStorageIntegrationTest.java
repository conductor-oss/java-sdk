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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.model.file.FileMetadata;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @TempDir static Path tempDir;

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
        ConductorWorkflow<Object> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(WAIT_WF_NAME);
        workflow.setVersion(1);
        workflow.add(new Wait("wait_task", Duration.ofSeconds(300)));
        workflow.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        workflow.setTimeoutSeconds(600);
        workflow.registerWorkflow(true, true);

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        workflowId = workflowClient.startWorkflow(request);
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
    void pathUploadReturnsRawHandleAndMetadata() throws Exception {
        String handle = fileClient.upload(
                workflowId,
                writeFile("hello.txt", "hello world"),
                new FileUploadOptions().setContentType("text/plain"));

        assertTrue(handle.startsWith("conductor://file/"));
        FileMetadata metadata = fileClient.getMetadata(workflowId, handle);
        assertEquals("hello.txt", metadata.getFileName());
        assertEquals("text/plain", metadata.getContentType());
        assertEquals(11L, metadata.getFileSize());
    }

    @Test
    void streamUploadDoesNotRequireAWorkerFileObject() {
        String handle = fileClient.upload(
                workflowId,
                new ByteArrayInputStream("stream payload".getBytes(StandardCharsets.UTF_8)),
                new FileUploadOptions()
                        .setFileName("stream.txt")
                        .setContentType("text/plain"));

        assertTrue(handle.startsWith("conductor://file/"));
    }

    @Test
    void explicitDownloadRoundTripsContent() throws Exception {
        String handle = fileClient.upload(
                workflowId,
                writeFile("data.bin", "round-trip content"),
                new FileUploadOptions().setContentType("application/octet-stream"));
        Path destination = tempDir.resolve("downloaded/data.bin");

        assertEquals(
                destination.toAbsolutePath(),
                fileClient.download(workflowId, handle, destination));
        assertEquals("round-trip content", Files.readString(destination));
    }

    @Test
    void multipleUploadsProduceDistinctRawHandles() throws Exception {
        String first = fileClient.upload(workflowId, writeFile("doc1.pdf", "pdf 1"));
        String second = fileClient.upload(workflowId, writeFile("doc2.pdf", "pdf 2"));

        assertNotEquals(first, second);
    }

    private static Path writeFile(String name, String content) throws Exception {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
