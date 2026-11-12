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
package org.conductoross.conductor.sdk;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskContinuation;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a worker sends the server: failed continuations, and task definitions it does not own. */
class WorkerProtocolTest {

    public static class Mine {
        @WorkerTask("shared_task")
        public void shared() {}

        @WorkerTask("my_task")
        public void mine() {}
    }

    private MockWebServer server;
    private Conductor conductor;

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aContinuationThatFailsIsReportedAsTheCodesResultNotTheTasks() throws Exception {
        server.enqueue(json("{\"workflowId\":\"wf-1\",\"status\":\"RUNNING\"}"));
        server.enqueue(new MockResponse().setResponseCode(204));
        TaskContinuation marker = new TaskContinuation("main", 2);
        marker.setState(TaskContinuation.LEASED);
        marker.setEpoch(3);
        Task task = new Task();
        task.setTaskId("t-1");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("t");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setContinuation(marker);

        conductor.host().process("not.hosted.code", task); // no such workflow: replay fails

        server.takeRequest(); // the fetch of the execution
        RecordedRequest update = server.takeRequest();
        assertEquals("/api/tasks/update-v2", update.getPath());
        Map<String, Object> body = Json.MAPPER.readValue(update.getBody().readUtf8(), Map.class);
        assertEquals("FAILED", body.get("status"));
        Map<String, Object> output = (Map<String, Object>) body.get("outputData");
        assertEquals(2, output.get("protocol"));
        assertEquals(2, output.get("n"));
        assertEquals(3, output.get("epoch"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aTaskDefinitionOwnedBySomeoneElseIsLeftAlone() throws Exception {
        server.enqueue(json("[{\"name\":\"shared_task\",\"ownerEmail\":\"team-a@example.com\",\"retryCount\":7}]"));
        server.enqueue(json("\"ok\""));

        Registration.register(conductor.client(), List.of(), List.of(new Mine()), 30);

        server.takeRequest();
        RecordedRequest post = server.takeRequest();
        List<Map<String, Object>> defs = Json.MAPPER.readValue(post.getBody().readUtf8(), List.class);
        assertEquals(List.of("my_task"), defs.stream().map(d -> d.get("name")).toList());
    }
}
