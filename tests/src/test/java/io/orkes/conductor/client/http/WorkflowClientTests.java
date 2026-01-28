/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client.http;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.conductoross.conductor.common.model.WorkflowStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.RerunWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.common.run.WorkflowTestRequest;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.Http;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Wait;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.util.ClientTestUtil;
import io.orkes.conductor.client.util.Commons;
import io.orkes.conductor.client.util.TestUtil;

import com.google.common.util.concurrent.Uninterruptibles;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowClientTests {
    private static OrkesWorkflowClient workflowClient;
    private static OrkesMetadataClient metadataClient;
    private static OrkesTaskClient taskClient;
    private static WorkflowExecutor workflowExecutor;

    private static final String WAIT_WF_NAME = "test_wait_wf_coverage";
    private static final String TEST_TASK_NAME = "test_simple_task_coverage";

    @BeforeAll
    public static void setup() {
        workflowClient = ClientTestUtil.getOrkesClients().getWorkflowClient();
        metadataClient = ClientTestUtil.getOrkesClients().getMetadataClient();
        taskClient = ClientTestUtil.getOrkesClients().getTaskClient();
        workflowExecutor = new WorkflowExecutor(ClientTestUtil.getClient(), 10);

        registerAdditionalWorkflows();
    }

    @AfterAll
    public static void cleanup() {
        try {
            metadataClient.unregisterWorkflowDef(WAIT_WF_NAME, 1);
        } catch (Exception ignored) {
        }
    }

    private static void registerAdditionalWorkflows() {
        // Register task def for simple workflow tests
        TaskDef taskDef = new TaskDef(TEST_TASK_NAME);
        taskDef.setRetryCount(0);
        taskDef.setOwnerEmail("test@test.com");
        try {
            metadataClient.registerTaskDefs(List.of(taskDef));
        } catch (Exception ignored) {
        }

        // Register wait workflow for pause/resume tests
        ConductorWorkflow<Object> waitWf = new ConductorWorkflow<>(workflowExecutor);
        waitWf.setName(WAIT_WF_NAME);
        waitWf.setVersion(1);
        waitWf.add(new Wait("wait_task", Duration.ofSeconds(300)));
        waitWf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        waitWf.setTimeoutSeconds(600);
        waitWf.registerWorkflow(true, true);
    }

    @Test
    public void startWorkflow() {
        String workflowId = workflowClient.startWorkflow(getStartWorkflowRequest());
        Workflow workflow = workflowClient.getWorkflow(workflowId, false);
        Assertions.assertEquals(workflow.getWorkflowName(), Commons.WORKFLOW_NAME);
    }

    @Test
    public void testSearchByCorrelationIds() {
        List<String> correlationIds = new ArrayList<>();
        Set<String> workflowNames = new HashSet<>();
        Map<String, Set<String>> correlationIdToWorkflows = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            String correlationId = UUID.randomUUID().toString();
            correlationIds.add(correlationId);
            for (int j = 0; j < 5; j++) {
                ConductorWorkflow<Object> workflow = new ConductorWorkflow<>(workflowExecutor);
                workflow.add(new Http("http").url("https://orkes-api-tester.orkesconductor.com/get"));
                workflow.setName("workflow_" + j);
                workflowNames.add(workflow.getName());
                StartWorkflowRequest request = new StartWorkflowRequest();
                request.setName(workflow.getName());
                request.setWorkflowDef(workflow.toWorkflowDef());
                request.setCorrelationId(correlationId);
                String id = workflowClient.startWorkflow(request);
                System.out.println("started " + id);
                Set<String> ids = correlationIdToWorkflows.getOrDefault(correlationId, new HashSet<>());
                ids.add(id);
                correlationIdToWorkflows.put(correlationId, ids);
            }
        }
        // Let's give couple of seconds for indexing to complete
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
        Map<String, List<Workflow>> result = workflowClient.getWorkflowsByNamesAndCorrelationIds(correlationIds,
                new ArrayList<>(workflowNames), true, false);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(correlationIds.size(), result.size());
        for (String correlationId : correlationIds) {
            Assertions.assertEquals(5, result.get(correlationId).size());
            Set<String> ids = result.get(correlationId).stream().map(Workflow::getWorkflowId)
                    .collect(Collectors.toSet());
            Assertions.assertEquals(correlationIdToWorkflows.get(correlationId), ids);
        }
    }

    @Test
    public void testWorkflowTerminate() {
        String workflowId = workflowClient.startWorkflow(getStartWorkflowRequest());
        Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(100));
        workflowClient.terminateWorkflowWithFailure(
                workflowId, "testing out some stuff", true);
        var workflow = workflowClient.getWorkflow(workflowId, false);
        Assertions.assertEquals(Workflow.WorkflowStatus.TERMINATED, workflow.getStatus());
    }

    @Test
    public void testSkipTaskFromWorkflow() throws Exception {
        var workflowName = "random_workflow_name_1hqiuwheiquwhe";
        var taskName1 = "random_task_name_1hqiuwheiquwheajnsdsand";
        var taskName2 = "random_task_name_1hqiuwheiquwheajnsdsandjsadh";

        var taskDef1 = new TaskDef(taskName1);
        taskDef1.setRetryCount(0);
        taskDef1.setOwnerEmail("test@orkes.io");
        var taskDef2 = new TaskDef(taskName2);
        taskDef2.setRetryCount(0);
        taskDef2.setOwnerEmail("test@orkes.io");

        TestUtil.retryMethodCall(
                () -> metadataClient.registerTaskDefs(List.of(taskDef1, taskDef2)));

        var wf = new ConductorWorkflow<>(workflowExecutor);
        wf.setName(workflowName);
        wf.setVersion(1);
        wf.add(new SimpleTask(taskName1, taskName1));
        wf.add(new SimpleTask(taskName2, taskName2));
        TestUtil.retryMethodCall(
                () -> wf.registerWorkflow(true));

        StartWorkflowRequest startWorkflowRequest = new StartWorkflowRequest();
        startWorkflowRequest.setName(workflowName);
        startWorkflowRequest.setVersion(1);
        startWorkflowRequest.setInput(new HashMap<>());
        var workflowId = (String) TestUtil.retryMethodCall(
                () -> workflowClient.startWorkflow(startWorkflowRequest));
        System.out.println("workflowId: " + workflowId);

        TestUtil.retryMethodCall(
                () -> workflowClient.skipTaskFromWorkflow(workflowId, taskName2));
        TestUtil.retryMethodCall(
                () -> workflowClient.terminateWorkflowsWithFailure(List.of(workflowId), null, false));
    }

    @Test
    public void testUpdateVariables() {
        ConductorWorkflow<Object> workflow = new ConductorWorkflow<>(workflowExecutor);
        workflow.add(new SimpleTask("simple_task", "simple_task_ref"));
        workflow.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        workflow.setTimeoutSeconds(60);
        workflow.setName("update_variable_test");
        workflow.setVersion(1);
        workflow.registerWorkflow(true, true);

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflow.getName());
        request.setVersion(workflow.getVersion());
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        Assertions.assertNotNull(workflowId);

        Workflow execution = workflowClient.getWorkflow(workflowId, false);
        Assertions.assertNotNull(execution);
        Assertions.assertTrue(execution.getVariables().isEmpty());

        Map<String, Object> variables = Map.of("k1", "v1", "k2", 42, "k3", Arrays.asList(3, 4, 5));
        execution = workflowClient.updateVariables(workflowId, variables);
        Assertions.assertNotNull(execution);
        Assertions.assertFalse(execution.getVariables().isEmpty());
        Assertions.assertEquals(variables.get("k1"), execution.getVariables().get("k1"));
        Assertions.assertEquals(variables.get("k2").toString(), execution.getVariables().get("k2").toString());
        Assertions.assertEquals(variables.get("k3").toString(), execution.getVariables().get("k3").toString());

        Map<String, Object> map = new HashMap<>();
        map.put("k1", null);
        map.put("v1", "xyz");
        execution = workflowClient.updateVariables(workflowId, map);
        Assertions.assertNotNull(execution);
        Assertions.assertFalse(execution.getVariables().isEmpty());
        Assertions.assertNull(execution.getVariables().get("k1"));
        Assertions.assertEquals(variables.get("k2").toString(), execution.getVariables().get("k2").toString());
        Assertions.assertEquals(variables.get("k3").toString(), execution.getVariables().get("k3").toString());
        Assertions.assertEquals("xyz", execution.getVariables().get("v1").toString());
    }

    @Test
    void testExecuteWorkflow() {
        // TODO
    }

    @Test
    void testWorkflow() {
        WorkflowTask task = new WorkflowTask();
        task.setName("testable-task");
        task.setTaskReferenceName("testable-task-ref");

        WorkflowDef workflowDef = new WorkflowDef();
        workflowDef.setName("testable-flow");
        workflowDef.setTasks(List.of(task));

        WorkflowTestRequest testRequest = new WorkflowTestRequest();
        testRequest.setName("testable-flow");
        testRequest.setWorkflowDef(workflowDef);
        testRequest.setTaskRefToMockOutput(Map.of(
                "testable-task-ref",
                List.of(new WorkflowTestRequest.TaskMock(TaskResult.Status.COMPLETED, Map.of("result", "ok")))));

        Workflow workflow = workflowClient.testWorkflow(testRequest);
        Assertions.assertEquals("ok", workflow.getOutput().get("result"));
    }

    // ==================== Pause/Resume Tests ====================

    @Test
    void testPauseWorkflow() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        assertNotNull(workflowId);

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            workflowClient.pauseWorkflow(workflowId);

            Workflow workflow = workflowClient.getWorkflow(workflowId, false);
            assertEquals(Workflow.WorkflowStatus.PAUSED, workflow.getStatus());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "test cleanup");
        }
    }

    @Test
    void testResumeWorkflow() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        assertNotNull(workflowId);

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            workflowClient.pauseWorkflow(workflowId);

            Workflow paused = workflowClient.getWorkflow(workflowId, false);
            assertEquals(Workflow.WorkflowStatus.PAUSED, paused.getStatus());

            workflowClient.resumeWorkflow(workflowId);

            Workflow resumed = workflowClient.getWorkflow(workflowId, false);
            assertEquals(Workflow.WorkflowStatus.RUNNING, resumed.getStatus());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "test cleanup");
        }
    }

    // ==================== Terminate Tests ====================

    @Test
    void testTerminateWorkflow() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        assertNotNull(workflowId);

        workflowClient.terminateWorkflow(workflowId, "test termination reason");

        Workflow workflow = workflowClient.getWorkflow(workflowId, false);
        assertEquals(Workflow.WorkflowStatus.TERMINATED, workflow.getStatus());
        assertEquals("test termination reason", workflow.getReasonForIncompletion());
    }

    @Test
    void testTerminateWorkflows() {
        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName(WAIT_WF_NAME);
            request.setVersion(1);
            request.setInput(Map.of());
            workflowIds.add(workflowClient.startWorkflow(request));
        }

        var result = workflowClient.terminateWorkflows(workflowIds, "bulk terminate test");
        assertNotNull(result);

        for (String id : workflowIds) {
            Workflow wf = workflowClient.getWorkflow(id, false);
            assertEquals(Workflow.WorkflowStatus.TERMINATED, wf.getStatus());
        }
    }

    // ==================== Delete Tests ====================

    @Test
    void testDeleteWorkflow() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        workflowClient.terminateWorkflow(workflowId, "test");

        workflowClient.deleteWorkflow(workflowId, false);

        assertThrows(Exception.class, () -> workflowClient.getWorkflow(workflowId, false));
    }

    // ==================== Restart/Retry Tests ====================

    @Test
    void testRestartWorkflow() {
        ConductorWorkflow<Object> simpleWf = new ConductorWorkflow<>(workflowExecutor);
        simpleWf.setName("test_restart_wf");
        simpleWf.setVersion(1);
        simpleWf.add(new SimpleTask(TEST_TASK_NAME, TEST_TASK_NAME));
        simpleWf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        simpleWf.setTimeoutSeconds(120);
        simpleWf.registerWorkflow(true, true);

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("test_restart_wf");
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        Workflow wf = workflowClient.getWorkflow(workflowId, true);
        if (!wf.getTasks().isEmpty()) {
            taskClient.updateTaskSync(workflowId, TEST_TASK_NAME, TaskResult.Status.COMPLETED, Map.of());
        }

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        wf = workflowClient.getWorkflow(workflowId, false);

        if (wf.getStatus().isTerminal()) {
            workflowClient.restart(workflowId, false);

            Workflow restarted = workflowClient.getWorkflow(workflowId, false);
            assertEquals(Workflow.WorkflowStatus.RUNNING, restarted.getStatus());

            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    @Test
    void testRetryLastFailedTask() {
        ConductorWorkflow<Object> simpleWf = new ConductorWorkflow<>(workflowExecutor);
        simpleWf.setName("test_retry_wf");
        simpleWf.setVersion(1);
        simpleWf.add(new SimpleTask(TEST_TASK_NAME, TEST_TASK_NAME));
        simpleWf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        simpleWf.setTimeoutSeconds(120);
        simpleWf.registerWorkflow(true, true);

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("test_retry_wf");
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            Workflow wf = workflowClient.getWorkflow(workflowId, true);

            if (!wf.getTasks().isEmpty()) {
                taskClient.updateTaskSync(workflowId, TEST_TASK_NAME, TaskResult.Status.FAILED,
                        Map.of("error", "test failure"));

                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                wf = workflowClient.getWorkflow(workflowId, false);

                if (wf.getStatus() == Workflow.WorkflowStatus.FAILED) {
                    workflowClient.retryLastFailedTask(workflowId);

                    Workflow retried = workflowClient.getWorkflow(workflowId, false);
                    assertEquals(Workflow.WorkflowStatus.RUNNING, retried.getStatus());
                }
            }
        } finally {
            try {
                workflowClient.terminateWorkflow(workflowId, "cleanup");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== Rerun Tests ====================

    @Test
    void testRerunWorkflow() {

        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        workflowClient.terminateWorkflow(workflowId, "terminating to test retrun");

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            RerunWorkflowRequest rerunRequest = new RerunWorkflowRequest();
            rerunRequest.setReRunFromWorkflowId(workflowId);

            String newWorkflowId = workflowClient.rerunWorkflow(workflowId, rerunRequest);
            assertNotNull(newWorkflowId);

            workflowClient.terminateWorkflow(newWorkflowId, "cleanup");
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // ==================== Search Tests ====================

    @Test
    void testSearchWorkflows() {
        String correlationId = "test-search-" + UUID.randomUUID();
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        request.setCorrelationId(correlationId);
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            SearchResult<WorkflowSummary> result = workflowClient.search("correlationId='" + correlationId + "'");
            assertNotNull(result);
            assertFalse(result.getResults().isEmpty());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // @Test
    // Disabled until the search v2 is rolled out
    void testSearchV2Workflows() {
        String correlationId = "test-searchv2-" + UUID.randomUUID();
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        request.setCorrelationId(correlationId);
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            SearchResult<Workflow> result = workflowClient.searchV2("correlationId='" + correlationId + "'");
            assertNotNull(result);
            assertFalse(result.getResults().isEmpty());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    @Test
    void testPaginatedSearch() {
        String correlationId = "test-paginated-" + UUID.randomUUID();
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        request.setCorrelationId(correlationId);
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            SearchResult<WorkflowSummary> result = workflowClient.search(
                    0, 10, "startTime:DESC", "*", "correlationId='" + correlationId + "'");
            assertNotNull(result);
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // @Test
    // Disabled until the search-v2 is rolledout
    void testPaginatedSearchV2() {
        String correlationId = "test-paginatedv2-" + UUID.randomUUID();
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        request.setCorrelationId(correlationId);
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);

            SearchResult<Workflow> result = workflowClient.searchV2(
                    0, 10, "startTime:DESC", "*", "correlationId='" + correlationId + "'");
            assertNotNull(result);
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    void testBulkPauseWorkflows() {
        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName(WAIT_WF_NAME);
            request.setVersion(1);
            request.setInput(Map.of());
            workflowIds.add(workflowClient.startWorkflow(request));
        }

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            var result = workflowClient.pauseWorkflows(workflowIds);
            assertNotNull(result);

            for (String id : workflowIds) {
                Workflow wf = workflowClient.getWorkflow(id, false);
                assertEquals(Workflow.WorkflowStatus.PAUSED, wf.getStatus());
            }
        } finally {
            for (String id : workflowIds) {
                try {
                    workflowClient.terminateWorkflow(id, "cleanup");
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void testBulkResumeWorkflows() {
        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName(WAIT_WF_NAME);
            request.setVersion(1);
            request.setInput(Map.of());
            workflowIds.add(workflowClient.startWorkflow(request));
        }

        try {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            workflowClient.pauseWorkflows(workflowIds);

            var result = workflowClient.resumeWorkflows(workflowIds);
            assertNotNull(result);

            for (String id : workflowIds) {
                Workflow wf = workflowClient.getWorkflow(id, false);
                assertEquals(Workflow.WorkflowStatus.RUNNING, wf.getStatus());
            }
        } finally {
            for (String id : workflowIds) {
                try {
                    workflowClient.terminateWorkflow(id, "cleanup");
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void testBulkRestartWorkflows() {
        ConductorWorkflow<Object> simpleWf = new ConductorWorkflow<>(workflowExecutor);
        simpleWf.setName("test_bulk_restart_wf");
        simpleWf.setVersion(1);
        simpleWf.add(new SimpleTask(TEST_TASK_NAME, TEST_TASK_NAME));
        simpleWf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        simpleWf.setTimeoutSeconds(120);
        simpleWf.registerWorkflow(true, true);

        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName("test_bulk_restart_wf");
            request.setVersion(1);
            request.setInput(Map.of());
            String id = workflowClient.startWorkflow(request);
            workflowIds.add(id);
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            try {
                taskClient.updateTaskSync(id, TEST_TASK_NAME, TaskResult.Status.COMPLETED, Map.of());
            } catch (Exception ignored) {
            }
        }

        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        try {
            var result = workflowClient.restartWorkflows(workflowIds, false);
            assertNotNull(result);
        } finally {
            for (String id : workflowIds) {
                try {
                    workflowClient.terminateWorkflow(id, "cleanup");
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void testBulkRetryWorkflows() {
        ConductorWorkflow<Object> simpleWf = new ConductorWorkflow<>(workflowExecutor);
        simpleWf.setName("test_bulk_retry_wf");
        simpleWf.setVersion(1);
        simpleWf.add(new SimpleTask(TEST_TASK_NAME, TEST_TASK_NAME));
        simpleWf.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.TIME_OUT_WF);
        simpleWf.setTimeoutSeconds(120);
        simpleWf.registerWorkflow(true, true);

        List<String> workflowIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            StartWorkflowRequest request = new StartWorkflowRequest();
            request.setName("test_bulk_retry_wf");
            request.setVersion(1);
            request.setInput(Map.of());
            String id = workflowClient.startWorkflow(request);
            workflowIds.add(id);
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            try {
                taskClient.updateTaskSync(id, TEST_TASK_NAME, TaskResult.Status.FAILED, Map.of());
            } catch (Exception ignored) {
            }
        }

        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

        try {
            var result = workflowClient.retryWorkflows(workflowIds);
            assertNotNull(result);
        } finally {
            for (String id : workflowIds) {
                try {
                    workflowClient.terminateWorkflow(id, "cleanup");
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== Status Summary Tests ====================

    @Test
    void testGetWorkflowStatusSummary() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        try {
            WorkflowStatus status = workflowClient.getWorkflowStatusSummary(workflowId, true, true);
            assertNotNull(status);
            assertNotNull(status.getWorkflowId());
            assertEquals(workflowId, status.getWorkflowId());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // ==================== Running Workflow Tests ====================

    @Test
    void testGetRunningWorkflow() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        try {
            List<String> running = workflowClient.getRunningWorkflow(WAIT_WF_NAME, 1);
            assertNotNull(running);
            assertTrue(running.contains(workflowId));
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    @Test
    void testGetWorkflowsByTimePeriod() {
        long startTime = System.currentTimeMillis() - 60000;
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);
        Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(2));
        long endTime = System.currentTimeMillis() + 60000;

        try {
            List<String> workflows = workflowClient.getWorkflowsByTimePeriod(WAIT_WF_NAME, 1, startTime, endTime);
            assertNotNull(workflows);
            assertTrue(workflows.contains(workflowId));
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    // ==================== Other Operations ====================

    @Test
    void testRunDecider() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        try {
            workflowClient.runDecider(workflowId);
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    @Test
    void testResetCallbacksForInProgressTasks() {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        String workflowId = workflowClient.startWorkflow(request);

        try {
            workflowClient.resetCallbacksForInProgressTasks(workflowId);
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    @Test
    void testGetWorkflowsWithCorrelationId() {
        String correlationId = "test-corr-" + UUID.randomUUID();
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WAIT_WF_NAME);
        request.setVersion(1);
        request.setInput(Map.of());
        request.setCorrelationId(correlationId);
        String workflowId = workflowClient.startWorkflow(request);

        try {
            Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

            List<Workflow> workflows = workflowClient.getWorkflows(WAIT_WF_NAME, correlationId, false, false);
            assertNotNull(workflows);
            assertFalse(workflows.isEmpty());
        } finally {
            workflowClient.terminateWorkflow(workflowId, "cleanup");
        }
    }

    StartWorkflowRequest getStartWorkflowRequest() {
        StartWorkflowRequest startWorkflowRequest = new StartWorkflowRequest();
        startWorkflowRequest.setName(Commons.WORKFLOW_NAME);
        startWorkflowRequest.setVersion(1);
        startWorkflowRequest.setInput(new HashMap<>());
        return startWorkflowRequest;
    }
}