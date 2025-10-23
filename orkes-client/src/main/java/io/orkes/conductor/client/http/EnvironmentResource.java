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
package io.orkes.conductor.client.http;

import java.util.List;
import java.util.Objects;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.environment.EnvironmentVariable;

import com.fasterxml.jackson.core.type.TypeReference;

class EnvironmentResource {

    private final ConductorClient client;

    EnvironmentResource(ConductorClient client) {
        this.client = client;
    }

    List<EnvironmentVariable> getAllEnvironmentVariables() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/environment")
                .build();
        ConductorClientResponse<List<EnvironmentVariable>> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    String getEnvironmentVariable(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/environment/{key}")
                .addPathParam("key", key)
                .build();
        ConductorClientResponse<String> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    void createOrUpdateEnvironmentVariable(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/environment/{key}")
                .addPathParam("key", key)
                .addHeaderParam("Content-Type", "text/plain")
                .body(value)
                .build();
        client.execute(request);
    }

    String deleteEnvironmentVariable(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/environment/{key}")
                .addPathParam("key", key)
                .build();
        ConductorClientResponse<String> resp = client.execute(request, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        return resp.getData();
    }

    List<Tag> getEnvironmentVariableTags(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/environment/{name}/tags")
                .addPathParam("name", name)
                .build();
        ConductorClientResponse<List<Tag>> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    void setEnvironmentVariableTags(String name, List<Tag> tags) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(tags, "List<Tag> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/environment/{name}/tags")
                .addPathParam("name", name)
                .body(tags)
                .build();
        client.execute(request);
    }

    void deleteEnvironmentVariableTags(String name, List<Tag> tags) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(tags, "List<Tag> cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/environment/{name}/tags")
                .addPathParam("name", name)
                .body(tags)
                .build();
        client.execute(request);
    }
}
