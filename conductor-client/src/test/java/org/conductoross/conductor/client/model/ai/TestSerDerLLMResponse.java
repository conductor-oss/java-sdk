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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerDerLLMResponse {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper nonNullMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void testSerializationDeserialization() throws Exception {
        // 1. Unmarshal SERVER_JSON to SDK POJO
        String SERVER_JSON = JsonTemplateSerDeserResolverUtil.getJsonString("LLMResponseV2");
        LLMResponse llmResponse = objectMapper.readValue(SERVER_JSON, LLMResponse.class);

        // 2. Assert that the fields are all correctly populated
        assertNotNull(llmResponse);
        assertNotNull(llmResponse.getResult());
        assertNotNull(llmResponse.getFinishReason());
        assertEquals("sample_finishReason", llmResponse.getFinishReason());
        assertEquals(123, llmResponse.getTokenUsed());
        assertEquals(50, llmResponse.getPromptTokens());
        assertEquals(73, llmResponse.getCompletionTokens());
        assertEquals("sample_jobId", llmResponse.getJobId());

        // Check media list
        assertNotNull(llmResponse.getMedia());
        assertEquals(1, llmResponse.getMedia().size());
        assertEquals("https://example.com/media.mp3", llmResponse.getMedia().get(0).getLocation());
        assertEquals("audio/mpeg", llmResponse.getMedia().get(0).getMimeType());

        // Check toolCalls
        assertNotNull(llmResponse.getToolCalls());
        assertEquals(1, llmResponse.getToolCalls().size());
        assertTrue(llmResponse.hasToolCalls());
        assertEquals("sample_tool", llmResponse.getToolCalls().get(0).getName());

        // workflow is null in the template
        assertNull(llmResponse.getWorkflow());

        // 3. Marshall this POJO to JSON again (exclude nulls for round-trip comparison
        // since Media.data and LLMResponse.workflow are null and not in the template)
        String serializedJson = nonNullMapper.writeValueAsString(llmResponse);

        // 4. Compare the JSONs - nothing should be lost
        assertEquals(objectMapper.readTree(SERVER_JSON), objectMapper.readTree(serializedJson));
    }
}
