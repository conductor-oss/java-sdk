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
package com.netflix.conductor.common.run.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;

/**
 * Typed wrapper for EVENT tasks providing convenient access to event properties.
 *
 * <p>EVENT tasks publish messages to external event systems like SQS, Kafka, NATS, etc.
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myEvent");
 * EventTask event = new EventTask(task);
 *
 * String sink = event.getSink();
 * Map<String, Object> payload = event.getEventPayload();
 * }</pre>
 */
public class EventTask extends TypedTask {

    public static final String SINK_INPUT = "sink";
    public static final String EVENT_PRODUCED_OUTPUT = "event_produced";

    /**
     * Creates an EventTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not an EVENT task
     */
    public EventTask(Task task) {
        super(task, TaskType.TASK_TYPE_EVENT);
    }

    /**
     * Checks if the given task is an EVENT task.
     */
    public static boolean isEventTask(Task task) {
        return task != null && TaskType.TASK_TYPE_EVENT.equals(task.getTaskType());
    }

    /**
     * Returns the event sink (e.g., "sqs:queue-name", "kafka:topic-name"), or null if not set.
     */
    public String getSink() {
        return getInputString(SINK_INPUT);
    }

    /**
     * Returns the sink type (e.g., "sqs", "kafka", "nats"), or null if sink is not set.
     */
    public String getSinkType() {
        String sink = getSink();
        if (sink == null) {
            return null;
        }
        int colonIndex = sink.indexOf(':');
        return colonIndex > 0 ? sink.substring(0, colonIndex) : sink;
    }

    /**
     * Returns the sink target (e.g., queue name, topic name), or null/empty if sink is not set.
     */
    public String getSinkTarget() {
        String sink = getSink();
        if (sink == null) {
            return null;
        }
        int colonIndex = sink.indexOf(':');
        return colonIndex > 0 ? sink.substring(colonIndex + 1) : "";
    }

    /**
     * Returns the event payload (all input data except sink).
     */
    public Map<String, Object> getEventPayload() {
        Map<String, Object> inputData = task.getInputData();
        if (inputData == null || inputData.isEmpty()) {
            return Collections.emptyMap();
        }
        // The payload is typically the entire input except for the sink
        HashMap<String, Object> payload = new HashMap<>(inputData);
        payload.remove(SINK_INPUT);
        return payload;
    }

    /**
     * Returns the produced event details (available after the event is sent).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEventProduced() {
        Object produced = task.getOutputData().get(EVENT_PRODUCED_OUTPUT);
        if (produced instanceof Map) {
            return (Map<String, Object>) produced;
        }
        return Collections.emptyMap();
    }

    /**
     * Returns true if the event has been published.
     */
    public boolean isEventPublished() {
        return !getEventProduced().isEmpty();
    }
}
