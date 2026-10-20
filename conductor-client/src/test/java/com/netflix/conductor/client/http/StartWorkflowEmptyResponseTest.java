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
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A successful response carrying no workflow id used to come back as a null return value, so a
 * caller saw a "successful" start with no id and no error. It must raise instead.
 */
class StartWorkflowEmptyResponseTest {

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

    private StartWorkflowRequest request() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("greetings");
        request.setVersion(1);
        return request;
    }

    @Test
    void emptyTwoHundredBodyRaisesInsteadOfReturningNull() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        ConductorClientException e =
                assertThrows(
                        ConductorClientException.class,
                        () -> workflowClient.startWorkflow(request()));

        assertEquals(200, e.getStatus());
        assertTrue(
                e.getMessage().contains("No workflow id was returned"),
                "message should say what went wrong, was: " + e.getMessage());
    }

    @Test
    void aBodyIsStillReturned() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("3919784b-7691-11f1-a292-00163e983f22"));

        assertEquals(
                "3919784b-7691-11f1-a292-00163e983f22", workflowClient.startWorkflow(request()));
    }
}
