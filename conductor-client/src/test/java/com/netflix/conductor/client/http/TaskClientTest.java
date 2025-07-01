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
package com.netflix.conductor.client.http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;

import com.netflix.conductor.client.config.ConductorClientConfiguration;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.enums.ReturnStrategy;
import com.netflix.conductor.common.metadata.tasks.PollData;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.model.SignalResponse;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.TaskSummary;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskClientTest {

    @Mock
    private ConductorClient mockClient;

    @Mock
    private ConductorClientConfiguration mockConfig;

    @Mock
    private ConductorClientResponse<Task> mockTaskResponse;

    @Mock
    private ConductorClientResponse<List<Task>> mockTaskListResponse;

    @Mock
    private ConductorClientResponse<Boolean> mockBooleanResponse;

    @Mock
    private ConductorClientResponse<SignalResponse> mockSignalResponse;

    @Mock
    private ConductorClientResponse<List<TaskExecLog>> mockLogResponse;

    @Mock
    private ConductorClientResponse<String> mockStringResponse;

    @Mock
    private ConductorClientResponse<Integer> mockIntegerResponse;

    @Mock
    private ConductorClientResponse<List<PollData>> mockPollDataResponse;

    @Mock
    private ConductorClientResponse<SearchResult<TaskSummary>> mockSearchResponse;

    @Mock
    private ConductorClientResponse<SearchResult<Task>> mockSearchV2Response;

    private TaskClient taskClient;
    private Task mockTask;
    private TaskResult mockTaskResult;
    private SignalResponse mockSignalResponseData;

    @BeforeEach
    void setUp() {
        when(mockConfig.isEnforceThresholds()).thenReturn(false);
        taskClient = new TaskClient(mockClient, mockConfig);
        
        mockTask = new Task();
        mockTask.setTaskId("test-task-id");
        mockTask.setTaskDefName("test-task");
        mockTask.setStatus(Task.Status.IN_PROGRESS);
        
        mockTaskResult = new TaskResult(mockTask);
        mockTaskResult.setStatus(TaskResult.Status.COMPLETED);
        
        mockSignalResponseData = new SignalResponse();
        mockSignalResponseData.setWorkflowId("test-workflow-id");
        mockSignalResponseData.setResponseType(ReturnStrategy.TARGET_WORKFLOW);
    }

    @Test
    void testPollTask() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockTaskResponse);
        when(mockTaskResponse.getData()).thenReturn(mockTask);

        Task result = taskClient.pollTask("test-task", "test-worker", "test-domain");

        assertNotNull(result);
        assertEquals("test-task-id", result.getTaskId());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testPollTaskWithBlankTaskType() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.pollTask("", "test-worker", "test-domain"));
    }

    @Test
    void testPollTaskWithBlankWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.pollTask("test-task", "", "test-domain"));
    }

    @Test
    void testBatchPollTasksByTaskType() {
        List<Task> tasks = Arrays.asList(mockTask);
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockTaskListResponse);
        when(mockTaskListResponse.getData()).thenReturn(tasks);

        List<Task> result = taskClient.batchPollTasksByTaskType("test-task", "test-worker", 5, 1000);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test-task-id", result.get(0).getTaskId());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testBatchPollTasksByTaskTypeWithInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.batchPollTasksByTaskType("test-task", "test-worker", 0, 1000));
    }

    @Test
    void testBatchPollTasksInDomain() {
        List<Task> tasks = Arrays.asList(mockTask);
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockTaskListResponse);
        when(mockTaskListResponse.getData()).thenReturn(tasks);

        List<Task> result = taskClient.batchPollTasksInDomain("test-task", "test-domain", "test-worker", 5, 1000);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testUpdateTask() {
        when(mockClient.execute(any(ConductorClientRequest.class))).thenReturn(null);

        assertDoesNotThrow(() -> taskClient.updateTask(mockTaskResult));
        verify(mockClient).execute(any(ConductorClientRequest.class));
    }

    @Test
    void testUpdateTaskWithNullTaskResult() {
        assertThrows(IllegalArgumentException.class, () -> taskClient.updateTask(null));
    }

    @Test
    void testUpdateTaskV2() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockTaskResponse);
        when(mockTaskResponse.getData()).thenReturn(mockTask);

        Task result = taskClient.updateTaskV2(mockTaskResult);

        assertNotNull(result);
        assertEquals("test-task-id", result.getTaskId());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testAck() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockBooleanResponse);
        when(mockBooleanResponse.getData()).thenReturn(true);

        Boolean result = taskClient.ack("test-task-id", "test-worker");

        assertTrue(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testAckWithBlankTaskId() {
        assertThrows(IllegalArgumentException.class, () -> taskClient.ack("", "test-worker"));
    }

    @Test
    void testLogMessageForTask() {
        when(mockClient.execute(any(ConductorClientRequest.class))).thenReturn(null);

        assertDoesNotThrow(() -> taskClient.logMessageForTask("test-task-id", "test message"));
        verify(mockClient).execute(any(ConductorClientRequest.class));
    }

    @Test
    void testLogMessageForTaskWithBlankTaskId() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.logMessageForTask("", "test message"));
    }

    @Test
    void testGetTaskLogs() {
        List<TaskExecLog> logs = Arrays.asList(new TaskExecLog());
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockLogResponse);
        when(mockLogResponse.getData()).thenReturn(logs);

        List<TaskExecLog> result = taskClient.getTaskLogs("test-task-id");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testGetTaskDetails() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockTaskResponse);
        when(mockTaskResponse.getData()).thenReturn(mockTask);

        Task result = taskClient.getTaskDetails("test-task-id");

        assertNotNull(result);
        assertEquals("test-task-id", result.getTaskId());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSignalWithDefaultReturnStrategy() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSignalResponse);
        when(mockSignalResponse.getData()).thenReturn(mockSignalResponseData);

        SignalResponse result = taskClient.signal("test-workflow-id", Task.Status.COMPLETED, Map.of("key", "value"));

        assertNotNull(result);
        assertEquals("test-workflow-id", result.getWorkflowId());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSignalWithCustomReturnStrategy() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSignalResponse);
        when(mockSignalResponse.getData()).thenReturn(mockSignalResponseData);

        SignalResponse result = taskClient.signal("test-workflow-id", Task.Status.COMPLETED, 
                Map.of("key", "value"), ReturnStrategy.BLOCKING_TASK);

        assertNotNull(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSignalWithBlankWorkflowId() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.signal("", Task.Status.COMPLETED, Map.of()));
    }

    @Test
    void testSignalWithNullStatus() {
        assertThrows(IllegalArgumentException.class, () -> 
            taskClient.signal("test-workflow-id", null, Map.of()));
    }

    @Test
    void testSignalAsync() {
        when(mockClient.execute(any(ConductorClientRequest.class))).thenReturn(null);

        assertDoesNotThrow(() -> 
            taskClient.signalAsync("test-workflow-id", Task.Status.COMPLETED, Map.of("key", "value")));
        verify(mockClient).execute(any(ConductorClientRequest.class));
    }

    @Test
    void testRemoveTaskFromQueue() {
        when(mockClient.execute(any(ConductorClientRequest.class))).thenReturn(null);

        assertDoesNotThrow(() -> taskClient.removeTaskFromQueue("test-task", "test-task-id"));
        verify(mockClient).execute(any(ConductorClientRequest.class));
    }

    @Test
    void testGetQueueSizeForTask() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockIntegerResponse);
        when(mockIntegerResponse.getData()).thenReturn(5);

        int result = taskClient.getQueueSizeForTask("test-task");

        assertEquals(5, result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testGetQueueSizeForTaskWithNullResponse() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockIntegerResponse);
        when(mockIntegerResponse.getData()).thenReturn(null);

        int result = taskClient.getQueueSizeForTask("test-task");

        assertEquals(0, result);
    }

    @Test
    void testGetQueueSizeForTaskWithParameters() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockIntegerResponse);
        when(mockIntegerResponse.getData()).thenReturn(3);

        int result = taskClient.getQueueSizeForTask("test-task", "test-domain", "test-isolation", "test-namespace");

        assertEquals(3, result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testGetPollData() {
        List<PollData> pollDataList = Arrays.asList(new PollData());
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockPollDataResponse);
        when(mockPollDataResponse.getData()).thenReturn(pollDataList);

        List<PollData> result = taskClient.getPollData("test-task");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testGetAllPollData() {
        List<PollData> pollDataList = Arrays.asList(new PollData());
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockPollDataResponse);
        when(mockPollDataResponse.getData()).thenReturn(pollDataList);

        List<PollData> result = taskClient.getAllPollData();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testRequeueAllPendingTasks() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockStringResponse);
        when(mockStringResponse.getData()).thenReturn("5 tasks requeued");

        String result = taskClient.requeueAllPendingTasks();

        assertEquals("5 tasks requeued", result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testRequeuePendingTasksByTaskType() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockStringResponse);
        when(mockStringResponse.getData()).thenReturn("3 tasks requeued");

        String result = taskClient.requeuePendingTasksByTaskType("test-task");

        assertEquals("3 tasks requeued", result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSearch() {
        SearchResult<TaskSummary> searchResult = new SearchResult<>();
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSearchResponse);
        when(mockSearchResponse.getData()).thenReturn(searchResult);

        SearchResult<TaskSummary> result = taskClient.search("test query");

        assertNotNull(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSearchV2() {
        SearchResult<Task> searchResult = new SearchResult<>();
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSearchV2Response);
        when(mockSearchV2Response.getData()).thenReturn(searchResult);

        SearchResult<Task> result = taskClient.searchV2("test query");

        assertNotNull(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSearchWithPagination() {
        SearchResult<TaskSummary> searchResult = new SearchResult<>();
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSearchResponse);
        when(mockSearchResponse.getData()).thenReturn(searchResult);

        SearchResult<TaskSummary> result = taskClient.search(0, 10, "createdTime:DESC", "free text", "test query");

        assertNotNull(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testSearchV2WithPagination() {
        SearchResult<Task> searchResult = new SearchResult<>();
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenReturn(mockSearchV2Response);
        when(mockSearchV2Response.getData()).thenReturn(searchResult);

        SearchResult<Task> result = taskClient.searchV2(0, 10, "createdTime:DESC", "free text", "test query");

        assertNotNull(result);
        verify(mockClient).execute(any(ConductorClientRequest.class), any(TypeReference.class));
    }

    @Test
    void testClientExceptionHandling() {
        when(mockClient.execute(any(ConductorClientRequest.class), any(TypeReference.class)))
                .thenThrow(new ConductorClientException("Test exception"));

        assertThrows(ConductorClientException.class, () -> 
            taskClient.pollTask("test-task", "test-worker", null));
    }
}
