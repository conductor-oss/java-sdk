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
import io.orkes.conductor.client.model.integration.Integration;
import io.orkes.conductor.client.model.integration.IntegrationApi;
import io.orkes.conductor.client.model.integration.IntegrationApiUpdate;
import io.orkes.conductor.client.model.integration.IntegrationUpdate;
import io.orkes.conductor.client.model.integration.ai.PromptTemplate;

/**
 * Client for managing integrations with external systems including AI/LLM
 * providers, databases, and message queues.
 * <p>
 * This interface provides methods to:
 * <ul>
 * <li>Manage integration providers (e.g., OpenAI, AWS Bedrock, PostgreSQL)</li>
 * <li>Configure integration APIs (e.g., specific models for LLM providers,
 * tables for databases)</li>
 * <li>Associate prompt templates with AI integrations</li>
 * <li>Track token usage for LLM integrations</li>
 * <li>Manage tags for integrations</li>
 * </ul>
 */
public interface IntegrationClient {

    // Integration Management

    /**
     * Retrieves details of a specific integration provider by name.
     *
     * @param integrationName the name of the integration provider to retrieve
     * @return the integration provider details
     */
    Integration getIntegration(String integrationName);

    /**
     * Retrieves all integration providers, optionally filtered by category and
     * active status.
     *
     * @param category   the category to filter by (e.g., "AI_MODEL", "VECTOR_DB",
     *                   "RELATIONAL_DB"), or null for all categories
     * @param activeOnly if true, only returns active integrations; if false,
     *                   returns all integrations
     * @return list of integration providers matching the filters
     */
    List<Integration> getIntegrations(String category, Boolean activeOnly);

    /**
     * Deletes an integration provider and all its associated APIs.
     *
     * @param integrationName the name of the integration provider to delete
     */
    void deleteIntegration(String integrationName);

    /**
     * Creates or updates an integration provider with the specified configuration.
     *
     * @param integrationName    the name of the integration provider
     * @param integrationDetails the configuration details for the integration
     */
    void saveIntegration(String integrationName, IntegrationUpdate integrationDetails);

    // Integration APIs (models for LLMs, database schemas/tables for DB etc)

    /**
     * Deletes a specific integration API (e.g., a specific LLM model or database
     * table).
     *
     * @param apiName         the name of the integration API to delete
     * @param integrationName the name of the parent integration provider
     */
    void deleteIntegrationApi(String apiName, String integrationName);

    /**
     * Retrieves details of a specific integration API.
     *
     * @param apiName         the name of the integration API
     * @param integrationName the name of the parent integration provider
     * @return the integration API details
     */
    IntegrationApi getIntegrationApi(String apiName, String integrationName);

    /**
     * Retrieves all integration APIs for a specific integration provider.
     *
     * @param integrationName the name of the integration provider
     * @return list of all integration APIs for the provider
     */
    List<IntegrationApi> getIntegrationApis(String integrationName);

    /**
     * Creates or updates an integration API with the specified configuration.
     *
     * @param integrationName the name of the parent integration provider
     * @param apiName         the name of the integration API
     * @param apiDetails      the configuration details for the integration API
     */
    void saveIntegrationApi(String integrationName, String apiName, IntegrationApiUpdate apiDetails);

    // Prompt Management

    /**
     * Associates a prompt template with a specific AI model integration.
     * <p>
     * This allows the prompt template to be used with the specified AI model when
     * executing LLM tasks.
     *
     * @param aiIntegration the name of the AI integration provider (e.g., "openai")
     * @param modelName     the name of the specific model (e.g., "gpt-4")
     * @param promptName    the name of the prompt template to associate
     */
    void associatePromptWithIntegration(String aiIntegration, String modelName, String promptName);

    /**
     * Retrieves all prompt templates associated with a specific AI model
     * integration.
     *
     * @param aiIntegration the name of the AI integration provider
     * @param modelName     the name of the specific model
     * @return list of prompt templates associated with the model
     */
    List<PromptTemplate> getPromptsWithIntegration(String aiIntegration, String modelName);

    // LLM metrics

    /**
     * Retrieves the token usage count for a specific integration API (e.g., a
     * specific LLM model).
     *
     * @param name            the name of the integration API
     * @param integrationName the name of the parent integration provider
     * @return the total number of tokens used
     */
    int getTokenUsageForIntegration(String name, String integrationName);

    /**
     * Retrieves token usage counts grouped by integration API for a specific
     * integration provider.
     *
     * @param name the name of the integration provider
     * @return map of integration API names to their token usage counts
     */
    Map<String, Integer> getTokenUsageForIntegrationProvider(String name);

    // Tags

    /**
     * Deletes specific tags from an integration.
     *
     * @param tags the list of tags to delete
     * @param name the name of the integration
     */
    void deleteTagForIntegration(List<TagObject> tags, String name);

    /**
     * Adds or updates tags for an integration.
     *
     * @param tags the list of tags to add or update
     * @param name the name of the integration
     */
    void saveTagForIntegration(List<TagObject> tags, String name);

    /**
     * Retrieves all tags associated with an integration.
     *
     * @param name the name of the integration
     * @return list of tags for the integration
     */
    List<TagObject> getTagsForIntegration(String name);
}
