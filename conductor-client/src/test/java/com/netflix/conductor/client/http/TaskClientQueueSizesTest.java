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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskClientQueueSizesTest {

    private MockWebServer server;
    private TaskClient taskClient;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        taskClient = new TaskClient(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sizesEveryQueueInOneRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"render_pdf\": 48, \"tenant1:send_email\": 6}"));

        Map<String, Integer> sizes = taskClient.getQueueSizesForTasks(List.of("render_pdf", "tenant1:send_email"));

        assertEquals(Map.of("render_pdf", 48, "tenant1:send_email", 6), sizes);
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/tasks/queue/sizes", request.getRequestUrl().encodedPath());
        assertEquals(List.of("render_pdf", "tenant1:send_email"), request.getRequestUrl().queryParameterValues("taskType"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void singleQueueSizeUsesTheSameRequest() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"render_pdf\": 7}"));

        assertEquals(7, taskClient.getQueueSizeForTask("render_pdf"));
    }
}
