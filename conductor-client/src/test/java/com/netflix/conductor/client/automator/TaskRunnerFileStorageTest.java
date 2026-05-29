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
package com.netflix.conductor.client.automator;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileUploadOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskRunnerFileStorageTest {

    private static final String FILE_ID = "conductor://file/" + UUID.randomUUID();

    private FileClient fileClient;
    private TaskRunner runner;

    @BeforeEach
    void setUp() {
        fileClient = mock(FileClient.class);
        Worker worker = mock(Worker.class);
        when(worker.getPollingInterval()).thenReturn(100);
        when(worker.getTaskDefName()).thenReturn("test_task");
        runner = newRunner(worker, fileClient);
    }

    @AfterEach
    void tearDown() {
        if (runner != null) {
            runner.shutdown(1);
        }
    }

    @Test
    @DisplayName("uploadFilesToFileStorage uploads LocalFileHandler and replaces entry with the uploaded FileHandler")
    void uploadFilesToFileStorage_uploadsFileHandlerValues() {
        FileHandler uploaded = mock(FileHandler.class);
        when(uploaded.getFileHandleId()).thenReturn(FILE_ID);
        when(fileClient.upload(any(), any(Path.class), any(FileUploadOptions.class)))
                .thenReturn(uploaded);

        TaskResult result = new TaskResult();
        FileHandler local = FileHandler.fromLocalFile(Path.of("/tmp/a.pdf"), "application/pdf");
        result.getOutputData().put("result", local);

        runner.uploadFilesToFileStorage(result, "wf-1", "task-1");

        ArgumentCaptor<FileUploadOptions> opts = ArgumentCaptor.forClass(FileUploadOptions.class);
        verify(fileClient, times(1))
                .upload(eq("wf-1"), eq(Path.of("/tmp/a.pdf")), opts.capture());
        assertEquals("application/pdf", opts.getValue().getContentType());
        assertEquals("task-1", opts.getValue().getTaskId());
        assertSame(uploaded, result.getOutputData().get("result"));
    }

    @Test
    @DisplayName("uploadFilesToFileStorage leaves an already-uploaded FileHandler in place (no upload, no flatten)")
    void uploadFilesToFileStorage_keepsHandlerForAlreadyUploaded() {
        PreUploadedFileHandler handler = new PreUploadedFileHandler(FILE_ID);
        TaskResult result = new TaskResult();
        result.getOutputData().put("result", handler);

        runner.uploadFilesToFileStorage(result, "wf-1", "task-1");

        verify(fileClient, never())
                .upload(any(), any(Path.class), any(FileUploadOptions.class));
        assertSame(handler, result.getOutputData().get("result"));
    }

    @Test
    @DisplayName("uploadFilesToFileStorage is a no-op when fileClient is null")
    void uploadFilesToFileStorage_noopWhenFileClientNull() {
        runner.shutdown(1);
        Worker worker = mock(Worker.class);
        when(worker.getPollingInterval()).thenReturn(100);
        when(worker.getTaskDefName()).thenReturn("test_task");
        runner = newRunner(worker, null);

        TaskResult result = new TaskResult();
        FileHandler local = FileHandler.fromLocalFile(Path.of("/tmp/a.pdf"), "application/pdf");
        result.getOutputData().put("result", local);

        runner.uploadFilesToFileStorage(result, "wf-1", "task-1");

        assertEquals(local, result.getOutputData().get("result"));
    }

    @Test
    @DisplayName("uploadFilesToFileStorage ignores non-FileHandler outputData values")
    void uploadFilesToFileStorage_ignoresNonFileHandlerValues() {
        FileHandler uploaded = mock(FileHandler.class);
        when(uploaded.getFileHandleId()).thenReturn(FILE_ID);
        when(fileClient.upload(any(), any(Path.class), any(FileUploadOptions.class)))
                .thenReturn(uploaded);

        TaskResult result = new TaskResult();
        FileHandler local = FileHandler.fromLocalFile(Path.of("/tmp/a.pdf"), "application/pdf");
        result.getOutputData().put("text", "hello");
        result.getOutputData().put("count", 42);
        result.getOutputData().put("file", local);

        runner.uploadFilesToFileStorage(result, "wf-1", "task-1");

        verify(fileClient, times(1))
                .upload(eq("wf-1"), eq(Path.of("/tmp/a.pdf")), any(FileUploadOptions.class));
        assertEquals("hello", result.getOutputData().get("text"));
        assertEquals(42, result.getOutputData().get("count"));
        assertSame(uploaded, result.getOutputData().get("file"));
    }

    @SuppressWarnings("unchecked")
    private static TaskRunner newRunner(Worker worker, FileClient fileClient) {
        return new TaskRunner(
                worker,
                mock(TaskClient.class),
                3,
                Map.of(),
                "test-worker-",
                1,
                100,
                List.of(),
                (EventDispatcher<TaskRunnerEvent>) mock(EventDispatcher.class),
                false,
                fileClient);
    }

    static class PreUploadedFileHandler implements FileHandler {
        private final String id;
        PreUploadedFileHandler(String id) { this.id = id; }
        @Override public String getFileHandleId() { return id; }
        @Override public InputStream getInputStream() { return null; }
        @Override public String getFileName() { return "pre.bin"; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public long getFileSize() { return 0L; }
    }
}
