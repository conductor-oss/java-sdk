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

import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.client.EnvironmentClient;
import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.environment.EnvironmentVariable;

public class OrkesEnvironmentClient implements EnvironmentClient {

    private final EnvironmentResource environmentResource;

    public OrkesEnvironmentClient(ConductorClient apiClient) {
        this.environmentResource = new EnvironmentResource(apiClient);
    }

    @Override
    public List<EnvironmentVariable> getAllEnvironmentVariables() {
        return environmentResource.getAllEnvironmentVariables();
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return environmentResource.getEnvironmentVariable(key);
    }

    @Override
    public void createOrUpdateEnvironmentVariable(String key, String value) {
        environmentResource.createOrUpdateEnvironmentVariable(key, value);
    }

    @Override
    public String deleteEnvironmentVariable(String key) {
        return environmentResource.deleteEnvironmentVariable(key);
    }

    @Override
    public List<Tag> getEnvironmentVariableTags(String name) {
        return environmentResource.getEnvironmentVariableTags(name);
    }

    @Override
    public void setEnvironmentVariableTags(String name, List<Tag> tags) {
        environmentResource.setEnvironmentVariableTags(name, tags);
    }

    @Override
    public void deleteEnvironmentVariableTags(String name, List<Tag> tags) {
        environmentResource.deleteEnvironmentVariableTags(name, tags);
    }
}
