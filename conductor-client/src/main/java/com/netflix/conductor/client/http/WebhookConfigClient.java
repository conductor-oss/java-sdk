/*
 * Copyright 2020 Conductor Authors.
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
package com.netflix.conductor.client.http;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.common.model.Tag;
import com.netflix.conductor.common.model.WebhookConfig;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for webhook configuration operations in Conductor
 */
public final class WebhookConfigClient {

    private ConductorClient client;

    /**
     * Creates a default webhook config client
     */
    public WebhookConfigClient() {
    }

    public WebhookConfigClient(ConductorClient client) {
        this.client = client;
    }

    /**
     * Kept only for backwards compatibility
     *
     * @param rootUri basePath for the ApiClient
     */
    @Deprecated
    public void setRootURI(String rootUri) {
        if (client != null) {
            client.shutdown();
        }
        client = new ConductorClient(rootUri);
    }

    /**
     * Create a new webhook configuration
     *
     * @param webhookConfig The webhook configuration to create
     * @return The created webhook configuration with assigned ID
     */
    public WebhookConfig createWebhook(WebhookConfig webhookConfig) {
        Validate.notNull(webhookConfig, "WebhookConfig cannot be null");
        Validate.notBlank(webhookConfig.getName(), "Webhook name cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/metadata/webhook")
                .body(webhookConfig)
                .build();

        ConductorClientResponse<WebhookConfig> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Update an existing webhook configuration
     *
     * @param id            The webhook ID to update
     * @param webhookConfig The updated webhook configuration
     * @return The updated webhook configuration
     */
    public WebhookConfig updateWebhook(String id, WebhookConfig webhookConfig) {
        Validate.notBlank(id, "Webhook ID cannot be blank");
        Validate.notNull(webhookConfig, "WebhookConfig cannot be null");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/metadata/webhook/{id}")
                .addPathParam("id", id)
                .body(webhookConfig)
                .build();

        ConductorClientResponse<WebhookConfig> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Delete a webhook configuration
     *
     * @param id The webhook ID to delete
     */
    public void deleteWebhook(String id) {
        Validate.notBlank(id, "Webhook ID cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/metadata/webhook/{id}")
                .addPathParam("id", id)
                .build();

        client.execute(request);
    }

    /**
     * Get a webhook configuration by ID
     *
     * @param id The webhook ID
     * @return The webhook configuration
     */
    public WebhookConfig getWebhook(String id) {
        Validate.notBlank(id, "Webhook ID cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook/{id}")
                .addPathParam("id", id)
                .build();

        ConductorClientResponse<WebhookConfig> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Get all webhook configurations
     *
     * @return List of all webhook configurations
     */
    public List<WebhookConfig> getAllWebhooks() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook")
                .build();

        ConductorClientResponse<List<WebhookConfig>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Get tags for a webhook
     *
     * @param id The webhook ID
     * @return List of tags associated with the webhook
     */
    public List<Tag> getTagsForWebhook(String id) {
        Validate.notBlank(id, "Webhook ID cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook/{id}/tags")
                .addPathParam("id", id)
                .build();

        ConductorClientResponse<List<Tag>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Set tags for a webhook (replaces existing tags)
     *
     * @param id   The webhook ID
     * @param tags The tags to set
     */
    public void putTagsForWebhook(String id, List<Tag> tags) {
        Validate.notBlank(id, "Webhook ID cannot be blank");
        Validate.notNull(tags, "Tags list cannot be null");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/metadata/webhook/{id}/tags")
                .addPathParam("id", id)
                .body(tags)
                .build();

        client.execute(request);
    }

    /**
     * Delete specific tags from a webhook
     *
     * @param id   The webhook ID
     * @param tags The tags to remove
     */
    public void deleteTagsForWebhook(String id, List<Tag> tags) {
        Validate.notBlank(id, "Webhook ID cannot be blank");
        Validate.notNull(tags, "Tags list cannot be null");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/metadata/webhook/{id}/tags")
                .addPathParam("id", id)
                .body(tags)
                .build();

        client.execute(request);
    }

    /**
     * Add a single tag to a webhook
     *
     * @param id  The webhook ID
     * @param tag The tag to add
     */
    public void addTagToWebhook(String id, Tag tag) {
        Validate.notNull(tag, "Tag cannot be null");
        putTagsForWebhook(id, List.of(tag));
    }

    /**
     * Remove a single tag from a webhook
     *
     * @param id  The webhook ID
     * @param tag The tag to remove
     */
    public void removeTagFromWebhook(String id, Tag tag) {
        Validate.notNull(tag, "Tag cannot be null");
        deleteTagsForWebhook(id, List.of(tag));
    }

    /**
     * Check if a webhook exists
     *
     * @param id The webhook ID
     * @return true if the webhook exists, false otherwise
     */
    public boolean webhookExists(String id) {
        try {
            getWebhook(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}