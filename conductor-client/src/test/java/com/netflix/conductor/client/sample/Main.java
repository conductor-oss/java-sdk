/*
 * Copyright 2020 Conductor Authors.
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
package com.netflix.conductor.client.sample;

import java.util.ArrayList;
import java.util.List;

import com.netflix.conductor.clientv2.ApiException;
import com.netflix.conductor.clientv2.http.MetadataResourceApi;
import com.netflix.conductor.clientv2.http.model.WorkflowDef;
import com.netflix.conductor.clientv2.http.model.WorkflowTask;



public class Main {

    public static void main(String[] args) {

        testUpdateWorkflowDef();
    }


    public static void testUpdateWorkflowDef() {
        try {
            MetadataResourceApi metadataResourceApi = new MetadataResourceApi();
            WorkflowDef workflowDef = new WorkflowDef();
            workflowDef.setName("test");
            workflowDef.setOwnerEmail("test@test.com");
            workflowDef.setTasks(new ArrayList<>());
            WorkflowTask workflowTask = new WorkflowTask();
            workflowTask.setTaskReferenceName("test");
            workflowTask.setName("test");
            workflowTask.setType("test");
            workflowDef.getTasks().add(workflowTask);
            List<WorkflowDef> workflowDefList = new ArrayList<>();
            workflowDefList.add(workflowDef);
            metadataResourceApi.update(workflowDefList);
        } catch (ApiException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCode());
            System.out.println(e.getResponseHeaders());
            System.out.println(e.getResponseBody());
            System.exit(1);
        }
    }
}
