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
import com.netflix.conductor.sdk.workflow.task.InputParam;
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
        List<String> posted = answer(
                "[]", "[{\"name\":\"shared_task\",\"ownerEmail\":\"team-a@example.com\",\"retryCount\":7}]");

        Registration.register(conductor.client(), List.of(), List.of(new Mine()), 30);

        List<Map<String, Object>> defs = Json.MAPPER.readValue(last(posted, "/api/metadata/taskdefs"), List.class);
        assertEquals(List.of("my_task"), defs.stream().map(d -> d.get("name")).toList());
    }

    public static class Typed {
        @WorkerTask("typed_task")
        public String typed(@InputParam("n") int n) {
            return "x";
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void schemasAreRegisteredByNameVersionedByContentAndNotEnforced() throws Exception {
        // the input schema is registered already as version 2; the output had another shape
        String input = Json.MAPPER.writeValueAsString(org.conductoross.conductor.sdk.internal.JsonSchemas.taskInput(
                Typed.class.getMethod("typed", int.class)));
        List<String> posted = answer(
                "[{\"name\":\"typed_task_input\",\"version\":2,\"data\":" + input + "},"
                        + "{\"name\":\"typed_task_output\",\"version\":4,\"data\":{\"type\":\"string\"}}]",
                "[]");

        Registration.register(conductor.client(), List.of(), List.of(new Typed()), 30);

        // only the output is new: the next version after the highest registered
        List<Map<String, Object>> schemas = Json.MAPPER.readValue(last(posted, "/api/schema"), List.class);
        assertEquals("typed_task_output", schemas.get(0).get("name"));
        assertEquals(5, schemas.get(0).get("version"));
        assertEquals(1, posted.stream().filter(p -> p.startsWith("/api/schema ")).count());
        Map<String, Object> def = ((List<Map<String, Object>>) Json.MAPPER.readValue(
                last(posted, "/api/metadata/taskdefs"), List.class)).get(0);
        assertEquals(Map.of("name", "typed_task_input", "version", 2, "type", "JSON"), def.get("inputSchema"));
        assertEquals(Map.of("name", "typed_task_output", "version", 5, "type", "JSON"), def.get("outputSchema"));
        assertEquals(false, def.get("enforceSchema"));
    }

    /** Answers by path, the schemas and task definitions given; records each POST as "path body". */
    private List<String> answer(String schemas, String taskDefs) {
        List<String> posted = new java.util.concurrent.CopyOnWriteArrayList<>();
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath().replaceAll("\\?.*", "");
                if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
                    posted.add(path + " " + request.getBody().readUtf8());
                    return json("\"ok\"");
                }
                return json(path.equals("/api/schema") ? schemas : path.equals("/api/metadata/taskdefs") ? taskDefs : "[]");
            }
        });
        return posted;
    }

    private static String last(List<String> posted, String path) {
        String found = null;
        for (String p : posted) {
            if (p.startsWith(path + " ")) {
                found = p.substring(path.length() + 1);
            }
        }
        return found;
    }
}
