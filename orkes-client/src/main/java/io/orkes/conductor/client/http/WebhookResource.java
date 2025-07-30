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
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.WebhookConfig;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Resource class for webhook configuration operations.
 * Provides HTTP client methods to interact with webhook endpoints.
 */
public class WebhookResource {

    private final ConductorClient client;

    public WebhookResource(ConductorClient client) {
        this.client = client;
    }

    /**
     * Create a new webhook configuration.
     *
     * @param webhookConfig the webhook configuration to create
     * @return ConductorClientResponse containing the created webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> createWebhook(WebhookConfig webhookConfig) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/metadata/webhook")
                .body(webhookConfig)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Update an existing webhook configuration.
     *
     * @param id the webhook ID to update
     * @param webhookConfig the updated webhook configuration
     * @return ConductorClientResponse containing the updated webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> updateWebhook(String id, WebhookConfig webhookConfig) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/metadata/webhook/" + id)
                .body(webhookConfig)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Delete a webhook configuration by ID.
     *
     * @param id the webhook ID to delete
     * @return ConductorClientResponse for the delete operation
     */
    public ConductorClientResponse<Void> deleteWebhook(String id) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/metadata/webhook/" + id)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Get a webhook configuration by ID.
     *
     * @param id the webhook ID to retrieve
     * @return ConductorClientResponse containing the webhook configuration
     */
    public ConductorClientResponse<WebhookConfig> getWebhook(String id) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook/" + id)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Get all webhook configurations.
     *
     * @return ConductorClientResponse containing a list of all webhook configurations
     */
    public ConductorClientResponse<List<WebhookConfig>> getAllWebhooks() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook")
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Get tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @return ConductorClientResponse containing a list of tags
     */
    public ConductorClientResponse<List<Tag>> getTagsForWebhook(String id) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/metadata/webhook/" + id + "/tags")
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Put tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @param tags the tags to set
     * @return ConductorClientResponse for the put operation
     */
    public ConductorClientResponse<Void> putTagsForWebhook(String id, List<Tag> tags) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/metadata/webhook/" + id + "/tags")
                .body(tags)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }

    /**
     * Delete tags for a webhook by ID.
     *
     * @param id the webhook ID
     * @param tags the tags to remove
     * @return ConductorClientResponse for the delete operation
     */
    public ConductorClientResponse<Void> deleteTagsForWebhook(String id, List<Tag> tags) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/metadata/webhook/" + id + "/tags")
                .body(tags)
                .build();

        return client.execute(request, new TypeReference<>() {
        });
    }
}