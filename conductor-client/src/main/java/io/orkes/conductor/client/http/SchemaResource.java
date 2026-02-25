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
import com.netflix.conductor.common.metadata.SchemaDef;

import com.fasterxml.jackson.core.type.TypeReference;

class SchemaResource {

    private final ConductorClient client;

    SchemaResource(ConductorClient client) {
        this.client = client;
    }

    void saveSchemas(List<SchemaDef> schemaDefs) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/schema")
                .body(schemaDefs)
                .build();
        client.execute(request);
    }

    List<SchemaDef> getAllSchemas(Boolean shortFormat) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/schema")
                .addQueryParam("short", shortFormat)
                .build();
        ConductorClientResponse<List<SchemaDef>> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    SchemaDef getSchema(String name) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/schema/{name}")
                .addPathParam("name", name)
                .build();
        ConductorClientResponse<SchemaDef> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    SchemaDef getSchema(String name, int version) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/schema/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .build();
        ConductorClientResponse<SchemaDef> resp = client.execute(request, new TypeReference<>() {});
        return resp.getData();
    }

    void deleteSchema(String name) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/schema/{name}")
                .addPathParam("name", name)
                .build();
        client.execute(request);
    }

    void deleteSchema(String name, int version) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/schema/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .build();
        client.execute(request);
    }
}
