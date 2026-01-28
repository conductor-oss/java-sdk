/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.client.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import io.orkes.conductor.client.IntegrationClient;
import io.orkes.conductor.client.PromptClient;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.integration.Category;
import io.orkes.conductor.client.model.integration.IntegrationApiUpdate;
import io.orkes.conductor.client.model.integration.IntegrationUpdate;
import io.orkes.conductor.client.model.integration.ai.PromptTemplate;
import io.orkes.conductor.client.util.ClientTestUtil;

public class PromptClientTests {
    private static final String PROMPT_NAME = "test-sdk-java-prompt";
    private static final String PROMPT_DESCRIPTION = "Test prompt for Java SDK";
    private static final String PROMPT_TEMPLATE_V1 = "Hello {{name}}, welcome to {{company}}!";
    private static final String PROMPT_TEMPLATE_V2 = "Hi {{name}}, welcome to {{company}}. How are you today?";

    private final PromptClient promptClient = ClientTestUtil.getOrkesClients().getPromptClient();
    private final IntegrationClient integrationClient = ClientTestUtil.getOrkesClients().getIntegrationClient();

    @BeforeEach
    void setup() {
        // Clean up any existing test prompts
        cleanup();
    }

    @AfterEach
    void cleanup() {
        try {
            promptClient.deletePrompt(PROMPT_NAME);
        } catch (ConductorClientException e) {
            // Ignore if prompt doesn't exist
            if (e.getStatus() != 404 && e.getStatus() != 500) {
                throw e;
            }
        }
    }

    @Test
    void testBasicCRUDOperations() {
        // Create a prompt
        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V1);

        // Get the prompt
        PromptTemplate prompt = promptClient.getPrompt(PROMPT_NAME);
        Assertions.assertNotNull(prompt);
        Assertions.assertEquals(PROMPT_NAME, prompt.getName());
        Assertions.assertEquals(PROMPT_DESCRIPTION, prompt.getDescription());
        Assertions.assertEquals(PROMPT_TEMPLATE_V1, prompt.getTemplate());

        // Verify it appears in list
        List<PromptTemplate> prompts = promptClient.getPrompts();
        Assertions.assertTrue(prompts.stream().anyMatch(p -> p.getName().equals(PROMPT_NAME)));

        // Delete the prompt
        promptClient.deletePrompt(PROMPT_NAME);

