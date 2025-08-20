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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.workflow.IdempotencyStrategy;
import com.netflix.conductor.common.metadata.workflow.RerunWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.SkipTaskRequest;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.UpgradeWorkflowRequest;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.common.run.WorkflowTestRequest;

import io.orkes.conductor.client.enums.Consistency;
import io.orkes.conductor.client.enums.ReturnStrategy;
import io.orkes.conductor.client.model.*;

import com.fasterxml.jackson.core.type.TypeReference;


class WorkflowResource {
    private final ConductorClient client;

    WorkflowResource(ConductorClient client) {
        this.client = client;
    }

    WorkflowRun executeWorkflow(StartWorkflowRequest req,
                                String name,
                                Integer version,
                                String waitUntilTaskRef,
                                String requestId) {
        return executeWorkflow(req, name, version, waitUntilTaskRef, requestId, null);
    }

    WorkflowRun executeWorkflow(StartWorkflowRequest req,
                                String name,
                                Integer version,
                                String waitUntilTaskRef,
                                String requestId,
                                Integer waitForSeconds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/execute/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .addQueryParam("requestId", requestId)
                .addQueryParam("waitUntilTaskRef", waitUntilTaskRef)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .body(req)
                .build();

        ConductorClientResponse<WorkflowRun> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    WorkflowRun executeWorkflow(StartWorkflowRequest req,
                                String name,
                                Integer version,
                                String waitUntilTaskRef,
                                String requestId,
                                Integer waitForSeconds,
                                Consistency consistency) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/execute/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .addQueryParam("requestId", requestId)
                .addQueryParam("waitUntilTaskRef", waitUntilTaskRef)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .addQueryParam("consistency", consistency.name())
                .body(req)
                .build();

        ConductorClientResponse<WorkflowRun> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    WorkflowRun executeWorkflow(StartWorkflowRequest req,
                                String name,
                                Integer version,
                                String waitUntilTaskRef,
                                String requestId,
                                Integer waitForSeconds,
                                Consistency consistency,
                                ReturnStrategy returnStrategy) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/execute/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .addQueryParam("requestId", requestId)
                .addQueryParam("waitUntilTaskRef", waitUntilTaskRef)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .addQueryParam("consistency", consistency.name())
                .addQueryParam("returnStrategy", returnStrategy.name())
                .body(req)
                .build();

        ConductorClientResponse<WorkflowRun> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    WorkflowStatus getWorkflowStatusSummary(String workflowId, Boolean includeOutput, Boolean includeVariables) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{workflowId}/status")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("includeOutput", includeOutput)
                .addQueryParam("includeVariables", includeVariables)
                .build();

        ConductorClientResponse<WorkflowStatus> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    SearchResult<Task> getExecutionStatusTaskList(String workflowId,
                                                  Integer start,
                                                  Integer count,
                                                  List<Task.Status> status) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{workflowId}/tasks")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("start", start)
                .addQueryParam("count", count)
                .addQueryParams("status", status.stream().map(Objects::toString).toList())
                .build();

        ConductorClientResponse<SearchResult<Task>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Map<String, List<Workflow>> getWorkflowsByNamesAndCorrelationIds(CorrelationIdsSearchRequest searchRequest,
                                                                     Boolean includeClosed,
                                                                     Boolean includeTasks) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/correlated/batch")
                .addQueryParam("includeClosed", includeClosed)
                .addQueryParam("includeTasks", includeTasks)
                .body(searchRequest)
                .build();

        ConductorClientResponse<Map<String, List<Workflow>>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Map<String, List<Workflow>> getWorkflows(String name,
                                             List<String> correlationIds,
                                             Boolean includeClosed,
                                             Boolean includeTasks) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{name}/correlated")
                .addPathParam("name", name)
                .addQueryParam("includeClosed", includeClosed)
                .addQueryParam("includeTasks", includeTasks)
                .body(correlationIds)
                .build();

        ConductorClientResponse<Map<String, List<Workflow>>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<Workflow> getWorkflows(String name,
                                String correlationId,
                                Boolean includeClosed,
                                Boolean includeTasks) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{name}/correlated/{correlationId}")
                .addPathParam("name", name)
                .addPathParam("correlationId", correlationId)
                .addQueryParam("includeClosed", includeClosed)
                .addQueryParam("includeTasks", includeTasks)
                .build();

        ConductorClientResponse<List<Workflow>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void uploadCompletedWorkflows() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/document-store/upload")
                .build();

        client.execute(request);
    }

