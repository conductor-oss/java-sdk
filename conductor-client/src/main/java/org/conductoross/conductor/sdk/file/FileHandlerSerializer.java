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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serializes any {@link FileHandler} as a fixed three-field JSON object:
 * {@code {fileHandleId, contentType, fileName}}. Other accessors on the interface
 * (e.g. {@code getInputStream}, {@code getFileSize}) and impl-specific getters are not emitted.
 */
public class FileHandlerSerializer extends JsonSerializer<FileHandler> {

    @Override
    public void serialize(FileHandler handler, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("fileHandleId", handler.getFileHandleId());
        gen.writeStringField("contentType", handler.getContentType());
        gen.writeStringField("fileName", handler.getFileName());
        gen.writeEndObject();
    }
}
