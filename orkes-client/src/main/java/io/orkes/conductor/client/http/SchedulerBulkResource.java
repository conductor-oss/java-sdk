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

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.ConductorClientRequest.Method;
import com.netflix.conductor.client.http.ConductorClientResponse;
import com.netflix.conductor.common.model.BulkResponse;

import com.fasterxml.jackson.core.type.TypeReference;


class SchedulerBulkResource {

    private final ConductorClient client;

    SchedulerBulkResource(ConductorClient client) {
        this.client = client;
    }

    BulkResponse<String> pauseSchedulers(List<String> workflowIds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/scheduler/bulk/pause")
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse<String>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }

    BulkResponse<String> resumeSchedulers(List<String> workflowIds) {
        ConductorClientRequest request = ConductorClientRequest.builder()
                .method(Method.PUT)
                .path("/scheduler/bulk/resume")
                .body(workflowIds)
                .build();

        ConductorClientResponse<BulkResponse<String>> resp = client.execute(request, new TypeReference<>() {
        });

        return resp.getData();
    }
}
