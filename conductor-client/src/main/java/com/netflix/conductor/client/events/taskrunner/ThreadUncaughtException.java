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
package com.netflix.conductor.client.events.taskrunner;

import lombok.Getter;
import lombok.ToString;

/**
 * Published when a worker thread terminates with an uncaught exception (the
 * default {@link Thread.UncaughtExceptionHandler} path).
 *
 * The {@code taskType} is empty for threads that are not tied to a specific
 * worker (e.g. internal executor threads shared by the runner).
 */
@Getter
@ToString
public final class ThreadUncaughtException extends TaskRunnerEvent {
    private final Throwable cause;

    public ThreadUncaughtException(String taskType, Throwable cause) {
        super(taskType);
        this.cause = cause;
    }

    public ThreadUncaughtException(Throwable cause) {
        super("");
        this.cause = cause;
    }
}
