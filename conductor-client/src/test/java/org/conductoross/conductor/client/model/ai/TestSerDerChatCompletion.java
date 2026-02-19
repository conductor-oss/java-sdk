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

public class TestSerDerChatCompletion {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializationDeserialization() throws Exception {
        String SERVER_JSON = JsonTemplateSerDeserResolverUtil.getJsonString("ChatCompletionV2");
        ChatCompletion chatCompletion = objectMapper.readValue(SERVER_JSON, ChatCompletion.class);

        // Verify own fields
        assertEquals("sample_instructions", chatCompletion.getInstructions());
        assertTrue(chatCompletion.isJsonOutput());
        assertFalse(chatCompletion.isGoogleSearchRetrieval());
        assertEquals("sample_userInput", chatCompletion.getUserInput());
        assertEquals("application/json", chatCompletion.getOutputMimeType());
        assertEquals(1000, chatCompletion.getThinkingTokenLimit());
        assertEquals("medium", chatCompletion.getReasoningEffort());
        assertEquals("sample_location", chatCompletion.getOutputLocation());
        assertEquals("alloy", chatCompletion.getVoice());

        // Verify messages list
        assertNotNull(chatCompletion.getMessages());
        assertEquals(1, chatCompletion.getMessages().size());
        ChatMessage message = chatCompletion.getMessages().get(0);
        assertEquals(ChatMessage.Role.user, message.getRole());
        assertEquals("sample_message", message.getMessage());

        // Verify tools list
        assertNotNull(chatCompletion.getTools());
        assertEquals(1, chatCompletion.getTools().size());
        ToolSpec tool = chatCompletion.getTools().get(0);
        assertEquals("sample_tool", tool.getName());
        assertEquals("SIMPLE", tool.getType());
        assertEquals("A sample tool", tool.getDescription());

        // Verify participants map
        assertNotNull(chatCompletion.getParticipants());
        assertEquals(1, chatCompletion.getParticipants().size());
        assertEquals(ChatMessage.Role.assistant, chatCompletion.getParticipants().get("agent1"));

        // Verify inherited fields from LLMWorkerInput
        assertEquals("sample_llmProvider", chatCompletion.getLlmProvider());
        assertEquals("sample_model", chatCompletion.getModel());
        assertEquals(123.456, chatCompletion.getTemperature(), 0.001);

        // Note: getPrompt() returns instructions (not the parent's prompt field),
        // which is intentional per server-side ChatCompletion behavior.
        assertEquals("sample_instructions", chatCompletion.getPrompt());

        // Verify round-trip: since getPrompt() overrides to return instructions,
        // the re-serialized JSON will have "prompt" = "sample_instructions" instead of
        // the original inherited "sample_prompt". We compare field-by-field instead of
        // full JSON equality.
        String serializedJson = objectMapper.writeValueAsString(chatCompletion);
        ChatCompletion deserialized2 = objectMapper.readValue(serializedJson, ChatCompletion.class);
        assertEquals(chatCompletion.getInstructions(), deserialized2.getInstructions());
        assertEquals(chatCompletion.isJsonOutput(), deserialized2.isJsonOutput());
        assertEquals(chatCompletion.getMessages().size(), deserialized2.getMessages().size());
        assertEquals(chatCompletion.getLlmProvider(), deserialized2.getLlmProvider());
        assertEquals(chatCompletion.getModel(), deserialized2.getModel());
        assertEquals(chatCompletion.getVoice(), deserialized2.getVoice());
        assertEquals(chatCompletion.getThinkingTokenLimit(), deserialized2.getThinkingTokenLimit());
    }
}
