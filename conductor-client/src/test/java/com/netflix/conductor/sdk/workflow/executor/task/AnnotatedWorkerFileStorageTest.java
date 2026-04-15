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
package com.netflix.conductor.sdk.workflow.executor.task;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.WorkflowFileClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AnnotatedWorkerFileStorageTest {

    private static final String FILE_ID = "conductor://file/" + UUID.randomUUID();

    // --- Workers ---

    static class FileReturnWorker {
        @WorkerTask("file_return")
        public @OutputParam("result") FileHandler doWork() {
            return FileHandler.fromLocalFile(Path.of("/tmp/out.pdf"), "application/pdf");
        }
    }

    static class FileReturnCustomKeyWorker {
        @WorkerTask("file_return_custom_key")
        public @OutputParam("doc") FileHandler doWork() {
            return FileHandler.fromLocalFile(Path.of("/tmp/out.pdf"), "application/pdf");
        }
    }

    static class AlreadyUploadedFileReturnWorker {
        @WorkerTask("file_return_already_uploaded")
        public @OutputParam("result") FileHandler doWork() {
            return new PreUploadedFileHandler(FILE_ID);
        }
    }

    static class FileInputWorker {
        @WorkerTask("file_input")
        public @OutputParam("got") String doWork(@InputParam("file") FileHandler file) {
            return file.getFileHandleId();
        }
    }

    // POJO carrying a FileHandler field — exercises Jackson deserializer path.
    // Public so Jackson's Afterburner module can generate an accessor class for it.
    public static class Attachment {
        public String label;
        public FileHandler file;
    }

    public static class AttachmentInputWorker {
        @WorkerTask("attachment_input")
        public @OutputParam("got") String doWork(@InputParam("att") Attachment att) {
            return att.label + ":" + (att.file == null ? "null" : att.file.getFileHandleId());
        }
    }

    // Non-ManagedFileHandler impl w/ a non-null handle id — simulates a worker
    // returning a FileHandler that was already uploaded upstream.
    static class PreUploadedFileHandler implements FileHandler {
        private final String id;
        PreUploadedFileHandler(String id) { this.id = id; }
        @Override public String getFileHandleId() { return id; }
        @Override public InputStream getInputStream() { return null; }
        @Override public String getFileName() { return "pre.bin"; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public long getFileSize() { return 0L; }
    }

    // --- setValue tests ---

    @Test
    @DisplayName("setValue puts LocalFileHandler in outputData without uploading (TaskRunner handles upload)")
    void setValue_putsFileHandlerInOutput_whenFileIdNull() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setWorkflowFileClient(workflowFileClient);

        FileReturnWorker bean = new FileReturnWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_return", bean.getClass().getMethod("doWork"), bean);

        TaskResult result = worker.execute(task);

        verify(workflowFileClient, never()).upload(any(Path.class), any());
        Object value = result.getOutputData().get("result");
        assertInstanceOf(FileHandler.class, value);
        assertNull(((FileHandler) value).getFileHandleId());
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("setValue puts pre-uploaded FileHandler in outputData unchanged")
    void setValue_putsFileHandlerInOutput_whenFileIdSet() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setWorkflowFileClient(workflowFileClient);

        AlreadyUploadedFileReturnWorker bean = new AlreadyUploadedFileReturnWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_return_already_uploaded", bean.getClass().getMethod("doWork"), bean);

        TaskResult result = worker.execute(task);

        verify(workflowFileClient, never()).upload(any(Path.class), any());
        Object value = result.getOutputData().get("result");
        assertInstanceOf(FileHandler.class, value);
        assertEquals(FILE_ID, ((FileHandler) value).getFileHandleId());
    }

    @Test
    @DisplayName("setValue honors @OutputParam value as outputData key for FileHandler return")
    void setValue_usesOutputParamKey() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setWorkflowFileClient(workflowFileClient);

        FileReturnCustomKeyWorker bean = new FileReturnCustomKeyWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_return_custom_key", bean.getClass().getMethod("doWork"), bean);

        TaskResult result = worker.execute(task);

        assertInstanceOf(FileHandler.class, result.getOutputData().get("doc"));
        assertNull(result.getOutputData().get("result"));
    }

    // --- getInputValue tests ---

    @Test
    @DisplayName("getInputValue wraps conductor://file/ id as ManagedFileHandler")
    void getInputValue_wrapsFileIdAsManagedFileHandler() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        FileInputWorker bean = new FileInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_input", bean.getClass().getMethod("doWork", FileHandler.class), bean);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("file", FILE_ID));
        task.setWorkflowFileClient(workflowFileClient);

        TaskResult result = worker.execute(task);

        assertEquals(FILE_ID, result.getOutputData().get("got"));
    }

    @Test
    @DisplayName("getInputValue accepts JSON object with fileHandleId and wraps as ManagedFileHandler")
    void getInputValue_acceptsJsonObjectWithFileHandleId() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        FileInputWorker bean = new FileInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_input", bean.getClass().getMethod("doWork", FileHandler.class), bean);

        Map<String, Object> serialized = Map.of(
                "fileHandleId", FILE_ID,
                "fileName", "doc.pdf",
                "contentType", "application/pdf");

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("file", serialized));
        task.setWorkflowFileClient(workflowFileClient);

        TaskResult result = worker.execute(task);

        assertEquals(FILE_ID, result.getOutputData().get("got"));
    }

    @Test
    @DisplayName("getInputValue throws when JSON object lacks fileHandleId")
    void getInputValue_throws_whenJsonObjectMissingFileHandleId() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        FileInputWorker bean = new FileInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_input", bean.getClass().getMethod("doWork", FileHandler.class), bean);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("file", Map.of("fileName", "x.pdf")));
        task.setWorkflowFileClient(workflowFileClient);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> worker.execute(task));
        assertInstanceOf(FileStorageException.class, thrown.getCause());
    }

    @Test
    @DisplayName("getInputValue throws FileStorageException when FileHandler param value is not a file id")
    void getInputValue_throwsFileStorageException_whenValueIsNotFileId() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        FileInputWorker bean = new FileInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_input", bean.getClass().getMethod("doWork", FileHandler.class), bean);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("file", "not-a-file-id"));
        task.setWorkflowFileClient(workflowFileClient);

        // FileStorageException is thrown during argument resolution, caught by the
        // outer catch(Exception) in execute() and rewrapped as RuntimeException.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> worker.execute(task));
        assertInstanceOf(FileStorageException.class, thrown.getCause());
    }

    @Test
    @DisplayName("POJO field FileHandler is bound from JSON object form")
    void pojoFieldFileHandler_fromJsonObject() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        AttachmentInputWorker bean = new AttachmentInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "attachment_input", bean.getClass().getMethod("doWork", Attachment.class), bean);

        Map<String, Object> attachment = Map.of(
                "label", "invoice",
                "file", Map.of(
                        "fileHandleId", FILE_ID,
                        "fileName", "inv.pdf",
                        "contentType", "application/pdf"));

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("att", attachment));
        task.setWorkflowFileClient(workflowFileClient);

        TaskResult result = worker.execute(task);

        assertEquals("invoice:" + FILE_ID, result.getOutputData().get("got"));
    }

    @Test
    @DisplayName("POJO field FileHandler is bound from raw conductor://file/ string")
    void pojoFieldFileHandler_fromRawString() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        AttachmentInputWorker bean = new AttachmentInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "attachment_input", bean.getClass().getMethod("doWork", Attachment.class), bean);

        Map<String, Object> attachment = Map.of("label", "raw", "file", FILE_ID);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("att", attachment));
        task.setWorkflowFileClient(workflowFileClient);

        TaskResult result = worker.execute(task);

        assertEquals("raw:" + FILE_ID, result.getOutputData().get("got"));
    }

    @Test
    @DisplayName("POJO field FileHandler throws when value is not a valid reference")
    void pojoFieldFileHandler_throwsOnInvalidValue() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        AttachmentInputWorker bean = new AttachmentInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "attachment_input", bean.getClass().getMethod("doWork", Attachment.class), bean);

        Map<String, Object> attachment = Map.of("label", "bad", "file", "not-a-file-id");

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("att", attachment));
        task.setWorkflowFileClient(workflowFileClient);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> worker.execute(task));
        assertInstanceOf(FileStorageException.class, rootCause(thrown));
    }

    private static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Test
    @DisplayName("getInputValue returns null FileHandler when input key is absent")
    void getInputValue_returnsNull_whenValueMissing() throws NoSuchMethodException {
        WorkflowFileClient workflowFileClient = mock(WorkflowFileClient.class);

        FileInputWorker bean = new FileInputWorker();
        AnnotatedWorker worker = new AnnotatedWorker(
                "file_input", bean.getClass().getMethod("doWork", FileHandler.class), bean);

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of());
        task.setWorkflowFileClient(workflowFileClient);

        // Worker body dereferences a null FileHandler → NPE → FAILED
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }
}
