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
package com.netflix.conductor.common.metadata.tasks;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * The continuation of a native (code-defined) workflow that runs when this task finishes.
 *
 * <p>Native workflow code pauses only on a task. The task it paused on carries this marker: whether
 * the continuation is leased to a worker, how many times it was offered again, and, once applied,
 * what it produced, which a worker replays from. Which continuation it is (the lane of the code and
 * its number) is the task's entry's {@code metadata}: {@code lane} and {@code continuation}.
 * Whether it can run follows from the task: once the task has ended and the decider has evaluated
 * it. The workflow's first continuation and its handler runs have no task to wait on; the workflow
 * record carries their markers (see {@code NativeState}).
 */
@Getter
@Setter
public class TaskContinuation {

    /**
     * Leased to a worker: offered, and not yet applied or handed back. At most one continuation of
     * an execution is leased at a time.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean leased;

    /**
     * How many times this continuation was offered again: 0 on its first offer, one more after each
     * attempt that did not apply (deferred, retried, rerun), as a task's retry count. A completion
     * names the count it was offered at, so one from an earlier attempt never applies.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int retryCount;

    /**
     * When it applied, 0 until then: later than every earlier apply in the run, the order replay
     * follows.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private long endTime;

    /** Whether it ended its lane: the code returned or raised there. */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean done;

    /** The values the code recorded while it ran ({@code once}, {@code now}, {@code random}). */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> recorded = new HashMap<>();

    /** The workflow variables it started from, which a replay restores before it. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> resumedWith = new HashMap<>();

    /**
     * The handler lane's run number, from 1; 0 on every other marker, whose number is its entry's.
     * Every handler run has the same message id, so this tells a run from the one before it.
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int n;

    /**
     * On the marker a poll hands out, never stored: the execution's stamp, which names the
     * completion that applied last. A worker that kept the code parked in memory continues from
     * there only when this is still the stamp it sent.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String stamp;

    /**
     * On the marker a poll hands out, never stored: the workflow's variables as the continuation
     * starts. A server that sends them pins executions to the worker that holds them in memory.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> variables;

    @Override
    public String toString() {
        return "TaskContinuation{"
                + (n > 0 ? "run " + n + " " : "")
                + (leased ? "leased" : endTime > 0 ? "applied" : "not applied")
                + (retryCount > 0 ? " retry " + retryCount : "")
                + "}";
    }
}
