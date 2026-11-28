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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.JsonSchemas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskContinuation;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** The entry a handed-out continuation's task carries: its lane, and its number in it. */
    private static WorkflowTask entry(String ref, String lane, int n) {
        var entry = new WorkflowTask();
        entry.setTaskReferenceName(ref);
        entry.setMetadata(new HashMap<>(Map.of("lane", lane, "continuation", n)));
        return entry;
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aContinuationThatFailsIsReportedAsTheCodesResultNotTheTasks() throws Exception {
        server.enqueue(json("{\"workflowId\":\"wf-1\",\"status\":\"RUNNING\"}"));
        TaskContinuation marker = new TaskContinuation();
        marker.setLeased(true);
        marker.setRetryCount(3);
        Task task = new Task();
        task.setWorkflowTask(entry("t", "main", 2)); // the entry names the continuation
        task.setTaskId("t-1");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("t");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setContinuation(marker);

        // no such workflow: the replay fails, and the completion TaskRunner sends says so
        TaskResult result = conductor.workflows().process("not.hosted.code", task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        Map<String, Object> output = result.getOutputData();
        assertEquals(2, output.get("protocol"));
        assertEquals(2, output.get("n"));
        assertEquals(3, output.get("retryCount"));
    }

    public static class Notes {
        @Variable
        private List<String> notes = new ArrayList<>();

        @Signal("add")
        public void add(String note) {
            notes.add(note);
        }

        @Workflow(name = "notes_test", version = 1)
        public String run() {
            return "done";
        }
    }

    public static class Ends {
        @Workflow(name = "ends_test", version = 1)
        public String run(String ignored) {
            return "done";
        }
    }

    @Test
    void theResultIsCheckedOnceTheServerHasTheContinuationThatEndedTheExecution() throws Exception {
        AtomicReference<String> status = new AtomicReference<>("RUNNING");
        AtomicInteger resultReads = new AtomicInteger();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("false".equals(request.getRequestUrl().queryParameter("includeTasks"))) {
                    resultReads.incrementAndGet(); // a check of the result
                }
                return json("{\"workflowId\":\"wf-1\",\"status\":\"" + status.get() + "\",\"output\":{\"result\":\"done\"}}");
            }
        });
        Workers workers = conductor.workflows(Ends.class);
        Task task = new Task();
        task.setWorkflowTask(entry("main", "main", 1));
        task.setTaskId("t-1");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("main");
        task.setStatus(Task.Status.IN_PROGRESS);
        TaskContinuation marker = new TaskContinuation();
        marker.setLeased(true);
        task.setContinuation(marker);
        // the code returns: this continuation ends the execution
        assertEquals(true, workers.process("ends_test.v1.code", task).getOutputData().get("done"));

        CompletableFuture<String> result = conductor.attach(Ends::run, "wf-1").resultAsync();
        long deadline = System.currentTimeMillis() + 5000;
        while (resultReads.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(5); // the first check; the next is 250 ms after it
        }
        status.set("COMPLETED");
        workers.sent("t-0", "wf-1"); // a continuation that did not end it
        Thread.sleep(30);
        assertEquals(1, resultReads.get(), "only the continuation that ends the execution checks its result");

        long sent = System.nanoTime();
        workers.sent("t-1", "wf-1");
        assertEquals("done", result.get(5, TimeUnit.SECONDS));
        long tookMillis = (System.nanoTime() - sent) / 1_000_000;
        assertTrue(tookMillis < 120, "checked " + tookMillis + " ms after the server had the completion");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aMessageAlreadyHandledIsSkippedAndEveryHandledMessageIsInTheReplies() throws Exception {
        long now = System.currentTimeMillis();
        // s-1 was handled by an earlier run; a retried send queued it again
        server.enqueue(json("{\"workflowId\":\"wf-1\",\"status\":\"RUNNING\",\"variables\":{\"notes\":[\"first\"]},"
                + "\"nativeState\":{\"replies\":{\"s-1\":{\"at\":" + now + "}}}}"));
        TaskContinuation marker = new TaskContinuation();
        marker.setN(2); // the handler lane's second run
        marker.setLeased(true);
        marker.setRetryCount(1);
        Task task = new Task();
        task.setWorkflowTask(entry("handlers", "handlers", 2)); // as the server builds it
        task.setTaskId("wf-1|handlers");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("handlers");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setContinuation(marker);
        task.setInputData(Map.of("messages", List.of(
                Map.of("id", "q-1", "payload", Map.of("kind", "signal", "name", "add", "id", "s-1", "args", "first")),
                Map.of("id", "q-2", "payload", Map.of("kind", "signal", "name", "add", "id", "s-2", "args", "second")))));

        TaskResult result = conductor.workflows(Notes.class).process("notes_test.v1.code", task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> output = result.getOutputData();
        assertEquals(List.of("first", "second"), ((Map<String, Object>) output.get("variables")).get("notes"));
        Map<String, Object> replies = (Map<String, Object>) output.get("replies");
        assertEquals(Set.of("s-1", "s-2"), replies.keySet());
        // a signal's entry says only when it was handled
        assertEquals(Set.of("at"), ((Map<String, Object>) replies.get("s-2")).keySet());
        assertEquals(null, output.get("seen"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aTaskDefinitionOwnedBySomeoneElseIsLeftAlone() throws Exception {
        List<String> posted = answer(
                "[]", "[{\"name\":\"shared_task\",\"ownerEmail\":\"team-a@example.com\",\"retryCount\":7}]");

        Registration.register(conductor.client(), List.of(), List.of(new Mine()), spec -> 30, true);

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
        String input = Json.MAPPER.writeValueAsString(JsonSchemas.taskInput(
                Typed.class.getMethod("typed", int.class)));
        List<String> posted = answer(
                "[{\"name\":\"typed_task_input\",\"version\":2,\"data\":" + input + "},"
                        + "{\"name\":\"typed_task_output\",\"version\":4,\"data\":{\"type\":\"string\"}}]",
                "[]");

        Registration.register(conductor.client(), List.of(), List.of(new Typed()), spec -> 30, true);

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
        List<String> posted = new CopyOnWriteArrayList<>();
        server.setDispatcher(new Dispatcher() {
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
