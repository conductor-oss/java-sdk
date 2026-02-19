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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerDerChatMessage {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializationDeserialization() throws Exception {
        // 1. Unmarshal SERVER_JSON to SDK POJO
        String SERVER_JSON = JsonTemplateSerDeserResolverUtil.getJsonString("ChatMessageV2");
        ChatMessage chatMessage = objectMapper.readValue(SERVER_JSON, ChatMessage.class);

        // 2. Assert that the fields are all correctly populated
        assertNotNull(chatMessage);
        assertEquals(ChatMessage.Role.user, chatMessage.getRole());
        assertEquals("sample_message", chatMessage.getMessage());

        // Check new fields
        assertNotNull(chatMessage.getMedia());
        assertEquals(1, chatMessage.getMedia().size());
        assertEquals("https://example.com/image.png", chatMessage.getMedia().get(0));

        assertEquals("text/plain", chatMessage.getMimeType());

        assertNotNull(chatMessage.getToolCalls());
        assertEquals(1, chatMessage.getToolCalls().size());
        ToolCall toolCall = chatMessage.getToolCalls().get(0);
        assertEquals("sample_taskRef", toolCall.getTaskReferenceName());
        assertEquals("sample_tool", toolCall.getName());

        // 3. Marshall this POJO to JSON again
        String regeneratedJson = objectMapper.writeValueAsString(chatMessage);

        // 4. Compare the JSONs - nothing should be lost
        JsonNode originalJsonNode = objectMapper.readTree(SERVER_JSON);
        JsonNode regeneratedJsonNode = objectMapper.readTree(regeneratedJson);

        assertEquals(originalJsonNode, regeneratedJsonNode,
                "The original and regenerated JSON should be equivalent");

        // Additional verification by deserializing the regenerated JSON
        ChatMessage regeneratedChatMessage = objectMapper.readValue(regeneratedJson, ChatMessage.class);
        assertEquals(chatMessage.getRole(), regeneratedChatMessage.getRole());
        assertEquals(chatMessage.getMessage(), regeneratedChatMessage.getMessage());
        assertEquals(chatMessage.getMedia(), regeneratedChatMessage.getMedia());
        assertEquals(chatMessage.getMimeType(), regeneratedChatMessage.getMimeType());
    }
}
