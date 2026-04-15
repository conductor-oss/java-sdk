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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserializes a {@link FileHandler} field on any POJO when the source JSON is either the raw
 * {@code conductor://file/<id>} string or a {@link FileHandlerSerializer}-shaped JSON object.
 *
 * <p>A {@link WorkflowFileClient} must be supplied via the {@code DeserializationContext}
 * attribute keyed by {@link #WORKFLOW_FILE_CLIENT_ATTR}; the task runner's {@code AnnotatedWorker}
 * sets this per-task before binding worker input. Without it this deserializer cannot construct
 * a {@link ManagedFileHandler} and fails.
 */
public class FileHandlerDeserializer extends JsonDeserializer<FileHandler> {

    public static final String WORKFLOW_FILE_CLIENT_ATTR =
            "org.conductoross.conductor.sdk.file.workflowFileClient";

    @Override
    public FileHandler deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Object raw = p.readValueAs(Object.class);
        String fileHandleId = FileHandler.extractFileHandleId(raw);
        if (!FileHandler.isFileHandleId(fileHandleId)) {
            throw new FileStorageException(
                    "Expected " + FileHandler.PREFIX + " reference, got: " + raw);
        }
        Object attr = ctxt.getAttribute(WORKFLOW_FILE_CLIENT_ATTR);
        if (!(attr instanceof WorkflowFileClient client)) {
            throw new FileStorageException(
                    "FileHandler input requires a WorkflowFileClient in deserialization context");
        }
        return new ManagedFileHandler(fileHandleId, client);
    }
}
