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
package com.netflix.conductor.common.metadata.tasks;

import java.util.Map;
import java.util.UUID;

import org.conductoross.conductor.sdk.file.FileHandler;
import org.conductoross.conductor.sdk.file.FileStorageException;
import org.conductoross.conductor.sdk.file.WorkflowFileClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TaskGetInputFileHandlerTest {

    private static final String FILE_ID = "conductor://file/" + UUID.randomUUID();

    @Test
    void wrapsRawStringFileHandleId() {
        Task task = newTask(Map.of("file", FILE_ID));

        FileHandler handler = task.getInputFileHandler("file");

        assertEquals(FILE_ID, handler.getFileHandleId());
    }

    @Test
    void wrapsJsonObjectWithFileHandleId() {
        Map<String, Object> serialized = Map.of(
                "fileHandleId", FILE_ID,
                "fileName", "doc.pdf",
                "contentType", "application/pdf");
        Task task = newTask(Map.of("file", serialized));

        FileHandler handler = task.getInputFileHandler("file");

        assertEquals(FILE_ID, handler.getFileHandleId());
    }

    @Test
    void throwsWhenStringLacksPrefix() {
        Task task = newTask(Map.of("file", "not-a-file-id"));

        assertThrows(FileStorageException.class, () -> task.getInputFileHandler("file"));
    }

    @Test
    void throwsWhenJsonObjectMissingFileHandleId() {
        Task task = newTask(Map.of("file", Map.of("fileName", "x.pdf")));

        assertThrows(FileStorageException.class, () -> task.getInputFileHandler("file"));
    }

    @Test
    void throwsWhenJsonObjectFileHandleIdLacksPrefix() {
        Task task = newTask(Map.of("file", Map.of("fileHandleId", "raw-uuid")));

        assertThrows(FileStorageException.class, () -> task.getInputFileHandler("file"));
    }

    @Test
    void throwsWhenKeyAbsent() {
        Task task = newTask(Map.of());

        assertThrows(FileStorageException.class, () -> task.getInputFileHandler("file"));
    }

    private static Task newTask(Map<String, Object> input) {
        Task task = new Task();
        task.setInputData(input);
        task.setWorkflowFileClient(mock(WorkflowFileClient.class));
        return task;
    }
}
