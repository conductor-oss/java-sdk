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
package io.orkes.conductor.client;

import java.util.List;
import java.util.Map;

import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.integration.ai.PromptTemplate;

/**
 * Client for managing prompt templates used with AI/LLM integrations.
 * <p>
 * Prompt templates support versioning, allowing multiple versions of the same prompt to be maintained.
 * Templates can include variables that are replaced at runtime and can be associated with specific AI models.
 */
public interface PromptClient {

    // Basic CRUD operations

    /**
     * Creates or updates a prompt template.
     * <p>
     * This is a simplified method that creates a new version or updates the latest version.
     * For more control over versioning, use {@link #savePrompt(String, String, String, List, Integer, boolean)}.
     *
     * @param promptName the name of the prompt template
     * @param description a description of what the prompt does
     * @param promptTemplate the template content with optional variable placeholders
     */
    void savePrompt(String promptName, String description, String promptTemplate);

    /**
     * Creates or updates a prompt template with full control over versioning and model associations.
     *
     * @param promptName the name of the prompt template
     * @param description a description of what the prompt does
     * @param promptTemplate the template content with optional variable placeholders
     * @param models optional list of AI model names along with integrations this prompt is compatible with (e.g., ["openai:gpt-4", "anthropic:sonnet-4.5"])
     * @param version specific version number to create or update, or null to update the latest version
     * @param autoIncrement if true, automatically creates a new version instead of updating existing
     */
    void savePrompt(String promptName, String description, String promptTemplate, List<String> models, Integer version, boolean autoIncrement);

    /**
     * Updates an existing prompt template at a specific version.
     *
     * @param promptName the name of the prompt template
     * @param version the version number to update
     * @param description a description of what the prompt does
     * @param promptTemplate the template content with optional variable placeholders
     * @param models optional list of AI model names this prompt is compatible with
     */
    void updatePrompt(String promptName, Integer version, String description, String promptTemplate, List<String> models);

    /**
     * Creates multiple prompt templates in a single bulk operation.
     *
     * @param prompts list of prompt templates to create
     * @param newVersion if true, creates new versions for existing prompts; if false, updates existing versions
     */
    void savePrompts(List<PromptTemplate> prompts, boolean newVersion);

    /**
     * Retrieves the latest version of a prompt template by name.
     *
     * @param promptName the name of the prompt template
     * @return the prompt template with the highest version number
     */
    PromptTemplate getPrompt(String promptName);

    /**
     * Retrieves a specific version of a prompt template.
     *
     * @param promptName the name of the prompt template
     * @param version the version number to retrieve
     * @return the prompt template at the specified version
     */
    PromptTemplate getPrompt(String promptName, Integer version);

    /**
     * Retrieves all versions of a specific prompt template.
     *
     * @param promptName the name of the prompt template
     * @return list of all versions of the prompt template, ordered by version number
     */
    List<PromptTemplate> getAllPromptVersions(String promptName);

    /**
     * Retrieves all prompt templates (latest versions only).
     *
     * @return list of all prompt templates
     */
    List<PromptTemplate> getPrompts();

    /**
     * Deletes all versions of a prompt template.
     *
     * @param promptName the name of the prompt template to delete
     */
    void deletePrompt(String promptName);

    /**
     * Deletes a specific version of a prompt template.
     *
     * @param promptName the name of the prompt template
     * @param version the version number to delete
     */
    void deletePrompt(String promptName, Integer version);

    // Tag management

    /**
     * Retrieves all tags associated with a prompt template.
     *
     * @param promptName the name of the prompt template
     * @return list of tags for the prompt template
     */
    List<TagObject> getTagsForPromptTemplate(String promptName);

    /**
     * Adds or updates tags for a prompt template.
     *
     * @param promptName the name of the prompt template
     * @param tags the list of tags to add or update
     */
    void updateTagForPromptTemplate(String promptName, List<TagObject> tags);

    /**
     * Deletes specific tags from a prompt template.
     *
     * @param promptName the name of the prompt template
     * @param tags the list of tags to delete
     */
    void deleteTagForPromptTemplate(String promptName, List<TagObject> tags);

    // Testing

    /**
     * Tests a prompt template by substituting variables and processing through the specified AI model.
     * <p>
     * This allows you to validate prompt templates before saving them or using them in workflows.
     *
     * @param promptText the text of the prompt template with variable placeholders
     * @param variables a map containing variables to be replaced in the template
     * @param aiIntegration the name of the AI integration provider (e.g., "openai")
     * @param textCompleteModel the AI model to use for completing text (e.g., "gpt-4")
     * @param temperature the randomness of the output, typically 0.0 to 1.0 (default is 0.1)
     * @param topP the nucleus sampling probability mass (default is 0.9)
     * @param stopWords optional list of words at which to stop generating further text
     * @return the processed prompt text after variable substitution and AI model processing
     */
    String testPrompt(String promptText, Map<String, Object> variables, String aiIntegration,
        String textCompleteModel, float temperature, float topP, List<String> stopWords);
}
