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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.conductoross.conductor.sdk.internal.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskContinuation;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An update from the caller to the handler and back: {@link WorkflowHandle#update} sends the
 * message, a worker's handler run answers it, and the caller reads the reply the server kept.
 */
class UpdateReplyTest {

    public record Receipt(String id, int total) {}

    public static class Orders {
        @Update
        public Receipt lookup(String id) {
            return "known".equals(id) ? new Receipt(id, 3) : null;
        }

        @Update
        public String label(String id) {
            return null;
        }

        @Workflow(name = "update_reply_test", version = 1)
        public String run(String input) {
            return input;
        }
    }

    private MockWebServer server;
    private Conductor conductor;
    /** The message the caller posted, and the replies the server keeps for the execution. */
    private final AtomicReference<Map<String, Object>> posted = new AtomicReference<>();
    private final AtomicReference<Object> replies = new AtomicReference<>(Map.of());

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            @SuppressWarnings("unchecked")
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse json = new MockResponse().setHeader("Content-Type", "application/json");
                try {
                    if ("POST".equals(request.getMethod())) {
                        posted.set(Json.MAPPER.readValue(request.getBody().readUtf8(), Map.class));
                        return json.setBody("\"queued\"");
                    }
                    return json.setBody(Json.MAPPER.writeValueAsString(Map.of(
                            "workflowId", "wf-1", "status", "RUNNING", "nativeState", Map.of("replies", replies.get()))));
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500);
                }
            }
        });
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        server.shutdown();
    }

    /** Sends the update, has a worker run the handler lane for it, and returns what the caller got. */
    private <V> V update(Functions.Fn1<Orders, String, V> handler, String argument) throws Exception {
        WorkflowHandle<String> handle = conductor.attach(Orders::run, "wf-1");
        CompletableFuture<V> answer = CompletableFuture.supplyAsync(() -> handle.update(handler, argument));
        while (posted.get() == null) {
            Thread.sleep(10);
        }
        TaskContinuation marker = TaskContinuation.handlerRun(2);
        marker.setLeased(true);
        var entry = new WorkflowTask();
        entry.setTaskReferenceName("handlers");
        entry.setMetadata(new HashMap<>(Map.of("lane", "handlers", "continuation", 2)));
        Task task = new Task();
        task.setWorkflowTask(entry);
        task.setTaskId("wf-1|handlers");
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("handlers");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setContinuation(marker);
        task.setInputData(Map.of("messages", List.of(Map.of("id", "q-1", "payload", posted.get()))));

        TaskResult result = conductor.workflows(Orders.class).process("update_reply_test.v1.code", task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        replies.set(result.getOutputData().get("replies")); // the server keeps them with the execution
        try {
            return answer.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void anUpdateReturnsWhatItsHandlerReturned() throws Exception {
        assertEquals(new Receipt("known", 3), update(Orders::lookup, "known"));
    }

    @Test
    void anUpdateWhoseHandlerReturnsNullReturnsNullForAnObject() throws Exception {
        assertNull(update(Orders::lookup, "unknown"));
    }

    @Test
    void anUpdateWhoseHandlerReturnsNullReturnsNullForAString() throws Exception {
        assertNull(update(Orders::label, "x"));
    }
}
