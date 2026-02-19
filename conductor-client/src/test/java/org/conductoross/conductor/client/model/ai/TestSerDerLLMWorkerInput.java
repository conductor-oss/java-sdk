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

import java.util.List;

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.util.JsonTemplateSerDeserResolverUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerDerLLMWorkerInput {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializationDeserialization() throws Exception {
        // 1. Unmarshal SERVER_JSON to SDK POJO
        String SERVER_JSON = JsonTemplateSerDeserResolverUtil.getJsonString("LLMWorkerInputV2");
        LLMWorkerInput llmWorkerInput = objectMapper.readValue(SERVER_JSON, LLMWorkerInput.class);

        // 2. Assert that the fields are all correctly populated
        assertNotNull(llmWorkerInput);
        assertEquals("sample_llmProvider", llmWorkerInput.getLlmProvider());
        assertEquals("sample_model", llmWorkerInput.getModel());
        assertEquals("sample_integrationName", llmWorkerInput.getIntegrationName());
        assertEquals("sample_prompt", llmWorkerInput.getPrompt());
        assertEquals(Integer.valueOf(2), llmWorkerInput.getPromptVersion());
        assertNotNull(llmWorkerInput.getPromptVariables());
        assertEquals("value1", llmWorkerInput.getPromptVariables().get("var1"));
        assertEquals(123.456, llmWorkerInput.getTemperature(), 0.001);
        assertEquals(0.5, llmWorkerInput.getFrequencyPenalty(), 0.001);
        assertEquals(123.456, llmWorkerInput.getTopP(), 0.001);
        assertEquals(Integer.valueOf(10), llmWorkerInput.getTopK());
        assertEquals(0.3, llmWorkerInput.getPresencePenalty(), 0.001);
        assertTrue(llmWorkerInput.isAllowRawPrompts());

        // Check the list values
        List<String> stopWords = llmWorkerInput.getStopWords();
        assertNotNull(stopWords);
        assertEquals(1, stopWords.size());

        // Checking maxTokens and maxResults
        assertEquals(Integer.valueOf(123), llmWorkerInput.getMaxTokens());
        assertEquals(123, llmWorkerInput.getMaxResults());

        // Check integrationNames (custom getter auto-populates AI_MODEL)
        assertNotNull(llmWorkerInput.getIntegrationNames());
        assertEquals("sample_llmProvider", llmWorkerInput.getIntegrationNames().get("AI_MODEL"));

        // 3. Marshall this POJO to JSON again
        String serializedJson = objectMapper.writeValueAsString(llmWorkerInput);

        // 4. Compare the JSONs - nothing should be lost
        assertEquals(objectMapper.readTree(SERVER_JSON), objectMapper.readTree(serializedJson));
    }
}
