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


import org.apache.commons.lang3.Validate;

import com.netflix.conductor.client.http.ConductorClientRequest.Method;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for incoming webhook operations in Conductor
 */
public final class IncomingWebhookClient {

    private ConductorClient client;

    /** Creates a default incoming webhook client */
    public IncomingWebhookClient() {
    }

    public IncomingWebhookClient(ConductorClient client) {
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
     * Handle incoming webhook with POST method
     *
     * @param id           The webhook ID
     * @param payload      The webhook payload as string
     * @return The response from the webhook handler
     */
    public String handleWebhook(String id, String payload) {
        Validate.notBlank(id, "Webhook ID cannot be blank");
        Validate.notNull(payload, "Payload cannot be null");

        ConductorClientRequest.Builder requestBuilder = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/webhook/{id}")
                .addPathParam("id", id)
                .body(payload);

        ConductorClientRequest request = requestBuilder.build();

        ConductorClientResponse<String> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Handle incoming webhook with GET method (typically for ping/health checks)
     *
     * @param id           The webhook ID
     * @return The response from the webhook handler
     */
    public String handlePing(String id) {
        Validate.notBlank(id, "Webhook ID cannot be blank");

        ConductorClientRequest.Builder requestBuilder = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/webhook/{id}")
                .addPathParam("id", id);

        ConductorClientRequest request = requestBuilder.build();

        ConductorClientResponse<String> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
}
