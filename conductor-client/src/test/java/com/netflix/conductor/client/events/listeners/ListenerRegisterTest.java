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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.task.TaskClientEvent;
import com.netflix.conductor.client.events.task.TaskPayloadUsedEvent;
import com.netflix.conductor.client.events.task.TaskResultPayloadSizeEvent;
import com.netflix.conductor.client.events.taskrunner.ActiveWorkersChanged;
import com.netflix.conductor.client.events.taskrunner.PollCompleted;
import com.netflix.conductor.client.events.taskrunner.PollFailure;
import com.netflix.conductor.client.events.taskrunner.PollStarted;
import com.netflix.conductor.client.events.taskrunner.TaskAckError;
import com.netflix.conductor.client.events.taskrunner.TaskAckFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionFailure;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionQueueFull;
import com.netflix.conductor.client.events.taskrunner.TaskExecutionStarted;
import com.netflix.conductor.client.events.taskrunner.TaskPaused;
import com.netflix.conductor.client.events.taskrunner.TaskRunnerEvent;
import com.netflix.conductor.client.events.taskrunner.TaskUpdateCompleted;
import com.netflix.conductor.client.events.taskrunner.TaskUpdateFailure;
import com.netflix.conductor.client.events.taskrunner.ThreadUncaughtException;
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
    void testRegisterActiveWorkersChangedListener() throws InterruptedException {
        EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ActiveWorkersChanged> received = new AtomicReference<>();

        TaskRunnerEventsListener listener = new TaskRunnerEventsListener() {
            @Override
            public void consume(PollStarted e) {}

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

            @Override
            public void consume(ActiveWorkersChanged e) {
                received.set(e);
                latch.countDown();
            }
        };

        ListenerRegister.register(listener, dispatcher);

        ActiveWorkersChanged event = new ActiveWorkersChanged("test_task", 5);
        dispatcher.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Listener should have received ActiveWorkersChanged event");
        assertSame(event, received.get());
        assertEquals("test_task", received.get().getTaskType());
        assertEquals(5, received.get().getCount());
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

    @Test
    void testAllTaskRunnerEventTypesDispatched() throws InterruptedException {
        EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
        ConcurrentHashMap<String, Object> received = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(14);

        TaskRunnerEventsListener listener = new TaskRunnerEventsListener() {
            @Override public void consume(PollStarted e) { received.put("PollStarted", e); latch.countDown(); }
            @Override public void consume(PollCompleted e) { received.put("PollCompleted", e); latch.countDown(); }
            @Override public void consume(PollFailure e) { received.put("PollFailure", e); latch.countDown(); }
            @Override public void consume(TaskExecutionStarted e) { received.put("TaskExecutionStarted", e); latch.countDown(); }
            @Override public void consume(TaskExecutionCompleted e) { received.put("TaskExecutionCompleted", e); latch.countDown(); }
            @Override public void consume(TaskExecutionFailure e) { received.put("TaskExecutionFailure", e); latch.countDown(); }
            @Override public void consume(TaskUpdateCompleted e) { received.put("TaskUpdateCompleted", e); latch.countDown(); }
            @Override public void consume(TaskUpdateFailure e) { received.put("TaskUpdateFailure", e); latch.countDown(); }
            @Override public void consume(TaskAckFailure e) { received.put("TaskAckFailure", e); latch.countDown(); }
            @Override public void consume(TaskAckError e) { received.put("TaskAckError", e); latch.countDown(); }
            @Override public void consume(TaskExecutionQueueFull e) { received.put("TaskExecutionQueueFull", e); latch.countDown(); }
            @Override public void consume(TaskPaused e) { received.put("TaskPaused", e); latch.countDown(); }
            @Override public void consume(ThreadUncaughtException e) { received.put("ThreadUncaughtException", e); latch.countDown(); }
            @Override public void consume(ActiveWorkersChanged e) { received.put("ActiveWorkersChanged", e); latch.countDown(); }
        };

        ListenerRegister.register(listener, dispatcher);

        dispatcher.publish(new PollStarted("t"));
        dispatcher.publish(new PollCompleted("t", 100));
        dispatcher.publish(new PollFailure("t", 200, new RuntimeException()));
        dispatcher.publish(new TaskExecutionStarted("t", "id", "w"));
        dispatcher.publish(new TaskExecutionCompleted("t", "id", "w", 300));
        dispatcher.publish(new TaskExecutionFailure("t", "id", "w", new RuntimeException(), 400));
        dispatcher.publish(new TaskUpdateCompleted("t", "id", "w", "wfId", 500));
        dispatcher.publish(new TaskUpdateFailure("t", "id", "w", "wfId", new RuntimeException(), 600));
        dispatcher.publish(new TaskAckFailure("t", "id"));
        dispatcher.publish(new TaskAckError("t", "id", new RuntimeException()));
        dispatcher.publish(new TaskExecutionQueueFull("t"));
        dispatcher.publish(new TaskPaused("t"));
        dispatcher.publish(new ThreadUncaughtException(new RuntimeException()));
        dispatcher.publish(new ActiveWorkersChanged("t", 5));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All 14 task runner event types should be dispatched");
        assertEquals(14, received.size());
    }

    @Test
    void testAllTaskClientEventTypesDispatched() throws InterruptedException {
        EventDispatcher<TaskClientEvent> dispatcher = new EventDispatcher<>();
        ConcurrentHashMap<String, Object> received = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(2);

        TaskClientListener listener = new TaskClientListener() {
            @Override public void consume(TaskPayloadUsedEvent e) { received.put("TaskPayloadUsedEvent", e); latch.countDown(); }
            @Override public void consume(TaskResultPayloadSizeEvent e) { received.put("TaskResultPayloadSizeEvent", e); latch.countDown(); }
        };

        ListenerRegister.register(listener, dispatcher);

        dispatcher.publish(new TaskPayloadUsedEvent("t", "WRITE", "output"));
        dispatcher.publish(new TaskResultPayloadSizeEvent("t", 2048L));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Both task client event types should be dispatched");
        assertEquals(2, received.size());
    }

    @Test
    void testAllWorkflowClientEventTypesDispatched() throws InterruptedException {
        EventDispatcher<WorkflowClientEvent> dispatcher = new EventDispatcher<>();
        ConcurrentHashMap<String, Object> received = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(3);

        WorkflowClientListener listener = new WorkflowClientListener() {
            @Override public void consume(WorkflowStartedEvent event) { received.put("WorkflowStartedEvent", event); latch.countDown(); }
            @Override public void consume(WorkflowInputPayloadSizeEvent event) { received.put("WorkflowInputPayloadSizeEvent", event); latch.countDown(); }
            @Override public void consume(WorkflowPayloadUsedEvent event) { received.put("WorkflowPayloadUsedEvent", event); latch.countDown(); }
        };

        ListenerRegister.register(listener, dispatcher);

        dispatcher.publish(new WorkflowStartedEvent("wf", 1));
        dispatcher.publish(new WorkflowInputPayloadSizeEvent("wf", 1, 1024L));
        dispatcher.publish(new WorkflowPayloadUsedEvent("wf", 1, "READ", "input"));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All 3 workflow client event types should be dispatched");
        assertEquals(3, received.size());
    }
}
