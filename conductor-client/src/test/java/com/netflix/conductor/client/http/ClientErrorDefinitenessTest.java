/*
 * Copyright 2026 Conductor Authors.
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

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A client error must not claim the request failed unless it did. Retrying an indeterminate start
 * runs the workflow twice.
 *
 * @see <a href="https://github.com/orkes-io/jepsen-conductor/issues/6">jepsen-conductor#6</a>
 */
class ClientErrorDefinitenessTest {

    private MockWebServer server;
    private WorkflowClient workflowClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        workflowClient = new WorkflowClient(new ConductorClient(server.url("/api").toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        Thread.interrupted();
    }

    @Test
    @DisplayName("The server rejected the request: definite, safe to retry")
    void badRequest_isDefinite() {
        server.enqueue(new MockResponse().setResponseCode(400).setBody("bad request"));

        var e = assertThrows(ConductorClientException.class, () -> workflowClient.startWorkflow(startRequest()));

        assertEquals(400, e.getStatus());
        assertTrue(e.isDefinite(), "a 400 means the workflow was never created");
    }

    @Test
    @DisplayName("The server failed after reading the request: indeterminate")
    void serverError_isNotDefinite() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        var e = assertThrows(ConductorClientException.class, () -> workflowClient.startWorkflow(startRequest()));

        assertEquals(500, e.getStatus());
        assertFalse(e.isDefinite(), "a 500 may have created the workflow before failing");
    }

    @Test
    @DisplayName("The connection dropped after the request was read: indeterminate, this is the Jepsen case")
    void connectionDroppedAfterRequest_isNotDefinite() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

        var e = assertThrows(ConductorClientException.class, () -> workflowClient.startWorkflow(startRequest()));

        assertFalse(e.isDefinite(), "the server may have created the workflow before the connection dropped");
    }

    @Test
    @DisplayName("The connection dropped before the request was read: still indeterminate, OkHttp may have already sent it once")
    void connectionDroppedAtStart_isNotDefinite() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        var e = assertThrows(ConductorClientException.class, () -> workflowClient.startWorkflow(startRequest()));

        assertFalse(e.isDefinite(), "a dropped connection never proves the request was not delivered");
    }

    @Test
    @DisplayName("A 2xx with no workflow id: the start was applied, the outcome is unknown")
    void emptySuccessBody_isNotDefinite() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        var e = assertThrows(ConductorClientException.class, () -> workflowClient.startWorkflow(startRequest()));

        assertFalse(e.isDefinite(), "the server accepted the request; only the id was lost");
    }

    private StartWorkflowRequest startRequest() {
        var request = new StartWorkflowRequest();
        request.setName("definiteness_test");
        request.setVersion(1);
        return request;
    }
}
