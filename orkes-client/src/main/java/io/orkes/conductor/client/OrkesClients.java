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
package io.orkes.conductor.client;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ServiceRegistryClient;

import io.orkes.conductor.client.http.*;

public class OrkesClients {

    private final ConductorClient client;

    public OrkesClients(ConductorClient client) {
        this.client = client;
    }

    public OrkesWorkflowClient getWorkflowClient() {
        return new OrkesWorkflowClient(client);
    }

    public AuthorizationClient getAuthorizationClient() {
        return new OrkesAuthorizationClient(client);
    }

    public OrkesEventClient getEventClient() {
        return new OrkesEventClient(client);
    }

    public OrkesMetadataClient getMetadataClient() {
        return new OrkesMetadataClient(client);
    }

    public OrkesSchedulerClient getSchedulerClient() {
        return new OrkesSchedulerClient(client);
    }

    public ServiceRegistryClient getServiceRegistryClient() {
        return new ServiceRegistryClient(client);
    }

    public OrkesSecretClient getSecretClient() {
        return new OrkesSecretClient(client);
    }

    public OrkesTaskClient getTaskClient() {
        return new OrkesTaskClient(client);
    }

    public OrkesTokenClient getTokenClient() {
        return new OrkesTokenClient(client);
    }

    public IntegrationClient getIntegrationClient() {
        return new OrkesIntegrationClient(client);
    }

    public PromptClient getPromptClient() {
        return new OrkesPromptClient(client);
    }
}
