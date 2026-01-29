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
import io.orkes.conductor.client.model.integration.IntegrationUpdate;
import io.orkes.conductor.client.model.integration.ai.PromptTemplate;

import com.fasterxml.jackson.core.type.TypeReference;


class IntegrationResource {

    private final ConductorClient client;

    IntegrationResource(ConductorClient client) {
        this.client = client;
    }

    void associatePromptWithIntegration(String integrationName, String modelName, String promptName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationName}/integration/{modelName}/prompt/{promptName}")
                .addPathParam("integrationName", integrationName)
                .addPathParam("modelName", modelName)
                .addPathParam("promptName", promptName)
                .build();

        client.execute(request);
    }

    void deleteIntegrationApi(String integrationName, String api) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationName}/integration/{api}")
                .addPathParam("integrationName", integrationName)
                .addPathParam("api", api)
                .build();

        client.execute(request);
    }

    void deleteIntegrationProvider(String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationName}")
                .addPathParam("integrationName", integrationName)
                .build();

        client.execute(request);
    }

    void deleteTagForIntegrationProvider(List<TagObject> body, String integrationName) {
        Objects.requireNonNull(body, "List<TagObject> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/integrations/provider/{integrationName}/tags")
                .addPathParam("integrationName", integrationName)
                .body(body)
                .build();

        client.execute(request);
    }

    IntegrationApi getIntegrationApi(String integrationName, String api) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}/integration/{api}")
                .addPathParam("integrationName", integrationName)
                .addPathParam("api", api)
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

    Integration getIntegrationProvider(String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}")
                .addPathParam("integrationName", integrationName)
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

    List<PromptTemplate> getPromptsWithIntegration(String integrationName, String model) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}/integration/{model}/prompt")
                .addQueryParam("integrationName", integrationName)
                .addQueryParam("model", model)
                .build();

        ConductorClientResponse<List<PromptTemplate>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<TagObject> getTagsForIntegrationProvider(String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}/tags")
                .addPathParam("integrationName", integrationName)
                .build();

        ConductorClientResponse<List<TagObject>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Integer getTokenUsageForIntegration(String integrationName, String model) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}/integration/{model}/metrics")
                .addPathParam("integrationName", integrationName)
                .addPathParam("model", model)
                .build();
        ConductorClientResponse<Integer> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Map<String, Integer> getTokenUsageForIntegrationProvider(String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/integrations/provider/{integrationName}/metrics")
                .addPathParam("integrationName", integrationName)
                .build();
        ConductorClientResponse<Map<String, Integer>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void putTagForIntegrationProvider(List<TagObject> body, String integrationName) {
        Objects.requireNonNull(body, "List<TagObject> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/integrations/provider/{integrationName}/tags")
                .addPathParam("integrationName", integrationName)
                .body(body)
                .build();

        client.execute(request);
    }

    void saveIntegrationApi(IntegrationApiUpdate body, String integrationName, String api) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationName}/integration/{api}")
                .addPathParam("integrationName", integrationName)
                .addPathParam("api", api)
                .body(body)
                .build();

        client.execute(request);
    }

    void saveIntegrationProvider(IntegrationUpdate body, String integrationName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/integrations/provider/{integrationName}")
                .addPathParam("integrationName", integrationName)
                .body(body)
                .build();

        client.execute(request);
    }
}