    Workflow updateVariables(String workflowId, Map<String, Object> variables) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/variables")
                .addPathParam("workflowId", workflowId)
                .body(variables)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();

    }

    void upgradeRunningWorkflow(UpgradeWorkflowRequest body, String workflowId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/upgrade")
                .addPathParam("workflowId", workflowId)
                .body(body)
                .build();

        client.execute(request);
    }

    WorkflowRun updateWorkflowState(WorkflowStateUpdate updateRequest,
                                    String requestId,
                                    String workflowId,
                                    String waitUntilTaskRef,
                                    Integer waitForSeconds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/state")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("requestId", requestId)
                .addQueryParam("waitUntilTaskRef", waitUntilTaskRef)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .body(updateRequest)
                .build();

        ConductorClientResponse<WorkflowRun> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public void terminateWithAReason(String workflowId, String reason, boolean triggerFailureWorkflow) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(ConductorClientRequest.Method.DELETE)
                .path("/workflow/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("reason", reason)
                .addQueryParam("triggerFailureWorkflow", triggerFailureWorkflow)
                .build();

        client.execute(request);
    }

    Workflow getExecutionStatus(String workflowId, Boolean includeTasks, Boolean summarize) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("includeTasks", includeTasks)
                .addQueryParam("summarize", summarize)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    SignalResponse executeWorkflowWithReturnStrategy(StartWorkflowRequest req,
                                                     String name,
                                                     Integer version,
                                                     List<String> waitUntilTaskRef,
                                                     String requestId,
                                                     Integer waitForSeconds,
                                                     Consistency consistency,
                                                     ReturnStrategy returnStrategy) {

        String waitUntilTaskRefStr = null;
        if (waitUntilTaskRef != null && !waitUntilTaskRef.isEmpty()) {
            waitUntilTaskRefStr = String.join(",", waitUntilTaskRef);
        }

        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/execute/{name}/{version}")
                .addPathParam("name", name)
                .addPathParam("version", version)
                .addQueryParam("requestId", requestId)
                .addQueryParam("waitUntilTaskRef", waitUntilTaskRefStr)
                .addQueryParam("waitForSeconds", waitForSeconds)
                .addQueryParam("consistency", consistency.name())
                .addQueryParam("returnStrategy", returnStrategy.name())
                .body(req)
                .build();

        ConductorClientResponse<SignalResponse> resp = client.execute(request, new TypeReference<SignalResponse>() {
        });

        return resp.getData();
    }

    void decide(String workflowId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/decide/{workflowId}")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    String startWorkflow(StartWorkflowRequest req) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow")
                .body(req)
                .build();

        ConductorClientResponse<String> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    List<String> getRunningWorkflow(String name, Integer version, Long startTime, Long endTime) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/running/{name}")
                .addPathParam("name", name)
                .addQueryParam("version", version)
                .addQueryParam("startTime", startTime)
                .addQueryParam("endTime", endTime)
                .build();

        ConductorClientResponse<List<String>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    SearchResult<WorkflowSummary> search(Integer start,
                                         Integer size,
                                         String sort,
                                         String freeText,
                                         String query,
                                         Boolean skipCache) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/workflow/search")
                .addQueryParam("start", start)
                .addQueryParam("size", size)
                .addQueryParam("sort", sort)
                .addQueryParam("freeText", freeText)
                .addQueryParam("query", query)
                .addQueryParam("skipCache", skipCache)
                .build();

        ConductorClientResponse<SearchResult<WorkflowSummary>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    Workflow testWorkflow(WorkflowTestRequest req) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/test")
                .body(req)
                .build();

        ConductorClientResponse<Workflow> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    void deleteWorkflow(String workflowId, Boolean archiveWorkflow) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/workflow/{workflowId}/remove")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("archiveWorkflow", archiveWorkflow)
                .build();

        client.execute(request);
    }

    String rerun(String workflowId, RerunWorkflowRequest rerunRequest) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/rerun")
                .addPathParam("workflowId", workflowId)
                .body(rerunRequest)
                .build();

        ConductorClientResponse<String> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    /**
     * Resets callback times of all non-terminal SIMPLE tasks to 0
     * @param workflowId the workflow id to reset callbacks for
     */
    void resetWorkflow(String workflowId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/resetcallbacks")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    void retryWorkflow(String workflowId, Boolean resumeSubworkflowTasks, Boolean retryIfRetriedByParent) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/retry")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("resumeSubworkflowTasks", resumeSubworkflowTasks)
                .addQueryParam("retryIfRetriedByParent", retryIfRetriedByParent)
                .build();

        client.execute(request);
    }

    void restartWorkflow(String workflowId, Boolean useLatestDefinitions) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/workflow/{workflowId}/restart")
                .addPathParam("workflowId", workflowId)
                .addQueryParam("useLatestDefinitions", useLatestDefinitions)
                .build();

        client.execute(request);
    }

    void resumeWorkflow(String workflowId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/resume")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    void pauseWorkflow(String workflowId) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/pause")
                .addPathParam("workflowId", workflowId)
                .build();

        client.execute(request);
    }

    void skipTaskFromWorkflow(String workflowId, String taskReferenceName, SkipTaskRequest requestBody) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/workflow/{workflowId}/skiptask/{taskReferenceName}")
                .addPathParam("workflowId", workflowId)
                .addPathParam("taskReferenceName", taskReferenceName)
                .body(requestBody)
                .build();

        client.execute(request);
    }

}
