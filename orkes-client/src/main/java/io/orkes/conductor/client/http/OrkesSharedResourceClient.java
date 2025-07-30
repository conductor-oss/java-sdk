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

import io.orkes.conductor.client.model.SharedResourceModel;

/**
 * Client for managing shared resources in Orkes Conductor.
 * Provides functionality to share various types of resources (workflows, tasks, secrets, etc.)
 * with other users, groups, or applications within the Conductor ecosystem.
 */
public class OrkesSharedResourceClient {

    private final SharedResource sharedResource;

    /**
     * Constructs a new OrkesSharedResourceClient with the specified ConductorClient.
     *
     * @param client the ConductorClient to use for making HTTP requests to the server
     */
    public OrkesSharedResourceClient(ConductorClient client) {
        this.sharedResource = new SharedResource(client);
    }

    /**
     * Shares a resource with another user, group, or application.
     *
     * @param resourceType the type of resource to share (e.g., "WORKFLOW", "TASK", "SECRET")
     * @param resourceName the name/identifier of the specific resource to share
     * @param sharedWith   the identifier of the entity to share with (user email, group name, or application ID)
     */
    public void shareResource(String resourceType, String resourceName, String sharedWith) {
        sharedResource.shareResource(resourceType, resourceName, sharedWith);
    }

    /**
     * Removes sharing access for a resource from a specific entity.
     *
     * @param resourceType the type of resource to stop sharing (e.g., "WORKFLOW", "TASK", "SECRET")
     * @param resourceName the name/identifier of the specific resource to stop sharing
     * @param sharedWith   the identifier of the entity to remove sharing from (user email, group name, or application ID)
     */
    public void removeSharingResource(String resourceType, String resourceName, String sharedWith) {
        sharedResource.removeSharingResource(resourceType, resourceName, sharedWith);
    }

    /**
     * Retrieves a list of all resources that are currently shared by or with the current user/application.
     *
     * @return a list of SharedResourceModel objects representing all shared resources
     */
    public List<SharedResourceModel> getSharedResources() {
        return sharedResource.getSharedResources();
    }
}