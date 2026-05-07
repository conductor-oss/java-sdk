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

public class ListenerRegister {

    private static final java.util.Set<RegistrationKey> registered =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static void register(TaskRunnerEventsListener listener, EventDispatcher<TaskRunnerEvent> dispatcher) {
        if (!registered.add(new RegistrationKey(listener, dispatcher))) {
            return;
        }
        dispatcher.register(PollFailure.class, listener::consume);
        dispatcher.register(PollCompleted.class, listener::consume);
        dispatcher.register(PollStarted.class, listener::consume);
        dispatcher.register(TaskExecutionStarted.class, listener::consume);
        dispatcher.register(TaskExecutionCompleted.class, listener::consume);
        dispatcher.register(TaskExecutionFailure.class, listener::consume);
        dispatcher.register(TaskUpdateCompleted.class, listener::consume);
        dispatcher.register(TaskUpdateFailure.class, listener::consume);
        dispatcher.register(TaskAckFailure.class, listener::consume);
        dispatcher.register(TaskAckError.class, listener::consume);
        dispatcher.register(TaskExecutionQueueFull.class, listener::consume);
        dispatcher.register(TaskPaused.class, listener::consume);
        dispatcher.register(ThreadUncaughtException.class, listener::consume);
        dispatcher.register(ActiveWorkersChanged.class, listener::consume);
    }

    public static void register(TaskClientListener listener, EventDispatcher<TaskClientEvent> dispatcher) {
        if (!registered.add(new RegistrationKey(listener, dispatcher))) {
            return;
        }
        dispatcher.register(TaskResultPayloadSizeEvent.class, listener::consume);
        dispatcher.register(TaskPayloadUsedEvent.class, listener::consume);
    }

    public static void register(WorkflowClientListener listener, EventDispatcher<WorkflowClientEvent> dispatcher) {
        if (!registered.add(new RegistrationKey(listener, dispatcher))) {
            return;
        }
        dispatcher.register(WorkflowStartedEvent.class, listener::consume);
        dispatcher.register(WorkflowInputPayloadSizeEvent.class, listener::consume);
        dispatcher.register(WorkflowPayloadUsedEvent.class, listener::consume);
    }

    private record RegistrationKey(Object listener, Object dispatcher) { }
}
