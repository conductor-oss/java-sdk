package com.netflix.conductor.client.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.netflix.conductor.client.events.listeners.WorkflowClientListener;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.common.metadata.workflow.RerunWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.UpgradeWorkflowRequest;
import com.netflix.conductor.common.model.BulkResponse;
import com.netflix.conductor.common.run.ExternalStorageLocation;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.common.run.WorkflowTestRequest;
import com.netflix.conductor.common.utils.ExternalPayloadStorage;

public interface WorkflowClientInterface {

   // Start and Execute workflows
    String startWorkflow(StartWorkflowRequest startWorkflowRequest);
    CompletableFuture<Workflow> executeWorkflow(StartWorkflowRequest request, List<String> waitUntilTasks, Integer waitForSeconds);
    CompletableFuture<WorkflowRun> executeWorkflow(StartWorkflowRequest request, ReturnStrategy returnStrategy);
    CompletableFuture<WorkflowRun> executeWorkflow(StartWorkflowRequest request, List<String> waitUntilTaskRef, Integer waitForSeconds, Consistency consistency, ReturnStrategy returnStrategy);

    // test endpoint does not really run the workflow but uses "mock" execution and used for testing
    Workflow testWorkflow(WorkflowTestRequest testRequest);

    // Deprecate these which are just helpers
    CompletableFuture<Workflow> executeWorkflow(StartWorkflowRequest request, String waitUntilTask, Integer waitForSeconds);
    Workflow executeWorkflow(StartWorkflowRequest request, String waitUntilTask, Duration waitTimeout) throws ExecutionException, InterruptedException, TimeoutException;
    CompletableFuture<Workflow> executeWorkflow(StartWorkflowRequest request, String waitUntilTask);
    CompletableFuture<SignalResponse> executeWorkflowWithReturnStrategy(StartWorkflowRequest request);

    // Manage workflow state at runtime
    void pauseWorkflow(String workflowId);
    void resumeWorkflow(String workflowId);
    void skipTaskFromWorkflow(String workflowId, String taskReferenceName);
    String rerunWorkflow(String workflowId, RerunWorkflowRequest rerunWorkflowRequest);
    void restart(String workflowId, boolean useLatestDefinitions);
    void retryLastFailedTask(String workflowId);
    void terminateWorkflow(String workflowId, String reason);
    void deleteWorkflow(String workflowId, boolean archiveWorkflow);
    void terminateWorkflowWithFailure(String workflowId, String reason, boolean triggerFailureWorkflow);
    void upgradeRunningWorkflow(String workflowId, UpgradeWorkflowRequest upgradeWorkflowRequest);
    Workflow updateWorkflow(String workflowId, List<String> waitUntilTaskRefNames, Integer waitForSeconds, WorkflowStateUpdate updateRequest);

    //Deprecate this or delegate this to the updateWorkflow method
    Workflow updateVariables(String workflowId, Map<String, Object> variables);

    // Batch operations on workflows
    BulkResponse terminateWorkflows(List<String> workflowIds, String reason);
    BulkResponse pauseWorkflow(List<String> workflowIds);
    BulkResponse restartWorkflow(List<String> workflowIds, Boolean useLatestDefinitions);
    BulkResponse resumeWorkflow(List<String> workflowIds);
    BulkResponse retryWorkflow(List<String> workflowIds);
    BulkResponse terminateWorkflowsWithFailure(List<String> workflowIds, String reason, boolean triggerFailureWorkflow);

    // Search and Get Workflows
    Workflow getWorkflow(String workflowId, boolean includeTasks);
    Workflow.WorkflowStatus getWorkflowStatusSummary(String workflowId, Boolean includeOutput, Boolean includeVariables);

    List<Workflow> getWorkflows(String name, String correlationId, boolean includeClosed, boolean includeTasks);
    List<String> getRunningWorkflow(String workflowName, Integer version);
    List<String> getRunningWorkflow(String name, Integer version, Long startTime, Long endTime);
    List<String> getWorkflowsByTimePeriod(String workflowName, int version, Long startTime, Long endTime);
    Map<String, List<Workflow>> getWorkflowsByNamesAndCorrelationIds(List<String> correlationIds, List<String> workflowNames, Boolean includeClosed, Boolean includeTasks);

    SearchResult<WorkflowSummary> search(String query);
    SearchResult<WorkflowSummary> search(Integer start, Integer size, String sort, String freeText, String query);

    SearchResult<Workflow> searchV2(String query);
    SearchResult<Workflow> searchV2(Integer start, Integer size, String sort, String freeText, String query);

    // Helper methods
    void populateWorkflowOutput(Workflow workflow);
    void uploadCompletedWorkflows();
    void registerListener(WorkflowClientListener listener);
    void checkAndUploadToExternalStorage(StartWorkflowRequest startWorkflowRequest);
    void resetCallbacksForInProgressTasks(String workflowId);
    void runDecider(String workflowId);

}
