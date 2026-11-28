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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** One {@link Conductor} used from several threads: a start while another thread adds workers. */
class ConcurrentStartTest {

    public static class First {
        @Workflow(name = "concurrent_first", version = 1)
        public String run(@InputParam("input") String input) {
            return input;
        }
    }

    public static class Second {
        @Workflow(name = "concurrent_second", version = 1)
        public String run(@InputParam("input") String input) {
            return input;
        }
    }

    private MockWebServer server;
    private Conductor conductor;
    /** Held until the test lets the server answer: the registration of First waits on it. */
    private final CountDownLatch serverAnswers = new CountDownLatch(1);

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String path = request.getPath() == null ? "" : request.getPath().replaceAll("\\?.*", "");
                MockResponse json = new MockResponse().setHeader("Content-Type", "application/json");
                if (path.startsWith("/api/tasks/")) {
                    return json.setBody("[]"); // polls find nothing
                }
                serverAnswers.await();
                if ("POST".equals(request.getMethod()) && path.equals("/api/workflow")) {
                    return new MockResponse().setBody("wf-1"); // the id, as text
                }
                return json.setBody("GET".equals(request.getMethod()) ? "[]" : "\"ok\"");
            }
        });
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        serverAnswers.countDown();
        conductor.close();
        server.shutdown();
    }

    @Test
    void aStartWaitingForRegistrationIsNotBrokenByAnotherThreadAddingWorkers() throws Exception {
        Workers first = conductor.workflows(First.class).start(); // registering: the server has not answered

        // a start in the same process waits for that registration...
        CompletableFuture<String> started = CompletableFuture.supplyAsync(
                () -> conductor.start(First::run, "x").workflowId());
        Thread.sleep(300);
        // ... and meanwhile another thread hosts another workflow on the same Conductor
        conductor.workflows(Second.class);
        serverAnswers.countDown();

        assertEquals("wf-1", started.get(20, TimeUnit.SECONDS));
        first.close();
    }
}
