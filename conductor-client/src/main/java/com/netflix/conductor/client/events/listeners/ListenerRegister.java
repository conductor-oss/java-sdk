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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.netflix.conductor.client.events.ConductorClientEvent;
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
 * Idempotent registration (and unregistration) of event listeners onto
 * {@link EventDispatcher} instances.
 *
 * <h3>Why the dedup key includes the dispatcher reference</h3>
 *
 * <p>A single {@link com.netflix.conductor.client.metrics.MetricsCollector
 * MetricsCollector} is intentionally registered on <em>multiple, distinct</em>
 * dispatchers because each dispatcher is an independent event source:
 *
 * <ol>
 *   <li>{@code TaskClient.eventDispatcher} &mdash;
 *       {@link TaskClientEvent}s (payload size, external storage)</li>
 *   <li>{@code TaskClient.taskRunnerEventDispatcher} &mdash;
 *       {@link TaskRunnerEvent}s emitted from {@code TaskClient.ack()}</li>
 *   <li>{@code WorkflowClient.eventDispatcher} &mdash;
 *       {@link WorkflowClientEvent}s (workflow started, payload size)</li>
 *   <li>{@code TaskRunnerConfigurer.Builder.eventDispatcher} &mdash;
 *       {@link TaskRunnerEvent}s from the poll/execute/update cycle</li>
 * </ol>
 *
 * <p>The key {@code (listener, dispatcher)} therefore correctly allows the same
 * collector to be wired onto all four dispatchers while preventing the same
 * collector from being registered onto the <em>same</em> dispatcher twice
 * (which would double-count every event on that source).
 *
 * <h3>Lifecycle</h3>
 *
 * <p>Call the matching {@code unregister} overload during shutdown to release
 * the entry from the static set and detach the listener from the dispatcher.
 * This prevents the static set from accumulating stale entries in long-lived
 * JVMs that re-create client or configurer instances.
 */
public class ListenerRegister {

    private static final Map<RegistrationKey, List<ConsumerBinding>> registered =
            new ConcurrentHashMap<>();

    // --- TaskRunnerEventsListener ---

    public static void register(TaskRunnerEventsListener listener, EventDispatcher<TaskRunnerEvent> dispatcher) {
        RegistrationKey key = new RegistrationKey(listener, dispatcher);
        if (registered.containsKey(key)) {
            return;
        }

        List<ConsumerBinding> bindings = List.of(
                bind(dispatcher, PollFailure.class, listener::consume),
                bind(dispatcher, PollCompleted.class, listener::consume),
                bind(dispatcher, PollStarted.class, listener::consume),
                bind(dispatcher, TaskExecutionStarted.class, listener::consume),
                bind(dispatcher, TaskExecutionCompleted.class, listener::consume),
                bind(dispatcher, TaskExecutionFailure.class, listener::consume),
                bind(dispatcher, TaskUpdateCompleted.class, listener::consume),
                bind(dispatcher, TaskUpdateFailure.class, listener::consume),
                bind(dispatcher, TaskAckFailure.class, listener::consume),
                bind(dispatcher, TaskAckError.class, listener::consume),
                bind(dispatcher, TaskExecutionQueueFull.class, listener::consume),
                bind(dispatcher, TaskPaused.class, listener::consume),
                bind(dispatcher, ThreadUncaughtException.class, listener::consume),
                bind(dispatcher, ActiveWorkersChanged.class, listener::consume));

        registered.putIfAbsent(key, bindings);
    }

    public static void unregister(TaskRunnerEventsListener listener, EventDispatcher<TaskRunnerEvent> dispatcher) {
        removeBindings(new RegistrationKey(listener, dispatcher));
    }

    // --- TaskClientListener ---

    public static void register(TaskClientListener listener, EventDispatcher<TaskClientEvent> dispatcher) {
        RegistrationKey key = new RegistrationKey(listener, dispatcher);
        if (registered.containsKey(key)) {
            return;
        }

        List<ConsumerBinding> bindings = List.of(
                bind(dispatcher, TaskResultPayloadSizeEvent.class, listener::consume),
                bind(dispatcher, TaskPayloadUsedEvent.class, listener::consume));

        registered.putIfAbsent(key, bindings);
    }

    public static void unregister(TaskClientListener listener, EventDispatcher<TaskClientEvent> dispatcher) {
        removeBindings(new RegistrationKey(listener, dispatcher));
    }

    // --- WorkflowClientListener ---

    public static void register(WorkflowClientListener listener, EventDispatcher<WorkflowClientEvent> dispatcher) {
        RegistrationKey key = new RegistrationKey(listener, dispatcher);
        if (registered.containsKey(key)) {
            return;
        }

        List<ConsumerBinding> bindings = List.of(
                bind(dispatcher, WorkflowStartedEvent.class, listener::consume),
                bind(dispatcher, WorkflowInputPayloadSizeEvent.class, listener::consume),
                bind(dispatcher, WorkflowPayloadUsedEvent.class, listener::consume));

        registered.putIfAbsent(key, bindings);
    }

    public static void unregister(WorkflowClientListener listener, EventDispatcher<WorkflowClientEvent> dispatcher) {
        removeBindings(new RegistrationKey(listener, dispatcher));
    }

    // --- internals ---

    @SuppressWarnings("unchecked")
    private static void removeBindings(RegistrationKey key) {
        List<ConsumerBinding> bindings = registered.remove(key);
        if (bindings == null) {
            return;
        }
        for (ConsumerBinding b : bindings) {
            b.dispatcher.unregister(b.eventType, b.consumer);
        }
    }

    private static <T extends ConductorClientEvent, U extends T> ConsumerBinding bind(
            EventDispatcher<T> dispatcher, Class<U> eventType, Consumer<U> consumer) {
        dispatcher.register(eventType, consumer);
        return new ConsumerBinding(dispatcher, eventType, consumer);
    }

    private record RegistrationKey(Object listener, Object dispatcher) { }

    @SuppressWarnings("rawtypes")
    private record ConsumerBinding(EventDispatcher dispatcher, Class eventType, Consumer consumer) { }
}
