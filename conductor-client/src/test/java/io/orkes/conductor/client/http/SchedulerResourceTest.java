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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.model.CronSchedule;
import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.SearchResultWorkflowScheduleExecution;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.WorkflowSchedule;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two behaviour changes to {@link SchedulerResource} that make it portable across
 * OSS Conductor (PUT pause/resume, 200-empty get-miss) and Orkes Conductor (GET pause/resume,
 * 404 get-miss): the cached PUT-then-405-fallback state machine, and the normalized get-miss
 * contract.
 */
class SchedulerResourceTest {

    private MockWebServer server;
    private SchedulerResource resource;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        resource = new SchedulerResource(new ConductorClient(server.url("/api").toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request, "expected a request to reach the stub server");
        return request;
    }

    // ── pause/resume verb fallback ──────────────────────────────────────

    @Test
    void pauseSendsPutFirst() throws InterruptedException {
        server.enqueue(json("{}"));

        resource.pauseSchedule("s1");

        RecordedRequest request = takeRequest();
        assertEquals("PUT", request.getMethod());
        assertTrue(request.getPath().endsWith("/scheduler/schedules/s1/pause"));
    }

    @Test
    void pauseFallsBackToGetOn405AndCachesVerdictAcrossPauseAndResume() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(405));
        server.enqueue(json("{}"));

        resource.pauseSchedule("s1");

        RecordedRequest first = takeRequest();
        assertEquals("PUT", first.getMethod());
        RecordedRequest second = takeRequest();
        assertEquals("GET", second.getMethod());
        assertTrue(second.getPath().endsWith("/scheduler/schedules/s1/pause"));

        // Verdict is cached: resume (a different method) goes straight to GET, no PUT probe.
        server.enqueue(json("{}"));
        resource.resumeSchedule("s1");

        RecordedRequest third = takeRequest();
        assertEquals("GET", third.getMethod());
        assertTrue(third.getPath().endsWith("/scheduler/schedules/s1/resume"));
    }

    @Test
    void pauseRethrowsOn403WithoutFallingBackToGet() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(403));

        ConductorClientException ex = assertThrows(ConductorClientException.class, () -> resource.pauseSchedule("s1"));
        assertEquals(403, ex.getStatus());

        assertEquals(1, server.getRequestCount());
    }

    @Test
    void pauseRethrowsOn404WithoutFallingBackToGet() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(404));

        ConductorClientException ex = assertThrows(ConductorClientException.class, () -> resource.pauseSchedule("s1"));
        assertEquals(404, ex.getStatus());

        assertEquals(1, server.getRequestCount());
    }

    @Test
    void resumeSharesTheSameCachedFallbackVerdictAsPause() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(405));
        server.enqueue(json("{}"));

        resource.resumeSchedule("s1");
        takeRequest(); // PUT (405)
        RecordedRequest fallback = takeRequest();
        assertEquals("GET", fallback.getMethod());

        server.enqueue(json("{}"));
        resource.pauseSchedule("s2");

        RecordedRequest thirdCall = takeRequest();
        assertEquals("GET", thirdCall.getMethod());
    }

    // ── reason query param ───────────────────────────────────────────────

    @Test
    void pauseSendsReasonQueryParamWhenProvided() throws InterruptedException {
        server.enqueue(json("{}"));

        resource.pauseSchedule("s1", "maintenance window");

        RecordedRequest request = takeRequest();
        assertTrue(request.getPath().contains("reason=maintenance"));
    }

    @Test
    void pauseOmitsReasonQueryParamWhenNull() throws InterruptedException {
        server.enqueue(json("{}"));

        resource.pauseSchedule("s1");

        RecordedRequest request = takeRequest();
        assertFalse(request.getPath().contains("reason"));
    }

    @Test
    void reasonSurvivesTheGetFallback() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(405));
        server.enqueue(json("{}"));

        resource.pauseSchedule("s1", "maintenance window");

        takeRequest(); // PUT (405)
        RecordedRequest fallback = takeRequest();
        assertEquals("GET", fallback.getMethod());
        assertTrue(fallback.getPath().contains("reason=maintenance"));
    }

    // ── get-miss contract ────────────────────────────────────────────────

    @Test
    void getScheduleThrowsOn404() {
        server.enqueue(new MockResponse().setResponseCode(404));

        ConductorClientException ex = assertThrows(ConductorClientException.class, () -> resource.getSchedule("missing"));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void getScheduleThrowsOnOssStyleEmptyBody() {
        server.enqueue(json(""));

        assertThrows(ConductorClientException.class, () -> resource.getSchedule("missing"));
    }

    @Test
    void getScheduleThrowsOnEmptyJsonObject() {
        server.enqueue(json("{}"));

        assertThrows(ConductorClientException.class, () -> resource.getSchedule("missing"));
    }

    @Test
    void getScheduleReturnsScheduleOnHitIncludingNextRunTimeAndCronSchedules() {
        server.enqueue(json("{"
                + "\"name\":\"s1\","
                + "\"cronExpression\":\"0 */5 * * * *\","
                + "\"nextRunTime\":1700000000000,"
                + "\"cronSchedules\":[{\"cronExpression\":\"0 0 * * * *\",\"zoneId\":\"UTC\"}]"
                + "}"));

        WorkflowSchedule schedule = resource.getSchedule("s1");

        assertEquals("s1", schedule.getName());
        assertEquals(1700000000000L, schedule.getNextRunTime());
        assertEquals(1, schedule.getCronSchedules().size());
        assertEquals("0 0 * * * *", schedule.getCronSchedules().get(0).getCronExpression());
    }

    @Test
    void getScheduleToleratesMissingNextRunTime() {
        server.enqueue(json("{\"name\":\"s1\",\"cronExpression\":\"0 */5 * * * *\"}"));

        WorkflowSchedule schedule = resource.getSchedule("s1");

        assertEquals("s1", schedule.getName());
        assertEquals(null, schedule.getNextRunTime());
    }

    // ── save body wire shape ─────────────────────────────────────────────

    @Test
    void saveScheduleOmitsCronSchedulesWhenNotSet() throws InterruptedException {
        server.enqueue(json("{}"));

        SaveScheduleRequest request = SaveScheduleRequest.builder()
                .name("s1")
                .cronExpression("0 */5 * * * *")
                .build();
        resource.saveSchedule(request);

        RecordedRequest recorded = takeRequest();
        assertFalse(recorded.getBody().readUtf8().contains("cronSchedules"));
    }

    @Test
    void saveScheduleSendsCronSchedulesWhenSet() throws InterruptedException {
        server.enqueue(json("{}"));

        SaveScheduleRequest request = SaveScheduleRequest.builder()
                .name("s1")
                .cronSchedules(List.of(CronSchedule.builder().cronExpression("0 0 * * * *").zoneId("UTC").build()))
                .build();
        resource.saveSchedule(request);

        RecordedRequest recorded = takeRequest();
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("\"cronSchedules\""));
        assertTrue(body.contains("0 0 * * * *"));
    }

    // ── default-method backward compatibility ───────────────────────────

    /** A minimal {@link SchedulerClient} implementation predating the reason overload — proves
     * the interface addition doesn't break existing implementers. */
    private static class LegacySchedulerClient implements SchedulerClient {
        String lastPausedName;

        @Override
        public void pauseSchedule(String name) {
            lastPausedName = name;
        }

        @Override
        public void saveSchedule(SaveScheduleRequest saveScheduleRequest) {}

        @Override
        public WorkflowSchedule getSchedule(String name) {
            return null;
        }

        @Override
        public List<WorkflowSchedule> getAllSchedules(String workflowName) {
            return null;
        }

        @Override
        public void deleteSchedule(String name) {}

        @Override
        public void resumeSchedule(String name) {}

        @Override
        public void pauseAllSchedules() {}

        @Override
        public void resumeAllSchedules() {}

        @Override
        public com.netflix.conductor.common.model.BulkResponse pauseSchedulers(List<String> schedulerIds) {
            return null;
        }

        @Override
        public com.netflix.conductor.common.model.BulkResponse resumeSchedulers(List<String> schedulerIds) {
            return null;
        }

        @Override
        public List<Long> getNextFewSchedules(String cronExpression, Long scheduleStartTime, Long scheduleEndTime,
                Integer limit) {
            return null;
        }

        @Override
        public void requeueAllExecutionRecords() {}

        @Override
        public SearchResultWorkflowScheduleExecution search(Integer start, Integer size, String sort,
                String freeText, String query) {
            return null;
        }

        @Override
        public void setSchedulerTags(List<TagObject> body, String name) {}

        @Override
        public void deleteSchedulerTags(List<TagObject> body, String name) {}

        @Override
        public List<TagObject> getSchedulerTags(String name) {
            return null;
        }
    }

    @Test
    void defaultReasonOverloadDelegatesToOneArgOverloadWhenNotImplemented() {
        LegacySchedulerClient client = new LegacySchedulerClient();

        client.pauseSchedule("s1", "some reason");

        assertEquals("s1", client.lastPausedName);
    }
}
