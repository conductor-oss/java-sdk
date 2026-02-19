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
package com.netflix.conductor.client.events.listeners;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.task.TaskClientEvent;
import com.netflix.conductor.client.events.task.TaskPayloadUsedEvent;
import com.netflix.conductor.client.events.task.TaskResultPayloadSizeEvent;
import com.netflix.conductor.client.events.taskrunner.PollCompleted;
import com.netflix.conductor.client.events.taskrunner.PollFailure;
import com.netflix.conductor.client.events.taskrunner.PollStarted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionStarted;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.events.workflow.WorkflowClientEvent;
import com.netflix.conductor.client.events.workflow.WorkflowInputPayloadSizeEvent;
import com.netflix.conductor.client.events.workflow.WorkflowPayloadUsedEvent;
import com.netflix.conductor.client.events.workflow.WorkflowStartedEvent;

import static org.junit.jupiter.api.Assertions.*;

class ListenerRegisterTest {

    @Test
    void testRegisterTaskRunnerListener() throws InterruptedException {
        EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PollStarted> received = new AtomicReference<>();

        TaskRunnerEventsListener listener = new TaskRunnerEventsListener() {
            @Override
            public void consume(PollStarted e) {
                received.set(e);
                latch.countDown();
            }

            @Override
            public void consume(PollCompleted e) {}

            @Override
            public void consume(PollFailure e) {}

            @Override
            public void consume(TaskExecutionStarted e) {}

            @Override
            public void consume(TaskExecutionCompleted e) {}

            @Override
            public void consume(TaskExecutionFailure e) {}
        };

        ListenerRegister.register(listener, dispatcher);

        PollStarted event = new PollStarted("test_task");
        dispatcher.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener should have received PollStarted event");
        assertSame(event, received.get());
        assertEquals("test_task", received.get().getTaskType());
    }

    @Test
    void testRegisterTaskClientListener() throws InterruptedException {
        EventDispatcher<TaskClientEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TaskResultPayloadSizeEvent> received = new AtomicReference<>();

        TaskClientListener listener = new TaskClientListener() {
            @Override
            public void consume(TaskPayloadUsedEvent e) {}

            @Override
            public void consume(TaskResultPayloadSizeEvent e) {
                received.set(e);
                latch.countDown();
            }
        };

        ListenerRegister.register(listener, dispatcher);

        TaskResultPayloadSizeEvent event = new TaskResultPayloadSizeEvent("task1", 100L);
        dispatcher.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener should have received TaskResultPayloadSizeEvent");
        assertSame(event, received.get());
        assertEquals("task1", received.get().getTaskType());
        assertEquals(100L, received.get().getSize());
    }

    @Test
    void testRegisterWorkflowClientListener() throws InterruptedException {
        EventDispatcher<WorkflowClientEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<WorkflowStartedEvent> received = new AtomicReference<>();

        WorkflowClientListener listener = new WorkflowClientListener() {
            @Override
            public void consume(WorkflowStartedEvent event) {
                received.set(event);
                latch.countDown();
            }

            @Override
            public void consume(WorkflowInputPayloadSizeEvent event) {}

            @Override
            public void consume(WorkflowPayloadUsedEvent event) {}
        };

        ListenerRegister.register(listener, dispatcher);

        WorkflowStartedEvent event = new WorkflowStartedEvent("wf1", 1);
        dispatcher.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener should have received WorkflowStartedEvent");
        assertSame(event, received.get());
        assertEquals("wf1", received.get().getName());
        assertEquals(1, received.get().getVersion());
        assertTrue(received.get().isSuccess());
    }
}
