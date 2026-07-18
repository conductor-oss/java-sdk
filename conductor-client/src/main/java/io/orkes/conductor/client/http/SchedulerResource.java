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

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;

import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.SearchResultWorkflowScheduleExecution;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.WorkflowSchedule;

import com.fasterxml.jackson.core.type.TypeReference;


public class SchedulerResource {

    private final ConductorClient client;

    /**
     * Once a server answers {@code 405} to a PUT pause/resume request (OSS Conductor's original
     * verb; Orkes Conductor historically accepted only {@code GET}), fall back to {@code GET} for
     * the remaining lifetime of this instance rather than probing on every call.
     */
    private volatile boolean useGetForScheduleStateChange = false;

    public SchedulerResource(ConductorClient client) {
        this.client = client;
    }

    public void deleteSchedule(String name) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/scheduler/schedules/{name}")
                .addPathParam("name", name)
                .build();

        client.execute(request);
    }

    public List<WorkflowSchedule> getAllSchedules(String workflowName) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/schedules")
                .addQueryParam("workflowName", workflowName)
                .build();

        ConductorClientResponse<List<WorkflowSchedule>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public List<Long> getNextFewSchedules(String cronExpression,
                                          Long scheduleStartTime,
                                          Long scheduleEndTime,
                                          Integer limit) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/nextFewSchedules")
                .addQueryParam("cronExpression", cronExpression)
                .addQueryParam("scheduleStartTime", scheduleStartTime)
                .addQueryParam("scheduleEndTime", scheduleEndTime)
                .addQueryParam("limit", limit)
                .build();

        ConductorClientResponse<List<Long>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public WorkflowSchedule getSchedule(String name) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/schedules/{name}")
                .addPathParam("name", name)
                .build();

        ConductorClientResponse<WorkflowSchedule> resp = client.execute(request, new TypeReference<>() {
        });

        WorkflowSchedule schedule = resp.getData();
        if (schedule == null || schedule.getName() == null) {
            // OSS Conductor returns 200 with an empty/null body on a miss (Orkes returns 404,
            // which already surfaces as a ConductorClientException below); normalize both server
            // families to the same not-found contract.
            throw new ConductorClientException(404, "Schedule '" + name + "' not found");
        }
        return schedule;
    }

    public Map<String, Object> pauseAllSchedules() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/admin/pause")
                .build();

        ConductorClientResponse<Map<String, Object>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public void pauseSchedule(String name) {
        pauseSchedule(name, null);
    }

    /**
     * Pauses a schedule, recording an optional reason (persisted as {@code pausedReason} where
     * the server supports it; silently ignored otherwise — e.g. Orkes Conductor today).
     */
    public void pauseSchedule(String name, String reason) {
        executeStateChange("/scheduler/schedules/{name}/pause", name, reason);
    }

    public Map<String, Object> requeueAllExecutionRecords() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/admin/requeue")
                .build();

        ConductorClientResponse<Map<String, Object>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public Map<String, Object> resumeAllSchedules() {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/admin/resume")
                .build();

        ConductorClientResponse<Map<String, Object>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public void resumeSchedule(String name) {
        executeStateChange("/scheduler/schedules/{name}/resume", name, null);
    }

    /**
     * Sends a schedule state-change (pause/resume) as {@code PUT} first; if the server answers
     * {@code 405 Method Not Allowed} (Orkes Conductor, historically), falls back to {@code GET}
     * and remembers that verdict for the rest of this instance's lifetime. Any other error
     * status (403, 404, 5xx, ...) propagates immediately — only a routing mismatch (405)
     * triggers the fallback.
     */
    private void executeStateChange(String path, String name, String reason) {
        if (!useGetForScheduleStateChange) {
            try {
                client.execute(stateChangeRequest(Method.PUT, path, name, reason));
                return;
            } catch (ConductorClientException e) {
                if (e.getStatus() != 405) {
                    throw e;
                }
                useGetForScheduleStateChange = true;
            }
        }
        client.execute(stateChangeRequest(Method.GET, path, name, reason));
    }

    private ConductorClientRequest stateChangeRequest(Method method, String path, String name, String reason) {
        ConductorClientRequest.Builder builder = ConductorClientRequest.builder()
                .method(method)
                .path(path)
                .addPathParam("name", name);
        if (reason != null) {
            builder.addQueryParam("reason", reason);
        }
        return builder.build();
    }

    public void saveSchedule(SaveScheduleRequest saveScheduleRequest) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.POST)
                .path("/scheduler/schedules")
                .body(saveScheduleRequest)
                .build();

        client.execute(request);
    }

    public SearchResultWorkflowScheduleExecution search(
            Integer start, Integer size, String sort, String freeText, String query) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/search/executions")
                .addQueryParam("start", start)
                .addQueryParam("size", size)
                .addQueryParam("sort", sort)
                .addQueryParam("freeText", freeText)
                .addQueryParam("query", query)
                .build();

        ConductorClientResponse<SearchResultWorkflowScheduleExecution> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    public void deleteTagForSchedule(String name, List<TagObject> body) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.DELETE)
                .path("/scheduler/schedules/{name}/tags")
                .addPathParam("name", name)
                .body(body)
                .build();

        client.execute(request);
    }

    public void putTagForSchedule(String name, List<TagObject> body) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/scheduler/schedules/{name}/tags")
                .addPathParam("name", name)
                .body(body)
                .build();

        client.execute(request);
    }

    public List<TagObject> getTagsForSchedule(String name) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.GET)
                .path("/scheduler/schedules/{name}/tags")
                .addPathParam("name", name)
                .build();

        ConductorClientResponse<List<TagObject>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
}
