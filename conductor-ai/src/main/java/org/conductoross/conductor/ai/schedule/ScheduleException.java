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
package org.conductoross.conductor.ai.schedule;

/** Base class for schedule errors. */
public class ScheduleException extends RuntimeException {
    public ScheduleException(String message) {
        super(message);
    }

    public ScheduleException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Two schedules in the same agent share a name. */
    public static class NameConflict extends ScheduleException {
        public NameConflict(String message) {
            super(message);
        }
    }

    /** No schedule matches the given name. */
    public static class NotFound extends ScheduleException {
        public NotFound(String message) {
            super(message);
        }
    }

    /** Server rejected the cron expression as malformed. */
    public static class InvalidCron extends ScheduleException {
        public InvalidCron(String message) {
            super(message);
        }
    }

    /** A {@code runNow(..., wait=true)} workflow did not finish within the timeout. */
    public static class Timeout extends ScheduleException {
        public Timeout(String message) {
            super(message);
        }
    }
}
