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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** resultAsync: a future of the execution's outcome, checked by one shared thread. */
class WorkflowHandleResultAsyncTest {

    public static class Greeter {
        @Workflow(name = "result_async_test", version = 1)
        public String run(String name) {
            return "hello " + name;
        }
    }

    private MockWebServer server;
    private Conductor conductor;
    /** What the next read of the execution returns: a status, or an HTTP code such as "503". */
    private final AtomicReference<String> next = new AtomicReference<>("RUNNING");
    private final AtomicInteger reads = new AtomicInteger();

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                reads.incrementAndGet();
                String state = next.get();
                if (state.matches("\\d+")) {
                    return new MockResponse().setResponseCode(Integer.parseInt(state)).setBody("{}");
                }
                String body = "{\"workflowId\":\"wf-1\",\"status\":\"" + state + "\",\"reasonForIncompletion\":\"card declined\""
                        + ",\"output\":{\"result\":\"hello ada\"}}";
                return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
            }
        });
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        conductor.close();
        server.shutdown();
    }

    @Test
    void completesWithTheResultOnceTheExecutionFinishesWaitingOutAServerError() throws Exception {
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        awaitReads(2);
        next.set("503"); // a restarting server
        awaitReads(2);
        assertTrue(!result.isDone(), "still waiting while the execution runs");
        next.set("COMPLETED");
        assertEquals("hello ada", result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void failsWithWorkflowFailedExceptionWhenTheExecutionFails() {
        next.set("FAILED");
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        ExecutionException e = assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertInstanceOf(WorkflowFailedException.class, e.getCause());
        assertTrue(e.getCause().getMessage().contains("card declined"), e.getCause().getMessage());
    }

    @Test
    void failsWhenTheServerRefusesTheRead() {
        next.set("404");
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void anExecutionEndedHereIsCheckedAtOnceNotAtTheNextCheck() throws Exception {
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        awaitReads(1); // the first check; the next is 250 ms after it
        next.set("COMPLETED");
        conductor.checkResultNow("wf-2"); // another execution: nothing to check
        Thread.sleep(30);
        assertTrue(!result.isDone(), "a check for another execution does not read this one");

        long nudged = System.nanoTime();
        conductor.checkResultNow("wf-1");
        assertEquals("hello ada", result.get(5, TimeUnit.SECONDS));
        long tookMillis = (System.nanoTime() - nudged) / 1_000_000;
        assertTrue(tookMillis < 120, "checked " + tookMillis + " ms after the execution ended here");
    }

    @Test
    void cancellingStopsTheChecks() throws Exception {
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        awaitReads(1);
        result.cancel(false);
        Thread.sleep(300); // a check already under way may finish
        int after = reads.get();
        Thread.sleep(1000);
        assertEquals(after, reads.get(), "no reads after the future was cancelled");
    }

    @Test
    void closingTheConductorCancelsTheFuturesStillWaiting() throws Exception {
        CompletableFuture<String> result = conductor.attach(Greeter::run, "wf-1").resultAsync();
        awaitReads(1);
        conductor.close();
        assertTrue(result.isCancelled());
    }

    private void awaitReads(int more) throws InterruptedException {
        int target = reads.get() + more;
        long deadline = System.currentTimeMillis() + 5000;
        while (reads.get() < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(reads.get() >= target, "the execution was read " + reads.get() + " times");
    }
}
