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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.integration.Integration;
import io.orkes.conductor.client.model.integration.IntegrationApi;
import io.orkes.conductor.client.model.integration.IntegrationApiUpdate;
import io.orkes.conductor.client.model.integration.IntegrationDef;
import io.orkes.conductor.client.model.integration.IntegrationEventStats;
import io.orkes.conductor.client.model.integration.IntegrationUpdate;
import io.orkes.conductor.client.model.integration.ai.PromptTemplate;

import com.fasterxml.jackson.core.type.TypeReference;


class IntegrationResource {

    private final ConductorClient client;

    IntegrationResource(ConductorClient client) {
        this.client = client;
    }

    void associatePromptWithIntegration(String integrationProvider, String integrationName, String promptName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}/prompt/{promptName}")
                .addPathParam("integrationProvider", integrationProvider)
                .addPathParam("integrationName", integrationName)
                .addPathParam("promptName", promptName)
                .build();

        client.execute(request);
    }

    void deleteIntegrationApi(String integrationProvider, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}")
                .addPathParam("integrationProvider", integrationProvider)
                .addPathParam("integrationName", integrationName)
                .build();

        client.execute(request);
    }

    void deleteIntegrationProvider(String integrationProvider) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationProvider}")
                .addPathParam("integrationProvider", integrationProvider)
                .build();

        client.execute(request);
    }

    void deleteTagForIntegrationProvider(List<TagObject> body, String integrationProvider) {
        Objects.requireNonNull(body, "List<TagObject> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationProvider}/tags")
                .addPathParam("integrationProvider", integrationProvider)
                .body(body)
                .build();

        client.execute(request);
    }

    IntegrationApi getIntegrationApi(String integrationProvider, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}")
                .addPathParam("integrationProvider", integrationProvider)
                .addPathParam("integrationName", integrationName)
                .build();

        ConductorClientResponse<IntegrationApi> resp = client.execute(request, new TypeReference<>() {
        });
        return resp.getData();
    }

    List<IntegrationApi> getIntegrationApis(String name, Boolean activeOnly) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{name}/integration")
                .addPathParam("name", name)
                .addQueryParam("activeOnly", activeOnly)
                .build();

        ConductorClientResponse<List<IntegrationApi>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Integration getIntegrationProvider(String integrationProvider) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}")
                .addPathParam("integrationProvider", integrationProvider)
                .build();

        ConductorClientResponse<Integration> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<Integration> getIntegrationProviders(String category, Boolean activeOnly) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider")
                .addQueryParam("category", category)
                .addQueryParam("activeOnly", activeOnly)
                .build();
        ConductorClientResponse<List<Integration>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<PromptTemplate> getPromptsWithIntegration(String integrationProvider, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}/prompt")
                .addQueryParam("integrationProvider", integrationProvider)
                .addQueryParam("integrationName", integrationName)
                .build();

        ConductorClientResponse<List<PromptTemplate>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<TagObject> getTagsForIntegrationProvider(String integrationProvider) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}/tags")
                .addPathParam("integrationProvider", integrationProvider)
                .build();

        ConductorClientResponse<List<TagObject>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Integer getTokenUsageForIntegration(String integrationProvider, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}/metrics")
                .addPathParam("integrationProvider", integrationProvider)
                .addPathParam("integrationName", integrationName)
                .build();
        ConductorClientResponse<Integer> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Map<String, Integer> getTokenUsageForIntegrationProvider(String integrationProvider) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationProvider}/metrics")
                .addPathParam("integrationProvider", integrationProvider)
                .build();
        ConductorClientResponse<Map<String, Integer>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void putTagForIntegrationProvider(List<TagObject> body, String integrationProvider) {
        Objects.requireNonNull(body, "List<TagObject> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/integrations/provider/{integrationProvider}/tags")
                .addPathParam("integrationProvider", integrationProvider)
                .body(body)
                .build();

        client.execute(request);
    }

    void saveIntegrationApi(IntegrationApiUpdate body, String integrationProvider, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationProvider}/integration/{integrationName}")
                .addPathParam("integrationProvider", integrationProvider)
                .addPathParam("integrationName", integrationName)
                .body(body)
                .build();

        client.execute(request);
    }

    void saveIntegrationProvider(IntegrationUpdate body, String integrationProvider) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationProvider}")
                .addPathParam("integrationProvider", integrationProvider)
                .body(body)
                .build();

        client.execute(request);
    }

    List<Integration> getAllIntegrations(String type, boolean activeOnly) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations")
                .addQueryParam("type", type)
                .addQueryParam("activeOnly", activeOnly)
                .build();
        ConductorClientResponse<List<Integration>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
    void saveAllIntegrations(List<Integration> integrations) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations")
                .body(integrations)
                .build();
        client.execute(request);
    }

    List<String> getIntegrationProviderNames(String type, boolean activeOnly) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/all")
                .addQueryParam("type", type)
                .addQueryParam("activeOnly", activeOnly)
                .build();
        ConductorClientResponse<List<String>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<IntegrationDef> getProvidersDefinitions(String type, boolean activeOnly) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/providers/definitions")
                .addQueryParam("type", type)
                .addQueryParam("activeOnly", activeOnly)
                .build();
        ConductorClientResponse<List<IntegrationDef>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<IntegrationEventStats> getEventStats(String type) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/eventStats/{type}")
                .addPathParam("type", type)
                .build();
        ConductorClientResponse<List<IntegrationEventStats>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void deleteIntegrationTags(String providerName, String integrationName, List<TagObject> tags) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{name}/integration/{integration_name}/tags")
                .addPathParam("name", providerName)
                .addPathParam("integration_name", integrationName)
                .body(tags)
                .build();
        client.execute(request);
    }

    List<TagObject> getIntegrationTags(String providerName, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{name}/integration/{integration_name}/tags")
                .addPathParam("name", providerName)
                .addPathParam("integration_name", integrationName)
                .build();
        ConductorClientResponse<List<TagObject>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void putIntegrationTags(String providerName, String integrationName, List<TagObject> tags) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/integrations/provider/{name}/integration/{integration_name}/tags")
                .addPathParam("name", providerName)
                .addPathParam("integration_name", integrationName)
                .body(tags)
                .build();
        client.execute(request);
    }

    List<String> getAllIntegrations(String providerName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{name}/integration/all")
                .addPathParam("name", providerName)
                .build();
        ConductorClientResponse<List<String>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
}
