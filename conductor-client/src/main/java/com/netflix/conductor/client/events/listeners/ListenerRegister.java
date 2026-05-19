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
package com.netflix.conductor.client.events.listeners;

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

/**
 * Stateless helper that wires event listeners onto {@link EventDispatcher}
 * instances using the listener itself as the registration key.
 *
 * <p>Because {@link EventDispatcher#register(Class, Object, java.util.function.Consumer)}
 * is idempotent per {@code (eventType, key)}, calling any {@code register}
 * method here multiple times with the same {@code (listener, dispatcher)} pair
 * is a safe no-op — no external synchronization or static tracking required.
 *
 * <p>A single listener is intentionally registered on <em>multiple, distinct</em>
 * dispatchers (each an independent event source). The {@code listener} object
 * serves as the key within each dispatcher, so the same listener can exist on
 * all dispatchers while being prevented from double-registering on any single
 * one.
 */
public class ListenerRegister {

    // --- TaskRunnerEventsListener ---

    public static void register(TaskRunnerEventsListener listener, EventDispatcher<TaskRunnerEvent> dispatcher) {
        dispatcher.register(PollFailure.class, listener, listener::consume);
        dispatcher.register(PollCompleted.class, listener, listener::consume);
        dispatcher.register(PollStarted.class, listener, listener::consume);
        dispatcher.register(TaskExecutionStarted.class, listener, listener::consume);
        dispatcher.register(TaskExecutionCompleted.class, listener, listener::consume);
        dispatcher.register(TaskExecutionFailure.class, listener, listener::consume);
        dispatcher.register(TaskUpdateCompleted.class, listener, listener::consume);
        dispatcher.register(TaskUpdateFailure.class, listener, listener::consume);
        dispatcher.register(TaskAckFailure.class, listener, listener::consume);
        dispatcher.register(TaskAckError.class, listener, listener::consume);
        dispatcher.register(TaskExecutionQueueFull.class, listener, listener::consume);
        dispatcher.register(TaskPaused.class, listener, listener::consume);
        dispatcher.register(ThreadUncaughtException.class, listener, listener::consume);
        dispatcher.register(ActiveWorkersChanged.class, listener, listener::consume);
    }

    public static void unregister(TaskRunnerEventsListener listener, EventDispatcher<TaskRunnerEvent> dispatcher) {
        dispatcher.unregister(PollFailure.class, listener);
        dispatcher.unregister(PollCompleted.class, listener);
        dispatcher.unregister(PollStarted.class, listener);
        dispatcher.unregister(TaskExecutionStarted.class, listener);
        dispatcher.unregister(TaskExecutionCompleted.class, listener);
        dispatcher.unregister(TaskExecutionFailure.class, listener);
        dispatcher.unregister(TaskUpdateCompleted.class, listener);
        dispatcher.unregister(TaskUpdateFailure.class, listener);
        dispatcher.unregister(TaskAckFailure.class, listener);
        dispatcher.unregister(TaskAckError.class, listener);
        dispatcher.unregister(TaskExecutionQueueFull.class, listener);
        dispatcher.unregister(TaskPaused.class, listener);
        dispatcher.unregister(ThreadUncaughtException.class, listener);
        dispatcher.unregister(ActiveWorkersChanged.class, listener);
    }

    // --- TaskClientListener ---

    public static void register(TaskClientListener listener, EventDispatcher<TaskClientEvent> dispatcher) {
        dispatcher.register(TaskResultPayloadSizeEvent.class, listener, listener::consume);
        dispatcher.register(TaskPayloadUsedEvent.class, listener, listener::consume);
    }

    public static void unregister(TaskClientListener listener, EventDispatcher<TaskClientEvent> dispatcher) {
        dispatcher.unregister(TaskResultPayloadSizeEvent.class, listener);
        dispatcher.unregister(TaskPayloadUsedEvent.class, listener);
    }

    // --- WorkflowClientListener ---

    public static void register(WorkflowClientListener listener, EventDispatcher<WorkflowClientEvent> dispatcher) {
        dispatcher.register(WorkflowStartedEvent.class, listener, listener::consume);
        dispatcher.register(WorkflowInputPayloadSizeEvent.class, listener, listener::consume);
        dispatcher.register(WorkflowPayloadUsedEvent.class, listener, listener::consume);
    }

    public static void unregister(WorkflowClientListener listener, EventDispatcher<WorkflowClientEvent> dispatcher) {
        dispatcher.unregister(WorkflowStartedEvent.class, listener);
        dispatcher.unregister(WorkflowInputPayloadSizeEvent.class, listener);
        dispatcher.unregister(WorkflowPayloadUsedEvent.class, listener);
    }
}
