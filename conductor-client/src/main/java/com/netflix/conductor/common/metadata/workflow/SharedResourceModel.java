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
package com.netflix.conductor.common.metadata.workflow;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing a shared resource between users/groups
 */
public class SharedResourceModel {

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("resourceName")
    private String resourceName;

    @JsonProperty("sharedWith")
    private String sharedWith;

    @JsonProperty("owner")
    private String owner;

    @JsonProperty("sharedAt")
    private Long sharedAt;

    public SharedResourceModel() {
    }

    public SharedResourceModel(String resourceType, String resourceName, String sharedWith) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.sharedWith = sharedWith;
    }

    /**
     * @return The type of resource being shared (e.g., "WORKFLOW", "TASK")
     */
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return The name of the resource being shared
     */
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * @return The user/group the resource is shared with
     */
    public String getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(String sharedWith) {
        this.sharedWith = sharedWith;
    }

    /**
     * @return The owner of the resource
     */
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return Timestamp when the resource was shared
     */
    public Long getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Long sharedAt) {
        this.sharedAt = sharedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedResourceModel that = (SharedResourceModel) o;
        return Objects.equals(resourceType, that.resourceType) &&
                Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(sharedWith, that.sharedWith) &&
                Objects.equals(owner, that.owner) &&
                Objects.equals(sharedAt, that.sharedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceName, sharedWith, owner, sharedAt);
    }

    @Override
    public String toString() {
        return "SharedResourceModel{" +
                "resourceType='" + resourceType + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", sharedWith='" + sharedWith + '\'' +
                ", owner='" + owner + '\'' +
                ", sharedAt=" + sharedAt +
                '}';
    }
}
