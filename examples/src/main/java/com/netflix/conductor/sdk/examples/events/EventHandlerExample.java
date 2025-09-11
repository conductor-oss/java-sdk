/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.sdk.examples.events;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import io.orkes.conductor.client.http.OrkesEventClient;
import io.orkes.conductor.client.model.OrkesEventHandler;
import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.sdk.examples.util.ClientUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Shows how you can create a worker that polls and executes a SIMPLE task
 * and register listeners to expose metrics with micrometer.
 */
@Slf4j
public class EventHandlerExample {
    private static final String WORKFLOW_NAME = "test_event_handler";
    private static final String EVENT_HANDLER_NAME = "test_event_handler";

    public static void main(String[] args) throws IOException {
        var client = ClientUtil.getClient();
        var eventClient = new OrkesEventClient(client);
        // var workflowClient = new OrkesWorkflowClient(client);
        var metadataClient = new MetadataClient(client);

        try {
            metadataClient.getWorkflowDef(WORKFLOW_NAME, 1);
        } catch (Exception e) {
            metadataClient.registerWorkflowDef(getWorkflowDef());
        }

        var eventHandlers = eventClient.getOrkesEventHandlers();
        if (!eventHandlers.stream().anyMatch(handler -> handler.getName().equals(EVENT_HANDLER_NAME))) {
            eventClient.registerEventHandler(getEventHandler());
        }

        eventClient.putTagsForEventHandler(EVENT_HANDLER_NAME, List.of(Tag.builder().key("test").value("test").build(), Tag.builder().key("test2").value("test2").build()));

        var oldHandlers = eventClient.getEventHandlers();
        var newHandlers = eventClient.getOrkesEventHandlers();
        log.info("Event handlers: {}", oldHandlers);
        log.info("Orkes event handlers: {}", newHandlers);

        var handler = eventClient.getOrkesEventHandlers(EVENT_HANDLER_NAME, false);
        log.info("Handlers by name: {}", handler);
    }

    private static OrkesEventHandler getEventHandler() {
        EventHandler.Action action = new EventHandler.Action();
        action.setAction(EventHandler.Action.Type.start_workflow);
        var startWorkflow = new EventHandler.StartWorkflow();
        startWorkflow.setName(WORKFLOW_NAME);
        action.setStart_workflow(startWorkflow);
        return OrkesEventHandler.orkesBuilder().name(EVENT_HANDLER_NAME)
                .event("sqs:IKsqs2:test-sqs-queue") // IKsqs2 integration should already exist
                .active(true)
                .actions(List.of(action))
                // TODO: this tags are ignored by the server
                .tags(List.of(Tag.builder().key("test").value("test").build(), Tag.builder().key("test2").value("test2").build()))
                .build();
    }

    private static WorkflowDef getWorkflowDef() {
        WorkflowTask task = new WorkflowTask();
        task.setName("test_simple_inline");
        task.setTaskReferenceName("test_simple_inline-ref");
        task.setType(TaskType.INLINE.toString());
        task.setInputParameters(Map.of("expression", "(function () { return 42;})();",
                "evaluatorType", "graaljs"));

        WorkflowDef workflowDef = new WorkflowDef();
        workflowDef.setName(WORKFLOW_NAME);
        workflowDef.setDescription("test event handler");
        workflowDef.setVersion(1);
        workflowDef.setSchemaVersion(2);
        workflowDef.setTasks(List.of(task));

        return workflowDef;
    }
}
