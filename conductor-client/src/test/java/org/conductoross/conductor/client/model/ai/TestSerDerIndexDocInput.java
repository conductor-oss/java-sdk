/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.client.model.ai;

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.util.JsonTemplateSerDeserResolverUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerDerIndexDocInput {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializationDeserialization() throws Exception {
        // 1. Unmarshal SERVER_JSON to SDK POJO
        String SERVER_JSON = JsonTemplateSerDeserResolverUtil.getJsonString("IndexDocInputV2");
        IndexDocInput indexDocInput = objectMapper.readValue(SERVER_JSON, IndexDocInput.class);

        // 2. Assert that the fields are all correctly populated
        assertNotNull(indexDocInput);

        // Own fields
        assertNotNull(indexDocInput.getEmbeddingModelProvider());
        assertNotNull(indexDocInput.getEmbeddingModel());
        assertNotNull(indexDocInput.getVectorDB());
        assertNotNull(indexDocInput.getText());
        assertNotNull(indexDocInput.getDocId());
        assertNotNull(indexDocInput.getUrl());
        assertNotNull(indexDocInput.getMediaType());
        assertNotNull(indexDocInput.getNamespace());
        assertNotNull(indexDocInput.getIndex());
        assertNotNull(indexDocInput.getIntegrationName());
        assertEquals("sample_integrationName", indexDocInput.getIntegrationName());

        // Test int fields
        assertEquals(indexDocInput.getChunkSize(), indexDocInput.getChunkSize() > 0 ? indexDocInput.getChunkSize() : 12000);
        assertEquals(indexDocInput.getChunkOverlap(), indexDocInput.getChunkOverlap() > 0 ? indexDocInput.getChunkOverlap() : 400);

        // Test Map field
        assertNotNull(indexDocInput.getMetadata());

        // Test Integer field
        if (indexDocInput.getDimensions() != null) {
            assertTrue(indexDocInput.getDimensions() > 0);
        }

        // Inherited fields from LLMWorkerInput
        assertEquals("sample_llmProvider", indexDocInput.getLlmProvider());
        assertEquals("sample_model", indexDocInput.getModel());

        // 3. Marshal this POJO to JSON again
        String serializedJson = objectMapper.writeValueAsString(indexDocInput);

        // 4. Compare the JSONs - nothing should be lost
        assertEquals(objectMapper.readTree(SERVER_JSON), objectMapper.readTree(serializedJson));
    }
}
