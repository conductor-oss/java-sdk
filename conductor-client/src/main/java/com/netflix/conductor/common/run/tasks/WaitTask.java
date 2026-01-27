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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskType;

/**
 * Typed wrapper for WAIT tasks providing convenient access to wait-specific properties.
 *
 * <p>WAIT tasks can be configured in three modes:
 * <ul>
 *   <li><b>Duration</b>: Wait for a specified time period (e.g., "30s", "5m")</li>
 *   <li><b>Until</b>: Wait until a specific date/time</li>
 *   <li><b>Signal</b>: Wait for an external signal (API call or event)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myWait");
 * WaitTask wait = new WaitTask(task);
 *
 * switch (wait.getWaitType()) {
 *     case DURATION:
 *         Duration d = wait.getDuration();
 *         break;
 *     case UNTIL:
 *         ZonedDateTime until = wait.getUntil();
 *         break;
 *     case SIGNAL:
 *         // waiting for external signal
 *         break;
 * }
 * }</pre>
 */
public class WaitTask extends TypedTask {

    public static final String DURATION_INPUT = "duration";
    public static final String UNTIL_INPUT = "until";

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    /**
     * Represents the type of wait condition.
     */
    public enum WaitType {
        /** Wait for a specified duration (e.g., "30s", "5m") */
        DURATION,
        /** Wait until a specific date/time */
        UNTIL,
        /** Wait for an external signal (API call or event) */
        SIGNAL
    }

    /**
     * Creates a WaitTask wrapper.
     *
     * @param task the underlying task to wrap
     * @throws IllegalArgumentException if task is null or not a WAIT task
     */
    public WaitTask(Task task) {
        super(task, TaskType.TASK_TYPE_WAIT);
    }

    /**
     * Checks if the given task is a WAIT task.
     */
    public static boolean isWaitTask(Task task) {
        return task != null && TaskType.TASK_TYPE_WAIT.equals(task.getTaskType());
    }

    /**
     * Returns the type of wait condition for this task.
     */
    public WaitType getWaitType() {
        if (isDurationBased()) {
            return WaitType.DURATION;
        } else if (isUntilBased()) {
            return WaitType.UNTIL;
        }
        return WaitType.SIGNAL;
    }

    /**
     * Returns true if this wait task uses a duration.
     */
    public boolean isDurationBased() {
        return hasInput(DURATION_INPUT);
    }

    /**
     * Returns true if this wait task uses an until timestamp.
     */
    public boolean isUntilBased() {
        return hasInput(UNTIL_INPUT);
    }

    /**
     * Returns true if this wait task waits for an external signal.
     */
    public boolean isSignalBased() {
        return !isDurationBased() && !isUntilBased();
    }

    /**
     * Returns the raw duration string (e.g., "30s", "5m", "1h"), or null if not set.
     */
    public String getDurationString() {
        return getInputString(DURATION_INPUT);
    }

    /**
     * Parses and returns the duration, or null if not set or invalid.
     * Supports formats: "30s" (seconds), "5m" (minutes), "1h" (hours), "1d" (days)
     */
    public Duration getDuration() {
        String durationStr = getDurationString();
        return durationStr != null ? parseDuration(durationStr) : null;
    }

    /**
     * Returns the raw until string, or null if not set.
     */
    public String getUntilString() {
        return getInputString(UNTIL_INPUT);
    }

    /**
     * Parses and returns the until timestamp, or null if not set or invalid.
     */
    public ZonedDateTime getUntil() {
        String untilStr = getUntilString();
        return untilStr != null ? parseDateTime(untilStr) : null;
    }

    private Duration parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return null;
        }
        durationStr = durationStr.trim().toLowerCase();

        if (durationStr.length() < 2) {
            // Assume seconds if just a number
            try {
                return Duration.ofSeconds(Long.parseLong(durationStr));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        char unit = durationStr.charAt(durationStr.length() - 1);
        String valueStr = durationStr.substring(0, durationStr.length() - 1);

        try {
            long value = Long.parseLong(valueStr);
            switch (unit) {
                case 's':
                    return Duration.ofSeconds(value);
                case 'm':
                    return Duration.ofMinutes(value);
                case 'h':
                    return Duration.ofHours(value);
                case 'd':
                    return Duration.ofDays(value);
                default:
                    // If the last char is a digit, assume the whole string is seconds
                    if (Character.isDigit(unit)) {
                        return Duration.ofSeconds(Long.parseLong(durationStr));
                    }
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ZonedDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try ISO format as fallback
            try {
                return ZonedDateTime.parse(dateTimeStr);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
