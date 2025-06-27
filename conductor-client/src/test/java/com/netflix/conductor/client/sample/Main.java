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
import java.util.stream.Collectors;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.validation.ValidationError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class Main {

    public static void main(String[] args) {

        testUpdateWorkflowDef();
    }


    public static void testUpdateWorkflowDef() {
        try {
            ConductorClient client = new ConductorClient();
            MetadataClient metadataClient = new MetadataClient(client);
            WorkflowDef workflowDef = new WorkflowDef();
            List<WorkflowDef> workflowDefList = new ArrayList<>();
            workflowDefList.add(workflowDef);
            metadataClient.updateWorkflowDefs(workflowDefList);
        } catch (ConductorClientException e) {
            assertEquals(400, e.getStatus());
            assertEquals("Validation failed, check below errors for detail.", e.getMessage());
            assertFalse(e.isRetryable());
            List<ValidationError> errors = e.getValidationErrors();
            List<String> errorMessages =
                errors.stream().map(ValidationError::getMessage).collect(Collectors.toList());
            assertEquals(3, errors.size());
            assertTrue(errorMessages.contains("WorkflowTask list cannot be empty"));
            assertTrue(errorMessages.contains("WorkflowDef name cannot be null or empty"));
            assertTrue(errorMessages.contains("ownerEmail cannot be empty"));
        }
    }
}
