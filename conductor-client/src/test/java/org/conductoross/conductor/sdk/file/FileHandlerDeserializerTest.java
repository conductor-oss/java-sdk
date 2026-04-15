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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.config.ObjectMapperProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


class FileHandlerDeserializerTest {

    private static final String FILE_HANDLE_ID = "conductor://file/abc-123";

    private final ObjectMapper mapper = new ObjectMapperProvider().getObjectMapper();
    private WorkflowFileClient client;

    @BeforeEach
    void setUp() {
        client = mock(WorkflowFileClient.class);
    }

    @Test
    void deserializesRawStringToManagedFileHandler() throws Exception {
        FileHandler handler = readerWithClient().readValue("\"" + FILE_HANDLE_ID + "\"");

        assertInstanceOf(ManagedFileHandler.class, handler);
        assertEquals(FILE_HANDLE_ID, handler.getFileHandleId());
    }

    @Test
    void deserializesJsonObjectWithFileHandleIdToManagedFileHandler() throws Exception {
        String json = """
                {
                    "fileHandleId": "%s",
                    "fileName": "doc.pdf",
                    "contentType": "application/pdf"
                }
                """.formatted(FILE_HANDLE_ID);

        FileHandler handler = readerWithClient().readValue(json);

        assertInstanceOf(ManagedFileHandler.class, handler);
        assertEquals(FILE_HANDLE_ID, handler.getFileHandleId());
    }

    @Test
    void throwsWhenStringLacksPrefix() {
        assertThrows(FileStorageException.class,
                () -> readerWithClient().readValue("\"raw-uuid\""));
    }

    @Test
    void throwsWhenJsonObjectFileHandleIdLacksPrefix() {
        String json = "{\"fileHandleId\":\"raw-uuid\"}";

        assertThrows(FileStorageException.class, () -> readerWithClient().readValue(json));
    }

    @Test
    void throwsWhenJsonObjectMissingFileHandleId() {
        String json = "{\"fileName\":\"doc.pdf\"}";

        assertThrows(FileStorageException.class, () -> readerWithClient().readValue(json));
    }

    @Test
    void throwsWhenWorkflowFileClientAttributeMissing() {
        // No .withAttribute(...) — context attribute is null.
        ObjectReader readerWithoutClient = mapper.readerFor(FileHandler.class);

        assertThrows(FileStorageException.class,
                () -> readerWithoutClient.readValue("\"" + FILE_HANDLE_ID + "\""));
    }

    private ObjectReader readerWithClient() {
        return mapper.readerFor(FileHandler.class)
                .withAttribute(FileHandlerDeserializer.WORKFLOW_FILE_CLIENT_ATTR, client);
    }
}
