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

import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A server that is down never fails hosting: registration is retried until it succeeds. */
class ServerOutageTest {

    public static class Flow {
        @Workflow(name = "outage_flow")
        public String run(@InputParam("x") String x) {
            return x;
        }
    }

    @Test
    void registrationIsRetriedInTheBackgroundUntilTheServerAcceptsIt() throws Exception {
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger registered = new AtomicInteger();
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.startsWith("/api/metadata/") && rejected.get() < 2) {
                    rejected.incrementAndGet();
                    return new MockResponse().setResponseCode(503).setBody("down");
                }
                if (path.startsWith("/api/metadata/workflow") && "PUT".equals(request.getMethod())) {
                    registered.incrementAndGet();
                }
                String body = path.startsWith("/api/metadata/taskdefs") && "GET".equals(request.getMethod())
                        || path.startsWith("/api/tasks/poll") ? "[]" : "{}";
                return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
            }
        });
        server.start();
        Conductor conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
        try {
            long started = System.nanoTime();
            Workers workers = conductor.host().workflows(Flow.class).start();
            assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() < 1000, "start does not wait for the server");

            long deadline = System.currentTimeMillis() + 15_000;
            while (!workers.registered() && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertTrue(workers.registered(), "registered once the server accepted it");
            assertEquals(2, rejected.get());
            assertEquals(1, registered.get());
        } finally {
            conductor.close();
            server.shutdown();
        }
    }

    @Test
    void anUnreachableServerDoesNotFailTheStart() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort(); // closed again: nothing listens there
        }
        Conductor conductor = Conductor.of(ConductorClient.builder().basePath("http://localhost:" + port + "/api").build());
        Workers workers = conductor.host().workflows(Flow.class).start();
        Thread.sleep(1500);
        assertFalse(workers.registered());
        conductor.close(); // stops the retries
    }
}