        // Verify deletion
        try {
            promptClient.getPrompt(PROMPT_NAME);
            Assertions.fail("Expected exception for non-existent prompt");
        } catch (ConductorClientException e) {
            Assertions.assertTrue(e.getStatus() == 404 || e.getStatus() == 500);
        }
    }

    @Test
    void testPromptVersioning() {
        // Create version 1
        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V1, null, 1, false);

        PromptTemplate v1 = promptClient.getPrompt(PROMPT_NAME, 1);
        Assertions.assertNotNull(v1);
        Assertions.assertEquals(PROMPT_TEMPLATE_V1, v1.getTemplate());
        Assertions.assertEquals(1, v1.getVersion());

        // Create version 2 with auto-increment
        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V2, null, null, true);

        // Get all versions
        List<PromptTemplate> versions = promptClient.getAllPromptVersions(PROMPT_NAME);
        Assertions.assertTrue(versions.size() >= 2);

        // Get latest version
        PromptTemplate latest = promptClient.getPrompt(PROMPT_NAME);
        Assertions.assertEquals(PROMPT_TEMPLATE_V2, latest.getTemplate());

        // Get specific version
        PromptTemplate v2 = promptClient.getPrompt(PROMPT_NAME, 2);
        Assertions.assertEquals(PROMPT_TEMPLATE_V2, v2.getTemplate());
        Assertions.assertEquals(2, v2.getVersion());

        // Delete specific version
        promptClient.deletePrompt(PROMPT_NAME, 1);

        // Verify version 1 is deleted but version 2 still exists
        try {
            promptClient.getPrompt(PROMPT_NAME, 1);
            Assertions.fail("Expected exception for deleted version");
        } catch (ConductorClientException e) {
            Assertions.assertTrue(e.getStatus() == 404 || e.getStatus() == 500);
        }

        PromptTemplate v2Still = promptClient.getPrompt(PROMPT_NAME, 2);
        Assertions.assertNotNull(v2Still);
    }

    @Test
    void testPromptWithModels() {

        IntegrationUpdate openai = new IntegrationUpdate();
        openai.setType("openai");
        openai.setCategory(Category.AI_MODEL);
        openai.setDescription("openai integration for testing");
        integrationClient.saveIntegration("openai", openai);

        IntegrationApiUpdate details = new IntegrationApiUpdate();
        details.setDescription("gpt-4 model");
        integrationClient.saveIntegrationApi("openai", "gpt-4", details);
        integrationClient.saveIntegrationApi("openai", "gpt-3.5-turbo", details);

        List<String> models = List.of("openai:gpt-4", "openai:gpt-3.5-turbo");

        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V1, models, null, false);

        PromptTemplate prompt = promptClient.getPrompt(PROMPT_NAME);
        Assertions.assertNotNull(prompt);
        Assertions.assertEquals(models.size(), prompt.getIntegrations().size());
        Assertions.assertTrue(prompt.getIntegrations().containsAll(models));
    }

    @Test
    void testUpdatePrompt() {
        // Create initial version
        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V1, null, 1, false);

        IntegrationUpdate openai = new IntegrationUpdate();
        openai.setType("anthropic");
        openai.setCategory(Category.AI_MODEL);
        openai.setDescription("anthropic integration for testing");
        integrationClient.saveIntegration("anthropic", openai);

        IntegrationApiUpdate details = new IntegrationApiUpdate();
        details.setDescription("claude-3 model");
        integrationClient.saveIntegrationApi("anthropic", "claude-3", details);


        // Update the prompt
        List<String> updatedModels = List.of("anthropic:claude-3");
        promptClient.updatePrompt(PROMPT_NAME, 1, "Updated description", PROMPT_TEMPLATE_V2, updatedModels);

        PromptTemplate updated = promptClient.getPrompt(PROMPT_NAME, 1);
        Assertions.assertEquals(PROMPT_TEMPLATE_V2, updated.getTemplate());
        Assertions.assertEquals("Updated description", updated.getDescription());
        Assertions.assertTrue(updated.getIntegrations().contains("anthropic:claude-3"));
    }

    @Test
    void testTagOperations() {
        // Create a prompt
        promptClient.savePrompt(PROMPT_NAME, PROMPT_DESCRIPTION, PROMPT_TEMPLATE_V1);

        // Add tags
        List<TagObject> tags = List.of(createTag("category", "greeting"), createTag("language", "english"));
        promptClient.updateTagForPromptTemplate(PROMPT_NAME, tags);

        // Get tags
        List<TagObject> retrievedTags = promptClient.getTagsForPromptTemplate(PROMPT_NAME);
        Assertions.assertEquals(2, retrievedTags.size());
        Assertions.assertTrue(containsTag(retrievedTags, "category", "greeting"));
        Assertions.assertTrue(containsTag(retrievedTags, "language", "english"));

        // Delete one tag
        promptClient.deleteTagForPromptTemplate(PROMPT_NAME, List.of(createTag("category", "greeting")));

        // Verify tag was deleted
        List<TagObject> remainingTags = promptClient.getTagsForPromptTemplate(PROMPT_NAME);
        Assertions.assertEquals(1, remainingTags.size());
        Assertions.assertTrue(containsTag(remainingTags, "language", "english"));
        Assertions.assertFalse(containsTag(remainingTags, "category", "greeting"));
    }

    @Test
    void testPromptTest() {
        // Note: This test requires a valid AI integration to be configured
        // You may need to skip this test if AI integration is not available
        String promptText = "Hello {{name}}!";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "World");

        try {
            // This will fail if no AI integration is configured, which is expected
            String result = promptClient.testPrompt(
                    promptText,
                    variables,
                    "openai", // Requires OpenAI integration to be configured
                    "gpt-3.5-turbo",
                    0.7f,
                    0.9f,
                    List.of("END"));

            // If we get here, verify result is not null
            Assertions.assertNotNull(result);
        } catch (ConductorClientException e) {
            // Expected if AI integration is not configured
            System.out.println("Skipping testPrompt as AI integration may not be configured: " + e.getMessage());
        }
    }

    @Test
    void testBulkSavePrompts() {
        PromptTemplate prompt1 = new PromptTemplate();
        prompt1.setName(PROMPT_NAME + "-bulk-1");
        prompt1.setDescription("Bulk prompt 1");
        prompt1.setTemplate("Template 1: {{var1}}");

        PromptTemplate prompt2 = new PromptTemplate();
        prompt2.setName(PROMPT_NAME + "-bulk-2");
        prompt2.setDescription("Bulk prompt 2");
        prompt2.setTemplate("Template 2: {{var2}}");

        try {
            // Save in bulk
            promptClient.savePrompts(List.of(prompt1, prompt2), false);

            // Verify both were created
            PromptTemplate retrieved1 = promptClient.getPrompt(PROMPT_NAME + "-bulk-1");
            PromptTemplate retrieved2 = promptClient.getPrompt(PROMPT_NAME + "-bulk-2");

            Assertions.assertNotNull(retrieved1);
            Assertions.assertNotNull(retrieved2);
            Assertions.assertEquals("Template 1: {{var1}}", retrieved1.getTemplate());
            Assertions.assertEquals("Template 2: {{var2}}", retrieved2.getTemplate());
        } finally {
            // Cleanup
            try {
                promptClient.deletePrompt(PROMPT_NAME + "-bulk-1");
            } catch (Exception e) {
                // Ignore
            }
            try {
                promptClient.deletePrompt(PROMPT_NAME + "-bulk-2");
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private TagObject createTag(String key, String value) {
        TagObject tag = new TagObject();
        tag.setKey(key);
        tag.setValue(value);
        return tag;
    }

    private boolean containsTag(List<TagObject> tags, String key, String value) {
        return tags.stream().anyMatch(tag -> tag.getKey().equals(key) && tag.getValue().equals(value));
    }
}
