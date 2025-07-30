package io.orkes.conductor.client.http;

import com.netflix.conductor.client.http.ConductorClient;

public class WebhooksConfigResource {
    private final ConductorClient client;

    WebhooksConfigResource(ConductorClient client) {
        this.client = client;
    }


}
