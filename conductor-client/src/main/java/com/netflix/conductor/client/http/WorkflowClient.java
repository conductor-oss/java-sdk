/*
 * Copyright 2021 Conductor Authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.conductoross.conductor.common.model.Consistency;
import org.conductoross.conductor.common.model.CorrelationIdsSearchRequest;
import org.conductoross.conductor.common.model.ReturnStrategy;
import org.conductoross.conductor.common.model.SignalResponse;
import org.conductoross.conductor.common.model.WorkflowRun;
import org.conductoross.conductor.common.model.WorkflowStateUpdate;
import org.conductoross.conductor.common.model.WorkflowStatus;

import com.netflix.conductor.client.config.ConductorClientConfiguration;
import com.netflix.conductor.client.config.DefaultConductorClientConfiguration;
import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.listeners.ListenerRegister;
import com.netflix.conductor.client.events.listeners.WorkflowClientListener;
import com.netflix.conductor.client.events.workflow.WorkflowClientEvent;
import com.netflix.conductor.client.events.workflow.WorkflowInputPayloadSizeEvent;
import com.netflix.conductor.client.events.workflow.WorkflowPayloadUsedEvent;
import com.netflix.conductor.client.events.workflow.WorkflowStartedEvent;
import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.common.config.ObjectMapperProvider;
import com.netflix.conductor.common.metadata.workflow.RerunWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.SkipTaskRequest;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.UpgradeWorkflowRequest;
import com.netflix.conductor.common.model.BulkResponse;
import com.netflix.conductor.common.run.ExternalStorageLocation;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.common.run.WorkflowTestRequest;
import com.netflix.conductor.common.utils.ExternalPayloadStorage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowClient implements AutoCloseable {

    // Static TypeReference instances for performance optimization - avoid creating
    // new instances per request
    private static final TypeReference<String> STRING_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Workflow> WORKFLOW_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Workflow>> WORKFLOW_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<BulkResponse> BULK_RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<SearchResult<Workflow>> SEARCH_RESULT_WORKFLOW_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<SearchResult<WorkflowSummary>> SEARCH_RESULT_WORKFLOW_SUMMARY_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<WorkflowStatus> WORKFLOW_STATUS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, List<Workflow>>> CORRELATION_WORKFLOWS_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<WorkflowRun> WORKFLOW_RUN_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<SignalResponse> SIGNAL_RESPONSE_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapperProvider().getObjectMapper();

    private final ConductorClientConfiguration conductorClientConfiguration;

    private final EventDispatcher<WorkflowClientEvent> eventDispatcher = new EventDispatcher<>();

    protected ConductorClient client;

    private PayloadStorage payloadStorage;

    private final ExecutorService executorService;

    /** Creates a default workflow client */
    public WorkflowClient() {
        // client will be set once root uri is set
        this(null, new DefaultConductorClientConfiguration(), 0);
    }

    public WorkflowClient(ConductorClient client) {
        this(client, new DefaultConductorClientConfiguration(), 0);
    }

    public WorkflowClient(ConductorClient client, ConductorClientConfiguration config) {
        this(client, config, 0);
    }

    public WorkflowClient(ConductorClient client, int executorThreadCount) {
        this(client, new DefaultConductorClientConfiguration(), executorThreadCount);
    }

    public WorkflowClient(ConductorClient client, ConductorClientConfiguration config, int executorThreadCount) {
        this.client = client;
        this.payloadStorage = new PayloadStorage(client);
        this.conductorClientConfiguration = config;

        ThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("WorkflowClient Executor %d")
                .build();

        if (executorThreadCount < 1) {
            this.executorService = Executors.newCachedThreadPool(factory);
        } else {
            this.executorService = Executors.newFixedThreadPool(executorThreadCount, factory);
        }
    }

    @Override
    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Kept only for backwards compatibility
     *
     * @param rootUri basePath for the ApiClient
     */
    @Deprecated
    public void setRootURI(String rootUri) {
        if (client != null) {
            client.shutdown();
        }
        client = new ConductorClient(rootUri);
        payloadStorage = new PayloadStorage(client);
    }

    public void registerListener(WorkflowClientListener listener) {
        ListenerRegister.register(listener, eventDispatcher);
    }

    /**
     * Starts a workflow. If the size of the workflow input payload is bigger than
     * {@link
     * ExternalPayloadStorage}, if enabled, else the workflow is rejected.
     *
     * @param startWorkflowRequest the {@link StartWorkflowRequest} object to start
     *                             the workflow
     * @return the id of the workflow instance that can be used for tracking
     */
    public String startWorkflow(StartWorkflowRequest startWorkflowRequest) {
        Validate.notNull(startWorkflowRequest, "StartWorkflowRequest cannot be null");
        Validate.notBlank(startWorkflowRequest.getName(), "Workflow name cannot be null or empty");
        Validate.isTrue(
                StringUtils.isBlank(startWorkflowRequest.getExternalInputPayloadStoragePath()),
                "External Storage Path must not be set");

        if (conductorClientConfiguration.isEnforceThresholds()) {
            checkAndUploadToExternalStorage(startWorkflowRequest);
        }

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow")
                .body(startWorkflowRequest)
                .build();

        ConductorClientResponse<String> resp = client.execute(request, STRING_TYPE);

        eventDispatcher
                .publish(new WorkflowStartedEvent(startWorkflowRequest.getName(), startWorkflowRequest.getVersion()));
        return resp.getData();
    }

    public void checkAndUploadToExternalStorage(StartWorkflowRequest startWorkflowRequest) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            objectMapper.writeValue(byteArrayOutputStream, startWorkflowRequest.getInput());
            byte[] workflowInputBytes = byteArrayOutputStream.toByteArray();
            long workflowInputSize = workflowInputBytes.length;
            eventDispatcher.publish(new WorkflowInputPayloadSizeEvent(startWorkflowRequest.getName(),
                    startWorkflowRequest.getVersion(), workflowInputSize));

            if (workflowInputSize > conductorClientConfiguration.getWorkflowInputPayloadThresholdKB() * 1024L) {
                if (!conductorClientConfiguration.isExternalPayloadStorageEnabled() ||
                        (workflowInputSize > conductorClientConfiguration.getWorkflowInputMaxPayloadThresholdKB()
                                * 1024L)) {
                    String errorMsg = String.format("Input payload larger than the allowed threshold of: %d KB",
                            conductorClientConfiguration.getWorkflowInputPayloadThresholdKB());
                    throw new ConductorClientException(errorMsg);
                } else {
                    eventDispatcher.publish(new WorkflowPayloadUsedEvent(startWorkflowRequest.getName(),
                            startWorkflowRequest.getVersion(),
                            ExternalPayloadStorage.Operation.WRITE.name(),
                            ExternalPayloadStorage.PayloadType.WORKFLOW_INPUT.name()));

                    String externalStoragePath = uploadToExternalPayloadStorage(
                            workflowInputBytes,
                            workflowInputSize);
                    startWorkflowRequest.setExternalInputPayloadStoragePath(externalStoragePath);
                    startWorkflowRequest.setInput(null);
                }
            }
        } catch (IOException e) {
            String errorMsg = String.format("Unable to start workflow:%s, version:%s",
                    startWorkflowRequest.getName(), startWorkflowRequest.getVersion());
            log.error(errorMsg, e);

            eventDispatcher.publish(new WorkflowStartedEvent(startWorkflowRequest.getName(),
                    startWorkflowRequest.getVersion(), false, e));

            throw new ConductorClientException(e);
        }
    }

    private String uploadToExternalPayloadStorage(byte[] payloadBytes, long payloadSize) {
        ExternalStorageLocation externalStorageLocation = payloadStorage.getLocation(
                ExternalPayloadStorage.Operation.WRITE, ExternalPayloadStorage.PayloadType.WORKFLOW_INPUT, "");
        payloadStorage.upload(
                externalStorageLocation.getUri(),
                new ByteArrayInputStream(payloadBytes),
                payloadSize);
        return externalStorageLocation.getPath();
    }

    /**
     * Retrieve a workflow by workflow id
     *
     * @param workflowId   the id of the workflow
     * @param includeTasks specify if the tasks in the workflow need to be returned
     * @return the requested workflow
     */
    public Workflow getWorkflow(String workflowId, boolean includeTasks) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("includeTasks", includeTasks)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, WORKFLOW_TYPE);

        Workflow workflow = resp.getData();
        populateWorkflowOutput(workflow);
        return workflow;
    }

    /**
     * Retrieve all workflows for a given correlation id and name
     *
     * @param name          the name of the workflow
     * @param correlationId the correlation id
     * @param includeClosed specify if all workflows are to be returned or only
     *                      running workflows
     * @param includeTasks  specify if the tasks in the workflow need to be returned
     * @return list of workflows for the given correlation id and name
     */
    public List<Workflow> getWorkflows(String name, String correlationId, boolean includeClosed, boolean includeTasks) {
        Validate.notBlank(name, "name cannot be blank");
        Validate.notBlank(correlationId, "correlationId cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{name}/correlated/{correlationId}")
                .addPathParam("name", name)
                .addPathParam("correlationId", correlationId)
                .addQueryParam("includeClosed", includeClosed)
                .addQueryParam("includeTasks", includeTasks)
                .build();

        ConductorClientResponse<List<Workflow>> resp = client.execute(request, WORKFLOW_LIST_TYPE);

        List<Workflow> workflows = resp.getData();
        workflows.forEach(this::populateWorkflowOutput);
        return workflows;
    }

    /**
     * Removes a workflow from the system
     *
     * @param workflowId      the id of the workflow to be deleted
     * @param archiveWorkflow flag to indicate if the workflow should be archived
     *                        before deletion
     */
    public void deleteWorkflow(String workflowId, boolean archiveWorkflow) {
        Validate.notBlank(workflowId, "Workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/workflow/{workflowId}/remove")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("archiveWorkflow", archiveWorkflow)
                .build();

        client.execute(request);
    }

    /**
     * Terminates the execution of all given workflows instances
     *
     * @param workflowIds the ids of the workflows to be terminated
     * @param reason      the reason to be logged and displayed
     * @return the {@link BulkResponse} contains bulkErrorResults and
     *         bulkSuccessfulResults
     */
    public BulkResponse terminateWorkflows(List<String> workflowIds, String reason) {
        Validate.isTrue(!workflowIds.isEmpty(), "workflow id cannot be blank");

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/bulk/terminate")
                .addQueryParam("reason", reason)
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    /**
     * Retrieve all running workflow instances for a given name and version
     *
     * @param workflowName the name of the workflow
     * @param version      the version of the wokflow definition. Defaults to 1.
     * @return the list of running workflow instances
     */
    public List<String> getRunningWorkflow(String workflowName, Integer version) {
        return getRunningWorkflow(workflowName, version, null, null);
    }

    /**
     * Retrieve all workflow instances for a given workflow name between a specific
     * time period
     *
     * @param workflowName the name of the workflow
     * @param version      the version of the workflow definition. Defaults to 1.
     * @param startTime    the start time of the period
     * @param endTime      the end time of the period
     * @return returns a list of workflows created during the specified during the
     *         time period
     */
    public List<String> getWorkflowsByTimePeriod(String workflowName, int version, Long startTime, Long endTime) {
        Validate.notBlank(workflowName, "Workflow name cannot be blank");
        Validate.notNull(startTime, "Start time cannot be null");
        Validate.notNull(endTime, "End time cannot be null");

        return getRunningWorkflow(workflowName, version, startTime, endTime);
    }

    /**
     * Starts the decision task for the given workflow instance
     *
     * @param workflowId the id of the workflow instance
     */
    public void runDecider(String workflowId) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/decide/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    /**
     * Pause a workflow by workflow id
     *
     * @param workflowId the workflow id of the workflow to be paused
     */
    public void pauseWorkflow(String workflowId) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/pause")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    /**
     * Resume a paused workflow by workflow id
     *
     * @param workflowId the workflow id of the paused workflow
     */
    public void resumeWorkflow(String workflowId) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/resume")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    /**
     * Skips a given task from a current RUNNING workflow
     *
     * @param workflowId        the id of the workflow instance
     * @param taskReferenceName the reference name of the task to be skipped
     */
    public void skipTaskFromWorkflow(String workflowId, String taskReferenceName) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        Validate.notBlank(taskReferenceName, "Task reference name cannot be blank");

        // FIXME skipTaskRequest content is always empty
        SkipTaskRequest skipTaskRequest = new SkipTaskRequest();
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/skiptask/{taskReferenceName}")
                .addPathParam("workflowId", workflowId)
                .addPathParam("taskReferenceName", taskReferenceName)
                .body(skipTaskRequest) // FIXME review this. It was passed as a query param?!
                .build();

        client.execute(request);
    }

    /**
     * Reruns the workflow from a specific task
     *
     * @param workflowId           the id of the workflow
     * @param rerunWorkflowRequest the request containing the task to rerun from
     * @return the id of the workflow
     */
    public String rerunWorkflow(String workflowId, RerunWorkflowRequest rerunWorkflowRequest) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        Validate.notNull(rerunWorkflowRequest, "RerunWorkflowRequest cannot be null");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/rerun")
                .addPathParam("workflowId", workflowId)
                .body(rerunWorkflowRequest)
                .build();

        ConductorClientResponse<String> resp = client.execute(request, STRING_TYPE);

        return resp.getData();
    }

    /**
     * Restart a completed workflow
     *
     * @param workflowId           the workflow id of the workflow to be restarted
     * @param useLatestDefinitions if true, use the latest workflow and task
     *                             definitions when
     *                             restarting the workflow if false, use the
     *                             workflow and task definitions embedded in the
     *                             workflow execution when restarting the workflow
     */
    public void restart(String workflowId, boolean useLatestDefinitions) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/restart")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("useLatestDefinitions", useLatestDefinitions)
                .build();

        client.execute(request);
    }

    /**
     * Retries the last failed task in a workflow
     *
     * @param workflowId the workflow id of the workflow with the failed task
     */
    public void retryLastFailedTask(String workflowId) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/retry")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    /**
     * Resets the callback times of all IN PROGRESS tasks to 0 for the given
     * workflow
     *
     * @param workflowId the id of the workflow
     */
    public void resetCallbacksForInProgressTasks(String workflowId) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/resetcallbacks")
                .addPathParam("workflowId", workflowId)
                .build();
        client.execute(request);
    }

    /**
     * Terminates the execution of the given workflow instance
     *
     * @param workflowId the id of the workflow to be terminated
     * @param reason     the reason to be logged and displayed
     */
    public void terminateWorkflow(String workflowId, String reason) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/workflow/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("reason", reason)
                .build();

        client.execute(request);
    }

    /**
     * Terminates the execution of the given workflow instance with failure workflow
     * trigger option
     *
     * @param workflowId             the id of the workflow to be terminated
     * @param reason                 the reason to be logged and displayed
     * @param triggerFailureWorkflow whether to trigger the failure workflow
     */
    public void terminateWorkflowWithFailure(String workflowId, String reason, boolean triggerFailureWorkflow) {
        Validate.notBlank(workflowId, "workflow id cannot be blank");
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/workflow/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("reason", reason)
                .addQueryParam("triggerFailureWorkflow", triggerFailureWorkflow)
                .build();

        client.execute(request);
    }

    /**
     * Search for workflows based on payload
     *
     * @param query the search query
     * @return the {@link SearchResult} containing the {@link WorkflowSummary} that
     *         match the query
     */
    public SearchResult<WorkflowSummary> search(String query) {
        return search(null, null, null, "", query);
    }

    /**
     * Search for workflows based on payload
     *
     * @param query the search query
     * @return the {@link SearchResult} containing the {@link Workflow} that match
     *         the query
     */
    public SearchResult<Workflow> searchV2(String query) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/search-v2")
                .addQueryParam("query", query)
                .build();

        ConductorClientResponse<SearchResult<Workflow>> resp = client.execute(request, SEARCH_RESULT_WORKFLOW_TYPE);

        return resp.getData();
    }

    /**
     * Paginated search for workflows based on payload
     *
     * @param start    start value of page
     * @param size     number of workflows to be returned
     * @param sort     sort order
     * @param freeText additional free text query
     * @param query    the search query
     * @return the {@link SearchResult} containing the {@link WorkflowSummary} that
     *         match the query
     */
    public SearchResult<WorkflowSummary> search(
            Integer start, Integer size, String sort, String freeText, String query) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/search")
                .addQueryParam("start", start)
                .addQueryParam("size", size)
                .addQueryParam("sort", sort)
                .addQueryParam("freeText", freeText)
                .addQueryParam("query", query)
                .build();

        ConductorClientResponse<SearchResult<WorkflowSummary>> resp = client.execute(request,
                SEARCH_RESULT_WORKFLOW_SUMMARY_TYPE);

        return resp.getData();
    }

    /**
     * Paginated search for workflows based on payload
     *
     * @param start    start value of page
     * @param size     number of workflows to be returned
     * @param sort     sort order
     * @param freeText additional free text query
     * @param query    the search query
     * @return the {@link SearchResult} containing the {@link Workflow} that match
     *         the query
     */
    public SearchResult<Workflow> searchV2(Integer start, Integer size, String sort, String freeText, String query) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/search-v2")
                .addQueryParam("start", start)
                .addQueryParam("size", size)
                .addQueryParam("sort", sort)
                .addQueryParam("freeText", freeText)
                .addQueryParam("query", query)
                .build();

        ConductorClientResponse<SearchResult<Workflow>> resp = client.execute(request, SEARCH_RESULT_WORKFLOW_TYPE);

        return resp.getData();
    }

    public Workflow testWorkflow(WorkflowTestRequest testRequest) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/test")
                .body(testRequest)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, WORKFLOW_TYPE);

        return resp.getData();
    }

    /**
     * Populates the workflow output from external payload storage if the external
     * storage path is
     * specified.
     *
     * @param workflow the workflow for which the output is to be populated.
     */
    public void populateWorkflowOutput(Workflow workflow) {
        if (StringUtils.isNotBlank(workflow.getExternalOutputPayloadStoragePath())) {
            eventDispatcher.publish(new WorkflowPayloadUsedEvent(workflow.getWorkflowName(),
                    workflow.getWorkflowVersion(),
                    ExternalPayloadStorage.Operation.READ.name(),
                    ExternalPayloadStorage.PayloadType.WORKFLOW_OUTPUT.name()));
            workflow.setOutput(downloadFromExternalStorage(workflow.getExternalOutputPayloadStoragePath()));
        }
    }

    // ========== Bulk Operations ==========

    /**
     * Pause multiple workflows in bulk
     *
     * @param workflowIds the list of workflow ids to pause
     * @return BulkResponse with results
     */
    public BulkResponse pauseWorkflows(List<String> workflowIds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/bulk/pause")
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    /**
     * Restart multiple workflows in bulk
     *
     * @param workflowIds          the list of workflow ids to restart
     * @param useLatestDefinitions whether to use latest workflow definitions
     * @return BulkResponse with results
     */
    public BulkResponse restartWorkflow(List<String> workflowIds, Boolean useLatestDefinitions) {
        return restartWorkflows(workflowIds, useLatestDefinitions);
    }

    /**
     * Restart multiple workflows in bulk
     *
     * @param workflowIds          the list of workflow ids to restart
     * @param useLatestDefinitions whether to use latest workflow definitions
     * @return BulkResponse with results
     */
    public BulkResponse restartWorkflows(List<String> workflowIds, Boolean useLatestDefinitions) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/bulk/restart")
                .addQueryParam("useLatestDefinitions", useLatestDefinitions)
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    /**
     * Resume multiple workflows in bulk
     *
     * @param workflowIds the list of workflow ids to resume
     * @return BulkResponse with results
     */
    public BulkResponse resumeWorkflow(List<String> workflowIds) {
        return resumeWorkflows(workflowIds);
    }

    /**
     * Resume multiple workflows in bulk
     *
     * @param workflowIds the list of workflow ids to resume
     * @return BulkResponse with results
     */
    public BulkResponse resumeWorkflows(List<String> workflowIds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/bulk/resume")
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    /**
     * Retry multiple workflows in bulk
     *
     * @param workflowIds the list of workflow ids to retry
     * @return BulkResponse with results
     */
    public BulkResponse retryWorkflow(List<String> workflowIds) {
        return retryWorkflows(workflowIds);
    }

    /**
     * Retry multiple workflows in bulk
     *
     * @param workflowIds the list of workflow ids to retry
     * @return BulkResponse with results
     */
    public BulkResponse retryWorkflows(List<String> workflowIds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/bulk/retry")
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    /**
     * Terminate multiple workflows with failure workflow trigger option
     *
     * @param workflowIds            the list of workflow ids to terminate
     * @param reason                 the reason for termination
     * @param triggerFailureWorkflow whether to trigger failure workflow
     * @return BulkResponse with results
     */
    public BulkResponse terminateWorkflowsWithFailure(List<String> workflowIds, String reason,
            boolean triggerFailureWorkflow) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/bulk/terminate")
                .addQueryParam("reason", reason)
                .addQueryParam("triggerFailureWorkflow", triggerFailureWorkflow)
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse> resp = client.execute(request, BULK_RESPONSE_TYPE);

        return resp.getData();
    }

    // ========== Extended Workflow Operations ==========

    /**
     * Get workflow status summary
     *
     * @param workflowId       the workflow id
     * @param includeOutput    whether to include workflow output
     * @param includeVariables whether to include workflow variables
     * @return WorkflowStatus summary
     */
    public WorkflowStatus getWorkflowStatusSummary(String workflowId, Boolean includeOutput, Boolean includeVariables) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{workflowId}/status")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("includeOutput", includeOutput)
                .addQueryParam("includeVariables", includeVariables)
                .build();

        ConductorClientResponse<WorkflowStatus> resp = client.execute(request, WORKFLOW_STATUS_TYPE);

        return resp.getData();
    }

    /**
     * Get workflows by names and correlation IDs
     *
     * @param correlationIds list of correlation IDs
     * @param workflowNames  list of workflow names
     * @param includeClosed  whether to include closed workflows
     * @param includeTasks   whether to include tasks
     * @return Map of correlation ID to list of workflows
     */
    public Map<String, List<Workflow>> getWorkflowsByNamesAndCorrelationIds(
            List<String> correlationIds, List<String> workflowNames, Boolean includeClosed, Boolean includeTasks) {
        CorrelationIdsSearchRequest searchRequest = new CorrelationIdsSearchRequest(correlationIds, workflowNames);
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/correlated/batch")
                .addQueryParam("includeClosed", includeClosed)
                .addQueryParam("includeTasks", includeTasks)
                .body(searchRequest)
                .build();

        ConductorClientResponse<Map<String, List<Workflow>>> resp = client.execute(request,
                CORRELATION_WORKFLOWS_MAP_TYPE);

        return resp.getData();
    }

    /**
     * Upload completed workflows to document store
     */
    public void uploadCompletedWorkflows() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/document-store/upload")
                .build();

        client.execute(request);
    }

    /**
     * Update workflow variables
     *
     * @param workflowId the workflow id
     * @param variables  the variables to update
     * @return the updated workflow
     */
    public Workflow updateVariables(String workflowId, Map<String, Object> variables) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/variables")
                .addPathParam("workflowId", workflowId)
                .body(variables)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, WORKFLOW_TYPE);

        return resp.getData();
    }

    /**
     * Upgrade a running workflow to a new version
     *
     * @param workflowId             the workflow id
     * @param upgradeWorkflowRequest the upgrade request
     */
    public void upgradeRunningWorkflow(String workflowId, UpgradeWorkflowRequest upgradeWorkflowRequest) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/upgrade")
                .addPathParam("workflowId", workflowId)
                .body(upgradeWorkflowRequest)
                .build();

        client.execute(request);
    }

    /**
     * Update workflow state
     *
     * @param workflowId            the workflow id
     * @param waitUntilTaskRefNames list of task reference names to wait for
     * @param waitForSeconds        maximum time to wait in seconds
     * @param updateRequest         the state update request
     * @return WorkflowRun with updated state
     */
    public WorkflowRun updateWorkflow(String workflowId, List<String> waitUntilTaskRefNames, Integer waitForSeconds,
            WorkflowStateUpdate updateRequest) {
        String joinedReferenceNames = "";
        if (waitUntilTaskRefNames != null) {
            joinedReferenceNames = String.join(",", waitUntilTaskRefNames);
        }

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/state")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("requestId", UUID.randomUUID().toString())
                .addQueryParam("waitUntilTaskRef", joinedReferenceNames)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .body(updateRequest)
                .build();

        ConductorClientResponse<WorkflowRun> resp = client.execute(request, WORKFLOW_RUN_TYPE);

        return resp.getData();
    }

    // ========== Async Workflow Execution ==========

    /**
     * Synchronously executes a workflow
     *
     * @param request       workflow execution request
     * @param waitUntilTask waits until workflow has reached this task
     * @return CompletableFuture with WorkflowRun
     */
    @Deprecated
    public CompletableFuture<WorkflowRun> executeWorkflow(StartWorkflowRequest request, String waitUntilTask) {
        return executeWorkflowAsync(request, waitUntilTask, null);
    }

    /**
     * Synchronously executes a workflow
     *
     * @param request        workflow execution request
     * @param waitUntilTask  waits until workflow has reached this task
     * @param waitForSeconds maximum amount of time to wait before returning
     * @return CompletableFuture with WorkflowRun
     */
    public CompletableFuture<WorkflowRun> executeWorkflow(StartWorkflowRequest request, String waitUntilTask,
            Integer waitForSeconds) {
        return executeWorkflowAsync(request, waitUntilTask, waitForSeconds);
    }

    /**
     * Synchronously executes a workflow
     *
     * @param request        workflow execution request
     * @param waitUntilTasks waits until workflow has reached one of these tasks
     * @param waitForSeconds maximum amount of time to wait before returning
     * @return CompletableFuture with WorkflowRun
     */
    public CompletableFuture<WorkflowRun> executeWorkflow(StartWorkflowRequest request, List<String> waitUntilTasks,
            Integer waitForSeconds) {
        String waitUntilTask = String.join(",", waitUntilTasks);
        return executeWorkflowAsync(request, waitUntilTask, waitForSeconds);
    }

    /**
     * Synchronously executes a workflow with timeout
     *
     * @param request       workflow execution request
     * @param waitUntilTask waits until workflow has reached this task
     * @param waitTimeout   maximum amount of time to wait
     * @return WorkflowRun
     */
    public WorkflowRun executeWorkflow(StartWorkflowRequest request, String waitUntilTask, Duration waitTimeout)
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<WorkflowRun> future = executeWorkflow(request, waitUntilTask);
        return future.get(waitTimeout.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    }

    /**
     * Executes a workflow with return strategy - new unified API
     *
     * @param request workflow execution request
     * @return SignalResponse with target workflow details (default strategy)
     */
    public CompletableFuture<SignalResponse> executeWorkflowWithReturnStrategy(StartWorkflowRequest request) {
        return executeWorkflowWithReturnStrategy(request, null, 10, Consistency.SYNCHRONOUS,
                ReturnStrategy.TARGET_WORKFLOW);
    }

    /**
     * Executes a workflow with specified return strategy
     *
     * @param request        workflow execution request
     * @param returnStrategy strategy for what data to return
     * @return SignalResponse based on the return strategy
     */
    public CompletableFuture<SignalResponse> executeWorkflowWithReturnStrategy(StartWorkflowRequest request,
            ReturnStrategy returnStrategy) {
        return executeWorkflowWithReturnStrategy(request, null, 10, Consistency.SYNCHRONOUS, returnStrategy);
    }

    /**
     * Executes a workflow with full control over execution parameters
     *
     * @param request          workflow execution request
     * @param waitUntilTaskRef reference name of the task to wait for
     * @param waitForSeconds   maximum time to wait in seconds
     * @param consistency      execution consistency mode
     * @param returnStrategy   strategy for what data to return
     * @return SignalResponse based on the return strategy
     */
    public CompletableFuture<SignalResponse> executeWorkflowWithReturnStrategy(
            StartWorkflowRequest request,
            List<String> waitUntilTaskRef,
            Integer waitForSeconds,
            Consistency consistency,
            ReturnStrategy returnStrategy) {

        CompletableFuture<SignalResponse> future = new CompletableFuture<>();
        String requestId = UUID.randomUUID().toString();

        executorService.submit(() -> {
            try {
                String waitUntilTaskRefStr = null;
                if (waitUntilTaskRef != null && !waitUntilTaskRef.isEmpty()) {
                    waitUntilTaskRefStr = String.join(",", waitUntilTaskRef);
                }

                ConductorClientRequest httpRequest = ConductorClientRequest.builder()
                        .method(Method.POST)
                        .path("/workflow/execute/{name}/{version}")
                        .addPathParam("name", request.getName())
                        .addPathParam("version", request.getVersion())
                        .addQueryParam("requestId", requestId)
                        .addQueryParam("waitUntilTaskRef", waitUntilTaskRefStr)
                        .addQueryParam("waitForSeconds", waitForSeconds)
                        .addQueryParam("consistency", consistency.name())
                        .addQueryParam("returnStrategy", returnStrategy.name())
                        .body(request)
                        .build();

                ConductorClientResponse<SignalResponse> resp = client.execute(httpRequest,
                        SIGNAL_RESPONSE_TYPE);

                future.complete(resp.getData());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    // ========== Private Methods ==========

    @SuppressWarnings("unchecked")
    private Map<String, Object> downloadFromExternalStorage(String path) {
        Validate.notBlank(path, "uri cannot be blank");
        ExternalStorageLocation externalStorageLocation = payloadStorage.getLocation(
                ExternalPayloadStorage.Operation.READ,
                ExternalPayloadStorage.PayloadType.WORKFLOW_OUTPUT, path);
        try (InputStream inputStream = payloadStorage.download(externalStorageLocation.getUri())) {
            return objectMapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            String errorMsg = String.format("Unable to download payload from external storage location: %s", path);
            log.error(errorMsg, e);
            throw new ConductorClientException(e);
        }
    }

    private List<String> getRunningWorkflow(String name, Integer version, Long startTime, Long endTime) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/running/{name}")
                .addPathParam("name", name)
                .addQueryParam("version", version)
                .addQueryParam("startTime", startTime)
                .addQueryParam("endTime", endTime)
                .build();

        ConductorClientResponse<List<String>> resp = client.execute(request, STRING_LIST_TYPE);

        return resp.getData();
    }

    private CompletableFuture<WorkflowRun> executeWorkflowAsync(StartWorkflowRequest startWorkflowRequest,
            String waitUntilTask, Integer waitForSeconds) {
        CompletableFuture<WorkflowRun> future = new CompletableFuture<>();
        String requestId = UUID.randomUUID().toString();
        executorService.submit(() -> {
            try {
                ConductorClientRequest request = ConductorClientRequest.builder()
                        .method(Method.POST)
                        .path("/workflow/execute/{name}/{version}")
                        .addPathParam("name", startWorkflowRequest.getName())
                        .addPathParam("version", startWorkflowRequest.getVersion())
                        .addQueryParam("requestId", requestId)
                        .addQueryParam("waitUntilTaskRef", waitUntilTask)
                        .addQueryParam("waitForSeconds", waitForSeconds)
                        .body(startWorkflowRequest)
                        .build();

                ConductorClientResponse<WorkflowRun> resp = client.execute(request, WORKFLOW_RUN_TYPE);

                future.complete(resp.getData());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }
}
