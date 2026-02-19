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
package com.netflix.conductor.client.events;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.task.TaskPayloadUsedEvent;
import com.netflix.conductor.client.events.task.TaskResultPayloadSizeEvent;
import com.netflix.conductor.client.events.taskrunner.PollCompleted;
import com.netflix.conductor.client.events.taskrunner.PollFailure;
import com.netflix.conductor.client.events.taskrunner.PollStarted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionStarted;
import com.netflix.conductor.client.events.workflow.WorkflowInputPayloadSizeEvent;
import com.netflix.conductor.client.events.workflow.WorkflowPayloadUsedEvent;
import com.netflix.conductor.client.events.workflow.WorkflowStartedEvent;

import static org.junit.jupiter.api.Assertions.*;

class EventPojoTests {

    // --- taskrunner package ---

    @Test
    void testPollStarted() {
        PollStarted event = new PollStarted("HTTP_TASK");

        assertEquals("HTTP_TASK", event.getTaskType());
        assertNotNull(event.getTime());
    }

    @Test
    void testPollCompleted() {
        PollCompleted event = new PollCompleted("HTTP_TASK", 500L);

        assertEquals("HTTP_TASK", event.getTaskType());
        assertEquals(Duration.ofMillis(500), event.getDuration());
        assertNotNull(event.getTime());
    }

    @Test
    void testPollFailure() {
        RuntimeException cause = new RuntimeException("timeout");
        PollFailure event = new PollFailure("HTTP_TASK", 1200L, cause);

        assertEquals("HTTP_TASK", event.getTaskType());
        assertEquals(Duration.ofMillis(1200), event.getDuration());
        assertSame(cause, event.getCause());
        assertNotNull(event.getTime());
    }

    @Test
    void testTaskExecutionStarted() {
        TaskExecutionStarted event = new TaskExecutionStarted("SIMPLE", "task-123", "worker-1");

        assertEquals("SIMPLE", event.getTaskType());
        assertEquals("task-123", event.getTaskId());
        assertEquals("worker-1", event.getWorkerId());
        assertNotNull(event.getTime());
    }

    @Test
    void testTaskExecutionCompleted() {
        TaskExecutionCompleted event = new TaskExecutionCompleted("SIMPLE", "task-456", "worker-2", 350L);

        assertEquals("SIMPLE", event.getTaskType());
        assertEquals("task-456", event.getTaskId());
        assertEquals("worker-2", event.getWorkerId());
        assertEquals(Duration.ofMillis(350), event.getDuration());
        assertNotNull(event.getTime());
    }

    @Test
    void testTaskExecutionFailure() {
        Throwable cause = new IllegalStateException("bad state");
        TaskExecutionFailure event = new TaskExecutionFailure("SIMPLE", "task-789", "worker-3", cause, 999L);

        assertEquals("SIMPLE", event.getTaskType());
        assertEquals("task-789", event.getTaskId());
        assertEquals("worker-3", event.getWorkerId());
        assertSame(cause, event.getCause());
        assertEquals(Duration.ofMillis(999), event.getDuration());
        assertNotNull(event.getTime());
    }

    // --- workflow package ---

    @Test
    void testWorkflowStartedEvent() {
        Throwable t = new RuntimeException("fail");
        WorkflowStartedEvent event = new WorkflowStartedEvent("myWorkflow", 2, false, t);

        assertEquals("myWorkflow", event.getName());
        assertEquals(2, event.getVersion());
        assertFalse(event.isSuccess());
        assertSame(t, event.getThrowable());
        assertNotNull(event.getTime());
    }

    @Test
    void testWorkflowInputPayloadSizeEvent() {
        WorkflowInputPayloadSizeEvent event = new WorkflowInputPayloadSizeEvent("wf1", 1, 4096L);

        assertEquals("wf1", event.getName());
        assertEquals(1, event.getVersion());
        assertEquals(4096L, event.getSize());
        assertNotNull(event.getTime());
    }

    @Test
    void testWorkflowPayloadUsedEvent() {
        WorkflowPayloadUsedEvent event = new WorkflowPayloadUsedEvent("wf2", 3, "READ", "input");

        assertEquals("wf2", event.getName());
        assertEquals(3, event.getVersion());
        assertEquals("READ", event.getOperation());
        assertEquals("input", event.getPayloadType());
        assertNotNull(event.getTime());
    }

    // --- task package ---

    @Test
    void testTaskPayloadUsedEvent() {
        TaskPayloadUsedEvent event = new TaskPayloadUsedEvent("DECIDE", "WRITE", "output");

        assertEquals("DECIDE", event.getTaskType());
        assertEquals("WRITE", event.getOperation());
        assertEquals("output", event.getPayloadType());
        assertNotNull(event.getTime());
    }

    @Test
    void testTaskResultPayloadSizeEvent() {
        TaskResultPayloadSizeEvent event = new TaskResultPayloadSizeEvent("HTTP_TASK", 2048L);

        assertEquals("HTTP_TASK", event.getTaskType());
        assertEquals(2048L, event.getSize());
        assertNotNull(event.getTime());
    }

    // --- cross-cutting tests ---

    @Test
    void testPollStartedInheritance() {
        PollStarted event = new PollStarted("testTask");

        assertInstanceOf(ConductorClientEvent.class, event);
        assertNotNull(event.getTime());
    }

    @Test
    void testWorkflowStartedConvenienceConstructor() {
        WorkflowStartedEvent event = new WorkflowStartedEvent("simpleWf", 1);

        assertEquals("simpleWf", event.getName());
        assertEquals(1, event.getVersion());
        assertTrue(event.isSuccess());
        assertNull(event.getThrowable());
    }

    @Test
    void testToStringDoesNotThrow() {
        PollStarted pollStarted = new PollStarted("task1");
        assertDoesNotThrow(() -> pollStarted.toString());
        assertNotNull(pollStarted.toString());

        WorkflowStartedEvent wfEvent = new WorkflowStartedEvent("wf", 1, false, null);
        assertDoesNotThrow(() -> wfEvent.toString());
        assertNotNull(wfEvent.toString());

        TaskResultPayloadSizeEvent sizeEvent = new TaskResultPayloadSizeEvent("task2", 100L);
        assertDoesNotThrow(() -> sizeEvent.toString());
        assertNotNull(sizeEvent.toString());
    }
}
