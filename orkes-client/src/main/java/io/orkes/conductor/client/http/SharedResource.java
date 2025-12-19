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
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.SharedResourceModel;

import com.fasterxml.jackson.core.type.TypeReference;

public class SharedResource {
    private final ConductorClient client;

    public SharedResource(ConductorClient client) {
        this.client = client;
    }

    public void shareResource(String resourceType, String resourceName, String sharedWith) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.POST)
                .path("/share/shareResource")
                .addQueryParam("resourceType", resourceType)
                .addQueryParam("resourceName", resourceName)
                .addQueryParam("sharedWith", sharedWith)
                .build();

        client.execute(request);
    }

    public void removeSharingResource(String resourceType, String resourceName, String sharedWith) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.DELETE)
                .path("/share/removeSharingResource")
                .addQueryParam("resourceType", resourceType)
                .addQueryParam("resourceName", resourceName)
                .addQueryParam("sharedWith", sharedWith)
                .build();

        client.execute(request);
    }

    public List<SharedResourceModel> getSharedResources() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.GET)
                .path("/share/getSharedResources")
                .build();

        ConductorClientResponse<List<SharedResourceModel>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
}
