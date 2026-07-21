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
package org.conductoross.conductor.ai.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.conductoross.conductor.ai.AgentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import io.orkes.conductor.client.ApiClient;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec R6 (T9/T10): declared secret names are stamped on
 * {@code TaskDef.runtimeMetadata} at registration (and re-stamped on EVERY
 * re-registration — the upsert overwrites the whole def), and dispatch resolves
 * values only from the wire-delivered {@code Task.runtimeMetadata} — fail
 * closed, never from ambient process env.
 */
@Timeout(30)
class WorkerManagerCredentialsTest {

    private MockWebServer server;

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
        CredentialContext.clear();
    }

    /** WorkerManager whose metadata upserts land on a stub server that records bodies. */
    private WorkerManager stampRecordingManager(List<String> taskDefBodies) throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() != null ? request.getPath() : "";
                if (path.startsWith("/api/metadata/taskdefs")) {
                    taskDefBodies.add(request.getBody().readUtf8());
                }
                return new MockResponse().setHeader("Content-Type", "application/json").setBody("{}");
            }
        });
        server.start();
        ApiClient apiClient = ApiClient.builder()
                .basePath(server.url("/api").toString())
                .build();
        return new WorkerManager(new AgentConfig(100, 1), apiClient);
    }

    /** WorkerManager with an unreachable transport — for pure dispatch tests. */
    private static WorkerManager offlineManager() {
        return new WorkerManager(new AgentConfig(100, 1), new ConductorClient("http://localhost:1/api"));
    }

    private static Task taskWithDelivered(Map<String, String> delivered) {
        Task task = new Task();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("wf-1");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setRuntimeMetadata(delivered);
        task.setInputData(new HashMap<>(Map.of("x", "test")));
        return task;
    }

    // ── T9: stamping ─────────────────────────────────────────────────────────

    @Test
    void registrationStampsDeclaredNamesOnTaskDef() throws IOException {
        List<String> bodies = new CopyOnWriteArrayList<>();
        WorkerManager manager = stampRecordingManager(bodies);

        manager.register("cred_task", input -> Map.of(), null, List.of("API_KEY", "DB_PASSWORD"));

        assertFalse(bodies.isEmpty(), "registration must upsert the task def");
        assertTrue(
                bodies.get(0).contains("\"runtimeMetadata\":[\"API_KEY\",\"DB_PASSWORD\"]"),
                "declared names must be stamped on TaskDef.runtimeMetadata; body=" + bodies.get(0));
    }

    @Test
    void reRegistrationReStampsTheTaskDef() throws IOException {
        List<String> bodies = new CopyOnWriteArrayList<>();
        WorkerManager manager = stampRecordingManager(bodies);

        manager.register("cred_task", input -> Map.of(), null, List.of("API_KEY"));
        int upsertsAfterFirst = bodies.size();
        manager.register("cred_task", input -> Map.of(), null, List.of("API_KEY"));

        assertTrue(
                bodies.size() > upsertsAfterFirst,
                "COUNTERFACTUAL: the old !isNew early-return skipped the task-def upsert on "
                        + "re-registration, so a re-register could leave a stale (or wiped) stamp");
        assertTrue(
                bodies.get(bodies.size() - 1).contains("\"runtimeMetadata\":[\"API_KEY\"]"),
                "the re-register upsert must carry the stamp too; body=" + bodies.get(bodies.size() - 1));
    }

    @Test
    void registrationWithoutCredentialsCarriesNoStamp() throws IOException {
        List<String> bodies = new CopyOnWriteArrayList<>();
        WorkerManager manager = stampRecordingManager(bodies);

        manager.register("plain_task", input -> Map.of());

        assertFalse(bodies.isEmpty());
        assertFalse(
                bodies.get(0).contains("runtimeMetadata\":["),
                "no declared credentials → no stamp; body=" + bodies.get(0));
    }

    // ── T10: dispatch ────────────────────────────────────────────────────────

    @Test
    void deliveredValuesReachTheHandlerAndAreClearedAfter() {
        WorkerManager manager = offlineManager();
        AtomicReference<Map<String, String>> seenByHandler = new AtomicReference<>();
        manager.register(
                "cred_task",
                input -> {
                    seenByHandler.set(CredentialContext.current());
                    return Map.of("ok", true);
                },
                null,
                List.of("API_KEY"));

        TaskResult result =
                manager.executeHandler("cred_task", taskWithDelivered(Map.of("API_KEY", "delivered-value")));

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(
                Map.of("API_KEY", "delivered-value"),
                seenByHandler.get(),
                "the wire-delivered value must be visible to the handler via CredentialContext");
        assertTrue(
                CredentialContext.current().isEmpty(),
                "the per-call secret context must be cleared after the handler returns");
    }

    @Test
    void missingDeliveryFailsTerminallyAndNeverReadsAmbientEnv() {
        WorkerManager manager = offlineManager();
        AtomicBoolean handlerRan = new AtomicBoolean(false);
        // PATH is guaranteed present in the ambient process env — if the SDK fell
        // back to env for a declared-but-undelivered name, this test would COMPLETE.
        manager.register(
                "cred_task",
                input -> {
                    handlerRan.set(true);
                    return Map.of();
                },
                null,
                List.of("PATH"));

        TaskResult result = manager.executeHandler("cred_task", taskWithDelivered(Map.of()));

        assertEquals(
                TaskResult.Status.FAILED_WITH_TERMINAL_ERROR,
                result.getStatus(),
                "a declared-but-undelivered credential is a config problem — retries are pointless");
        assertFalse(handlerRan.get(), "the handler must not run without its declared credentials");
        assertTrue(result.getReasonForIncompletion().contains("PATH"), "the missing name must be reported");
        assertTrue(
                result.getReasonForIncompletion().contains("Conductor OSS with PR #1255"),
                "the server capability requirement must be named: " + result.getReasonForIncompletion());
    }

    @Test
    void nullRuntimeMetadataAlsoFailsClosed() {
        WorkerManager manager = offlineManager();
        manager.register("cred_task", input -> Map.of(), null, List.of("API_KEY"));

        Task task = taskWithDelivered(null);
        TaskResult result = manager.executeHandler("cred_task", task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void partialDeliveryNamesOnlyTheMissing() {
        WorkerManager manager = offlineManager();
        manager.register("cred_task", input -> Map.of(), null, List.of("API_KEY", "DB_PASSWORD"));

        TaskResult result = manager.executeHandler("cred_task", taskWithDelivered(Map.of("API_KEY", "present")));

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("DB_PASSWORD"));
        assertFalse(
                result.getReasonForIncompletion().contains("API_KEY"),
                "delivered names must not be reported as missing");
    }
}
