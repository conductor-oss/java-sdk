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

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentConfig;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.exceptions.WorkerStallError;
import org.conductoross.conductor.ai.model.AgentHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.netflix.conductor.client.http.WorkflowClient;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.http.OrkesAgentClient;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec R11 (T16): a stateful run whose worker queue nothing polls must surface
 * as {@link WorkerStallError} within the liveness window instead of burning
 * the full wait timeout; healthy and terminal runs are never flagged.
 */
@Timeout(30)
class ServerLivenessMonitorTest {

    private MockWebServer server;
    private volatile String workflowStatus = "RUNNING";
    private volatile int pollCount = 0;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() != null ? request.getPath() : "";
                if (path.startsWith("/api/agent/start")) {
                    return json("{\"executionId\":\"exec-1\"}");
                }
                if (path.equals("/api/agent/exec-1/status")) {
                    return json("{\"executionId\":\"exec-1\",\"status\":\"RUNNING\",\"isComplete\":false,"
                            + "\"isRunning\":true}");
                }
                if (path.startsWith("/api/workflow/exec-1")) {
                    long scheduledAgo = System.currentTimeMillis() - 60_000;
                    return json("{\"workflowId\":\"exec-1\",\"status\":\"" + workflowStatus + "\",\"tasks\":["
                            + "{\"taskId\":\"t1\",\"status\":\"SCHEDULED\",\"pollCount\":" + pollCount + ","
                            + "\"scheduledTime\":" + scheduledAgo + ",\"referenceTaskName\":\"stateful_tool_ref\","
                            + "\"taskDefName\":\"stateful_tool\",\"workflowInstanceId\":\"exec-1\","
                            + "\"taskType\":\"stateful_tool\"}]}");
                }
                return json("[]");
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private ApiClient apiClient() {
        return AgentRuntime.client(server.url("/").toString());
    }

    private ServerLivenessMonitor monitor() {
        return new ServerLivenessMonitor(new WorkflowClient(apiClient()), "exec-1", 0.2, 0.05, true);
    }

    private static String awaitStall(ServerLivenessMonitor monitor, long deadlineMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + deadlineMs;
        while (System.currentTimeMillis() < deadline) {
            String stalled = monitor.stalledTask();
            if (stalled != null) {
                return stalled;
            }
            Thread.sleep(20);
        }
        return monitor.stalledTask();
    }

    @Test
    void recordsStallForUnpolledScheduledTask() throws InterruptedException {
        try (ServerLivenessMonitor monitor = monitor()) {
            monitor.start();

            assertEquals("stateful_tool_ref", awaitStall(monitor, 5_000));
        }
    }

    @Test
    void polledTaskIsNeverFlagged() throws InterruptedException {
        pollCount = 3;
        try (ServerLivenessMonitor monitor = monitor()) {
            monitor.start();

            assertNull(awaitStall(monitor, 600), "a task workers are polling is healthy");
        }
    }

    @Test
    void terminalWorkflowStopsMonitoringWithoutFlagging() throws InterruptedException {
        workflowStatus = "COMPLETED";
        try (ServerLivenessMonitor monitor = monitor()) {
            monitor.start();

            assertNull(awaitStall(monitor, 600));
        }
    }

    @Test
    void handleWaitSurfacesTheStall() throws InterruptedException {
        ApiClient apiClient = apiClient();
        ServerLivenessMonitor monitor = monitor();
        monitor.start();
        AgentHandle handle =
                new AgentHandle("exec-1", new OrkesAgentClient(apiClient), new WorkflowClient(apiClient), monitor);

        WorkerStallError error =
                assertThrows(WorkerStallError.class, () -> handle.waitForResult(20_000, 50));

        assertEquals("stateful_tool_ref", error.getTaskReferenceName());
        assertEquals("exec-1", error.getExecutionId());
    }

    @Test
    void statefulStartAttachesTheMonitorEndToEnd() {
        AgentRuntime runtime = new AgentRuntime(
                apiClient(),
                new AgentConfig().livenessStallSeconds(0.2).livenessCheckIntervalSeconds(0.05));
        try {
            AgentHandle handle = runtime.start(statefulAgent(), "hi");

            assertThrows(
                    WorkerStallError.class,
                    () -> handle.waitForResult(20_000, 50),
                    "COUNTERFACTUAL: without R11 this wait burns its full timeout on a dead queue");
        } finally {
            runtime.shutdown();
        }
    }

    @Test
    void livenessDisabledFallsBackToPlainTimeout() {
        AgentRuntime runtime = new AgentRuntime(
                apiClient(),
                new AgentConfig()
                        .livenessEnabled(false)
                        .livenessStallSeconds(0.2)
                        .livenessCheckIntervalSeconds(0.05));
        try {
            AgentHandle handle = runtime.start(statefulAgent(), "hi");

            RuntimeException timeout =
                    assertThrows(RuntimeException.class, () -> handle.waitForResult(700, 50));
            assertFalse(timeout instanceof WorkerStallError, "livenessEnabled=false must not construct a monitor");
            assertTrue(timeout.getMessage().contains("timed out"), "plain timeout expected: " + timeout.getMessage());
        } finally {
            runtime.shutdown();
        }
    }

    private static Agent statefulAgent() {
        return Agent.builder()
                .name("stateful_agent")
                .model("openai/gpt-4o")
                .instructions("You are a test agent.")
                .stateful(true)
                .build();
    }
}
