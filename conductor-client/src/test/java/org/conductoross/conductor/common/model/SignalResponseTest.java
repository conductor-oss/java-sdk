/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.common.model;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalResponseTest {

    @Test
    void responseTypeHelpersIdentifyOnlyTheirOwnShape() {
        SignalResponse response = new SignalResponse();

        assertAll(
                () -> assertFalse(response.isTargetWorkflow()),
                () -> assertFalse(response.isBlockingWorkflow()),
                () -> assertFalse(response.isBlockingTask()),
                () -> assertFalse(response.isBlockingTaskInput()),
                () -> assertFalse(response.isBlockingTaskList()));

        response.setResponseType(ReturnStrategy.TARGET_WORKFLOW);
        assertTrue(response.isTargetWorkflow());
        response.setResponseType(ReturnStrategy.BLOCKING_WORKFLOW);
        assertTrue(response.isBlockingWorkflow());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK);
        assertTrue(response.isBlockingTask());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK_INPUT);
        assertTrue(response.isBlockingTaskInput());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK_LIST);
        assertTrue(response.isBlockingTaskList());
    }

    @Test
    void hasBlockingTaskUsesTheFieldForEachResponseShape() {
        assertTrue(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_WORKFLOW)
                .blockingTaskId("blocking-task")
                .build()
                .hasBlockingTask());
        assertTrue(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK)
                .taskId("blocking-task")
                .build()
                .hasBlockingTask());
        assertTrue(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_INPUT)
                .taskId("blocking-task")
                .build()
                .hasBlockingTask());
        assertTrue(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_LIST)
                .tasks(List.of(new Task()))
                .build()
                .hasBlockingTask());
    }

    @Test
    void hasBlockingTaskIsFalseWhenTheResponseDoesNotIdentifyOne() {
        assertFalse(new SignalResponse().hasBlockingTask());
        assertFalse(SignalResponse.builder()
                .responseType(ReturnStrategy.TARGET_WORKFLOW)
                .tasks(List.of(new Task()))
                .build()
                .hasBlockingTask());
        assertFalse(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_WORKFLOW)
                .build()
                .hasBlockingTask());
        assertFalse(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK)
                .build()
                .hasBlockingTask());
        assertFalse(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_LIST)
                .tasks(List.of())
                .build()
                .hasBlockingTask());
        assertFalse(SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_LIST)
                .build()
                .hasBlockingTask());
    }

    @Test
    void getWorkflowCopiesWorkflowFieldsForBothWorkflowShapes() {
        Task task = new Task();
        Map<String, Object> input = Map.of("input", "value");
        Map<String, Object> output = Map.of("output", "value");
        Map<String, Object> variables = Map.of("variable", "value");
        SignalResponse response = SignalResponse.builder()
                .responseType(ReturnStrategy.TARGET_WORKFLOW)
                .workflowId("workflow-id")
                .status(Workflow.WorkflowStatus.RUNNING.name())
                .tasks(List.of(task))
                .createdBy("creator")
                .createTime(10L)
                .updateTime(20L)
                .input(input)
                .output(output)
                .variables(variables)
                .priority(1)
                .build();

        Workflow workflow = response.getWorkflow();

        assertAll(
                () -> assertEquals("workflow-id", workflow.getWorkflowId()),
                () -> assertEquals(Workflow.WorkflowStatus.RUNNING, workflow.getStatus()),
                () -> assertEquals(List.of(task), workflow.getTasks()),
                () -> assertEquals("creator", workflow.getCreatedBy()),
                () -> assertEquals(10L, workflow.getCreateTime()),
                () -> assertEquals(20L, workflow.getUpdateTime()),
                () -> assertSame(input, workflow.getInput()),
                () -> assertSame(output, workflow.getOutput()),
                () -> assertSame(variables, workflow.getVariables()),
                () -> assertEquals(1, workflow.getPriority()));

        response.setResponseType(ReturnStrategy.BLOCKING_WORKFLOW);
        assertEquals("workflow-id", response.getWorkflow().getWorkflowId());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK);
        assertThrows(IllegalStateException.class, response::getWorkflow);
    }

    @Test
    void getBlockingTaskBuildsFlattenedTask() {
        Map<String, Object> input = Map.of("input", "value");
        Map<String, Object> output = Map.of("output", "value");
        SignalResponse response = SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK)
                .taskId("task-id")
                .taskType("WAIT")
                .taskDefName("task-definition")
                .workflowType("workflow-definition")
                .referenceTaskName("task-reference")
                .retryCount(2)
                .status(Task.Status.IN_PROGRESS.name())
                .input(input)
                .output(output)
                .build();

        Task task = response.getBlockingTask();

        assertAll(
                () -> assertEquals("task-id", task.getTaskId()),
                () -> assertEquals("WAIT", task.getTaskType()),
                () -> assertEquals("task-definition", task.getTaskDefName()),
                () -> assertEquals("workflow-definition", task.getWorkflowType()),
                () -> assertEquals("task-reference", task.getReferenceTaskName()),
                () -> assertEquals(2, task.getRetryCount()),
                () -> assertEquals(Task.Status.IN_PROGRESS, task.getStatus()),
                () -> assertSame(input, task.getInputData()),
                () -> assertSame(output, task.getOutputData()));
    }

    @Test
    void getBlockingTaskHandlesEveryTaskShapeAndNoBlockerState() {
        Task listTask = new Task();
        SignalResponse response = SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_LIST)
                .tasks(List.of(listTask))
                .build();
        assertSame(listTask, response.getBlockingTask());

        response = SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_INPUT)
                .taskId("task-id")
                .retryCount(0)
                .build();
        assertEquals("task-id", response.getBlockingTask().getTaskId());

        response = SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK)
                .taskId("task-id")
                .retryCount(0)
                .build();
        assertNull(response.getBlockingTask().getStatus());

        response.setTaskId(null);
        assertNull(response.getBlockingTask());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK_LIST);
        response.setTasks(List.of());
        assertNull(response.getBlockingTask());
        response.setResponseType(ReturnStrategy.TARGET_WORKFLOW);
        assertThrows(IllegalStateException.class, response::getBlockingTask);
    }

    @Test
    void getTaskInputReturnsInputOnlyWhileATaskIsBlocking() {
        Map<String, Object> input = Map.of("input", "value");
        SignalResponse response = SignalResponse.builder()
                .responseType(ReturnStrategy.BLOCKING_TASK_INPUT)
                .taskId("task-id")
                .input(input)
                .build();

        assertSame(input, response.getTaskInput());
        response.setTaskId(null);
        assertNull(response.getTaskInput());
        response.setResponseType(ReturnStrategy.BLOCKING_TASK);
        assertThrows(IllegalStateException.class, response::getTaskInput);
    }
}
