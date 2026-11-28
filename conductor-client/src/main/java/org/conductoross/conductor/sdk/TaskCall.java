/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.sdk;

import java.util.Map;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Json;

/** A task called by name, for task names known only at runtime or owned by another team. */
public final class TaskCall {

    private final Command command;

    TaskCall(String name) {
        this.command = new Command(Command.Kind.SIMPLE, name);
    }

    public TaskCall ref(String taskReferenceName) {
        command.explicitRef = taskReferenceName;
        return this;
    }

    public TaskCall input(String key, Object value) {
        command.input.put(key, Json.plain(value));
        return this;
    }

    public TaskCall input(Map<String, ?> input) {
        input.forEach(this::input);
        return this;
    }

    /** Calls the task and returns its output as {@code type}. */
    public <T> T as(Class<T> type) {
        return type.cast(call(type));
    }

    /** Calls the task and returns its output as a map. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() {
        return (Map<String, Object>) call(Map.class);
    }

    /** Calls the task, for its effect. */
    public void run() {
        call(Void.class);
    }

    private Object call(Class<?> type) {
        Command c = command.copy();
        c.resultType = type;
        return Engine.current().call(c);
    }
}
