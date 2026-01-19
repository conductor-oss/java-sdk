/*
 * Copyright 2022 Conductor Authors.
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

import java.util.List;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.WebhookConfig;

/**
 * Client for managing webhook configurations in Orkes Conductor.
 * Provides functionality to create, update, delete, and manage webhook configurations
 * that can trigger workflows based on external events.
 */
public class OrkesWebhookClient {

    private final WebhookResource webhookResource;

    /**
     * Constructs a new OrkesWebhookClient with the specified ConductorClient.
     *
     * @param client the ConductorClient to use for making HTTP requests to the server
     */
    public OrkesWebhookClient(ConductorClient client) {
        this.webhookResource = new WebhookResource(client);
    }

    /**
     * Create a new webhook configuration.
     *
     * @param webhookConfig the webhook configuration to create
     * @return ConductorClientResponse containing the created webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> createWebhook(WebhookConfig webhookConfig) {
        return webhookResource.createWebhook(webhookConfig);
    }

    /**
     * Update an existing webhook configuration.
     *
     * @param id the webhook ID to update
     * @param webhookConfig the updated webhook configuration
     * @return ConductorClientResponse containing the updated webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> updateWebhook(String id, WebhookConfig webhookConfig) {
        return webhookResource.updateWebhook(id, webhookConfig);
    }

    /**
     * Delete a webhook configuration by ID.
     *
     * @param id the webhook ID to delete
     * @return ConductorClientResponse for the delete operation
     */
    public ConductorClientResponse<Void> deleteWebhook(String id) {
        return webhookResource.deleteWebhook(id);
    }

    /**
     * Get a webhook configuration by ID.
     *
     * @param id the webhook ID to retrieve
     * @return ConductorClientResponse containing the webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> getWebhook(String id) {
        return webhookResource.getWebhook(id);
    }

    /**
     * Get all webhook configurations.
     *
     * @return ConductorClientResponse containing a list of all webhook configurations
     */
    public ConductorClientResponse<List<WebhookConfig>> getAllWebhooks() {
        return webhookResource.getAllWebhooks();
    }

    /**
     * Get tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @return ConductorClientResponse containing a list of tags
     */
    public ConductorClientResponse<List<Tag>> getTagsForWebhook(String id) {
        return webhookResource.getTagsForWebhook(id);
    }

    /**
     * Put tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @param tags the tags to set
     * @return ConductorClientResponse for the put operation
     */
    public ConductorClientResponse<Void> putTagsForWebhook(String id, List<Tag> tags) {
        return webhookResource.putTagsForWebhook(id, tags);
    }

    /**
     * Delete tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @param tags the tags to remove
     * @return ConductorClientResponse for the delete operation
     */
    public ConductorClientResponse<Void> deleteTagsForWebhook(String id, List<Tag> tags) {
        return webhookResource.deleteTagsForWebhook(id, tags);
    }

    /**
     * Convenience method to get a webhook configuration directly without wrapper.
     *
     * @param id the webhook ID to retrieve
     * @return WebhookConfig object or null if failed
     */
    public WebhookConfig getWebhookConfig(String id) {
        try {
            ConductorClientResponse<WebhookConfig> response = getWebhook(id);
            return response != null ? response.getData() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convenience method to get all webhook configurations directly without wrapper.
     *
     * @return List of WebhookConfig objects or empty list if failed
     */
    public List<WebhookConfig> getWebhookConfigs() {
        try {
            ConductorClientResponse<List<WebhookConfig>> response = getAllWebhooks();
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Check if a webhook exists by ID.
     *
     * @param id the webhook ID to check
     * @return true if webhook exists, false otherwise
     */
    public boolean webhookExists(String id) {
        try {
            ConductorClientResponse<WebhookConfig> response = getWebhook(id);
            return response != null && response.getData() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
