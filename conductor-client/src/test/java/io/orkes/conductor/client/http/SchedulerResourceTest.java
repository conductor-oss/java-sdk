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
package io.orkes.conductor.client.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.ApiClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchedulerResourceTest {

    private MockWebServer server;
    private OrkesSchedulerClient schedulerClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        schedulerClient = new OrkesSchedulerClient(ApiClient.builder()
                .basePath(server.url("/api").toString())
                .build());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "expected a request to reach the stub server");
        return request;
    }

    @Test
    void pauseSchedulePrefersGetAndSendsReason() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(204));

        schedulerClient.pauseSchedule("nightly", "maintenance window");

        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/scheduler/schedules/nightly/pause?reason=maintenance%20window", request.getPath());
    }

    @Test
    void pauseScheduleRetriesWithPutOnlyAfterMethodNotAllowed() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(405));
        server.enqueue(new MockResponse().setResponseCode(204));

        schedulerClient.pauseSchedule("nightly");

        RecordedRequest get = takeRequest();
        RecordedRequest put = takeRequest();
        assertEquals("GET", get.getMethod());
        assertEquals("PUT", put.getMethod());
        assertEquals("/api/scheduler/schedules/nightly/pause", put.getPath());
        assertNull(put.getRequestUrl().queryParameter("reason"));
    }

    @Test
    void resumeScheduleUsesTheSameGetThenPutFallback() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(405));
        server.enqueue(new MockResponse().setResponseCode(204));

        schedulerClient.resumeSchedule("nightly");

        assertEquals("GET", takeRequest().getMethod());
        assertEquals("PUT", takeRequest().getMethod());
    }

    @Test
    void pauseScheduleDoesNotRetryNon405Failures() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(RuntimeException.class, () -> schedulerClient.pauseSchedule("nightly"));

        assertEquals("GET", takeRequest().getMethod());
        assertNull(server.takeRequest(100, TimeUnit.MILLISECONDS));
    }
}
