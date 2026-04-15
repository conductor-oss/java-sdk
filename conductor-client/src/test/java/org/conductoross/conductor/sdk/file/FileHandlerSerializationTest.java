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
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.config.ObjectMapperProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileHandlerSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapperProvider().getObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> EXPECTED_FIELDS = Set.of("fileHandleId", "contentType", "fileName");

    @Test
    void localFileHandlerSerializesToThreeFields() {
        FileHandler handler = FileHandler.fromLocalFile(Path.of("docs/note.txt"), "text/plain");

        Map<String, Object> json = MAPPER.convertValue(handler, MAP_TYPE);

        assertEquals(EXPECTED_FIELDS, json.keySet());
        assertNull(json.get("fileHandleId")); // local handler not yet uploaded
        assertEquals("text/plain", json.get("contentType"));
        assertEquals("note.txt", json.get("fileName"));
    }

    @Test
    void stubManagedFileHandlerSerializesToThreeFields() {
        FileHandler handler = new StubManagedFileHandler(
                "conductor://file/abc-123", "report.pdf", "application/pdf");

        Map<String, Object> json = MAPPER.convertValue(handler, MAP_TYPE);

        assertEquals(EXPECTED_FIELDS, json.keySet());
        assertEquals("conductor://file/abc-123", json.get("fileHandleId"));
        assertEquals("application/pdf", json.get("contentType"));
        assertEquals("report.pdf", json.get("fileName"));
    }

    @Test
    void userImplSerializesToThreeFieldsOnly() throws Exception {
        FileHandler handler = new UserImpl();

        String jsonString = MAPPER.writeValueAsString(handler);
        Map<String, Object> json = MAPPER.readValue(jsonString, MAP_TYPE);

        assertEquals(EXPECTED_FIELDS, json.keySet());
        assertEquals("conductor://file/u-1", json.get("fileHandleId"));
        assertFalse(jsonString.contains("inputStream"));
        assertFalse(jsonString.contains("fileSize"));
        assertFalse(jsonString.contains("extra"));
    }

    @Test
    void serializesInsideTaskOutputMap() {
        FileHandler handler = new UserImpl();
        Map<String, Object> output = Map.of("file", handler, "answer", 42);

        Map<String, Object> json = MAPPER.convertValue(output, MAP_TYPE);

        @SuppressWarnings("unchecked")
        Map<String, Object> fileJson = (Map<String, Object>) json.get("file");
        assertEquals(EXPECTED_FIELDS, fileJson.keySet());
        assertEquals(42, json.get("answer"));
    }

    private static final class UserImpl implements FileHandler {
        @Override public String getFileHandleId() { return "conductor://file/u-1"; }
        @Override public InputStream getInputStream() { throw new UnsupportedOperationException(); }
        @Override public String getFileName() { return "u.bin"; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public long getFileSize() { return 7L; }
        public String getExtra() { return "should-not-leak"; }
    }

    /** Minimal {@link ManagedFileHandler}-like impl for testing without a workflow client. */
    private static final class StubManagedFileHandler implements FileHandler {
        private final String fileHandleId;
        private final String fileName;
        private final String contentType;
        StubManagedFileHandler(String id, String fileName, String contentType) {
            this.fileHandleId = id;
            this.fileName = fileName;
            this.contentType = contentType;
        }
        @Override public String getFileHandleId() { return fileHandleId; }
        @Override public InputStream getInputStream() { throw new UnsupportedOperationException(); }
        @Override public String getFileName() { return fileName; }
        @Override public String getContentType() { return contentType; }
        @Override public long getFileSize() { return 0L; }
    }
}
