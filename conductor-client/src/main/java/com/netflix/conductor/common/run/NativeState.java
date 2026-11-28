/*
 * Copyright 2021 Conductor Authors.
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
package com.netflix.conductor.common.run;

import java.util.HashMap;
import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.TaskContinuation;

import lombok.Getter;
import lombok.Setter;

/**
 * Engine state of a native (code-defined) workflow that has no task to live on. Everything else
 * about the code's progress is on the tasks it paused on ({@link TaskContinuation}), so this stays
 * small however long the execution runs. The code type and the continuation timeout are the
 * definition's {@code metadata.native}, which the execution carries.
 */
@Getter
@Setter
public class NativeState {

    /**
     * The main lane's first continuation, which runs before any task exists. Always {@code main}
     * #1, so neither is stored.
     */
    private TaskContinuation bootstrap;

    /**
     * The handler lane: signal and update handlers schedule no tasks. Null without handlers. Its
     * lane is always {@code handlers}, so it is not stored; its number counts its runs.
     */
    private TaskContinuation handlers;
    private Map<String, Object> replies = new HashMap<>();

    /**
     * The task domain of the worker that keeps this execution's code parked in memory between
     * continuations; null when no worker does.
     */
    private String pinnedTo;

    /** Names the completion that applied last, as its worker chose. */
    private String stamp;
}
