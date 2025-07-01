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
import com.netflix.conductor.common.metadata.workflow.SharedResourceModel;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client for resource sharing operations in Conductor
 */
public final class ResourceSharingClient {

    private ConductorClient client;

    /** Creates a default resource sharing client */
    public ResourceSharingClient() {
    }

    public ResourceSharingClient(ConductorClient client) {
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
     * Share a resource with another user or group
     *
     * @param resourceType Type of resource to share (e.g., "WORKFLOW", "TASK")
     * @param resourceName Name of the resource to share
     * @param sharedWith User or group to share the resource with
     */
    public void shareResource(String resourceType, String resourceName, String sharedWith) {
        Validate.notBlank(resourceType, "ResourceType cannot be blank");
        Validate.notBlank(resourceName, "ResourceName cannot be blank");
        Validate.notBlank(sharedWith, "SharedWith cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/share/shareResource")
                .addQueryParam("resourceType", resourceType)
                .addQueryParam("resourceName", resourceName)
                .addQueryParam("sharedWith", sharedWith)
                .build();

        client.execute(request);
    }

    /**
     * Remove sharing of a resource with a user or group
     *
     * @param resourceType Type of resource to unshare
     * @param resourceName Name of the resource to unshare
     * @param sharedWith User or group to remove sharing from
     */
    public void removeSharingResource(String resourceType, String resourceName, String sharedWith) {
        Validate.notBlank(resourceType, "ResourceType cannot be blank");
        Validate.notBlank(resourceName, "ResourceName cannot be blank");
        Validate.notBlank(sharedWith, "SharedWith cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/share/removeSharingResource")
                .addQueryParam("resourceType", resourceType)
                .addQueryParam("resourceName", resourceName)
                .addQueryParam("sharedWith", sharedWith)
                .build();

        client.execute(request);
    }

    /**
     * Get all shared resources accessible to the current user
     *
     * @return List of shared resources
     */
    public List<SharedResourceModel> getSharedResources() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/share/getSharedResources")
                .build();

        ConductorClientResponse<List<SharedResourceModel>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Get shared resources filtered by resource type
     *
     * @param resourceType Type of resource to filter by
     * @return List of shared resources of the specified type
     */
    public List<SharedResourceModel> getSharedResources(String resourceType) {
        Validate.notBlank(resourceType, "ResourceType cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/share/getSharedResources")
                .addQueryParam("resourceType", resourceType)
                .build();

        ConductorClientResponse<List<SharedResourceModel>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Check if a specific resource is shared with a user or group
     *
     * @param resourceType Type of resource
     * @param resourceName Name of the resource
     * @param sharedWith User or group to check
     * @return true if the resource is shared, false otherwise
     */
    public boolean isResourceShared(String resourceType, String resourceName, String sharedWith) {
        List<SharedResourceModel> sharedResources = getSharedResources(resourceType);
        return sharedResources.stream()
                .anyMatch(resource -> 
                    resource.getResourceName().equals(resourceName) && 
                    resource.getSharedWith().equals(sharedWith));
    }
}
