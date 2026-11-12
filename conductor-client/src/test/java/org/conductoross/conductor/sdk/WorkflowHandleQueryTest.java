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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Queries run in the client on state restored from the execution's variables. */
class WorkflowHandleQueryTest {

    /** A workflow whose query tries to change state, which must not stick. */
    public static class Ledger {
        @Variable private List<String> deposits = new ArrayList<>();

        @Query("deposits")
        public List<String> deposits() {
            List<String> copy = List.copyOf(deposits);
            deposits.add("written-by-a-query");
            return copy;
        }

        @Workflow(name = "query_test", version = 1)
        public String run(String input) {
            return input;
        }
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

    private void execution(String status, String depositsJson) {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(
                "{\"workflowId\":\"wf-1\",\"status\":\"" + status + "\",\"variables\":{\"deposits\":" + depositsJson
                        + "},\"nativeState\":{\"replies\":{}}}"));
    }

    @Test
    void aQueryReadsTheAppliedStateWithoutSendingAMessage() throws Exception {
        execution("RUNNING", "[\"dep-1\",\"dep-2\"]");
        List<String> answer = conductor.attach(Ledger::run, "wf-1").query(Ledger::deposits);
        assertEquals(List.of("dep-1", "dep-2"), answer);

        assertEquals(1, server.getRequestCount());
        RecordedRequest read = server.takeRequest();
        assertEquals("GET", read.getMethod()); // a read, no message pushed, nothing written
        assertTrue(read.getPath().startsWith("/api/workflow/wf-1"));
    }

    @Test
    void aQueryWorksOnACompletedExecutionAndItsChangesDoNotStick() {
        execution("COMPLETED", "[\"dep-1\"]");
        execution("COMPLETED", "[\"dep-1\"]");
        WorkflowHandle<String> handle = conductor.attach(Ledger::run, "wf-1");
        assertEquals(List.of("dep-1"), handle.query(Ledger::deposits));
        // the first query added to its own copy; the second sees the stored state again
        assertEquals(List.of("dep-1"), handle.query(Ledger::deposits));
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void repliesExpireAndAreCapped() {
        long now = 1_000_000_000L;
        Map<String, Object> replies = new java.util.LinkedHashMap<>();
        replies.put("legacy", Map.of("value", 1)); // no timestamp: an older worker's reply
        replies.put("old", Map.of("value", 2, "at", now - Workers.REPLY_TTL_MILLIS - 1));
        for (int i = 0; i < Workers.REPLY_LIMIT + 5; i++) {
            replies.put("r" + i, Map.of("value", i, "at", now - 1000 + i));
        }
        Map<String, Object> kept = Workers.pruneReplies(replies, now);
        assertEquals(Workers.REPLY_LIMIT, kept.size());
        assertTrue(!kept.containsKey("legacy") && !kept.containsKey("old") && !kept.containsKey("r0"));
        assertTrue(kept.containsKey("r" + (Workers.REPLY_LIMIT + 4))); // the newest is kept
    }
}
