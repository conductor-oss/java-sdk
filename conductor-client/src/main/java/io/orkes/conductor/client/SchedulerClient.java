/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client;

import java.util.List;

import com.netflix.conductor.common.model.BulkResponse;

import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.SearchResultWorkflowScheduleExecution;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.WorkflowSchedule;

/**
 * Client for managing workflow schedules.
 * <p>
 * Schedules allow workflows to be automatically triggered based on cron
 * expressions or fixed-rate intervals.
 * This interface provides methods to:
 * <ul>
 * <li>Create, update, and delete schedules</li>
 * <li>Pause and resume schedules individually or in bulk</li>
 * <li>Search schedule executions</li>
 * <li>Manage schedule tags</li>
 * <li>Preview upcoming schedule executions</li>
 * </ul>
 */
public interface SchedulerClient {

    // Schedule CRUD operations

    /**
     * Creates or updates a workflow schedule.
     * <p>
     * Schedules can be triggered based on:
     * <ul>
     * <li>Cron expressions for time-based scheduling</li>
     * <li>Fixed-rate intervals</li>
     * </ul>
     *
     * @param saveScheduleRequest the schedule configuration including cron
     *                            expression, workflow details, and execution
     *                            parameters
     */
    void saveSchedule(SaveScheduleRequest saveScheduleRequest);

    /**
     * Retrieves a specific schedule by name.
     *
     * @param name the name of the schedule
     * @return the schedule configuration
     */
    WorkflowSchedule getSchedule(String name);

    /**
     * Retrieves all schedules, optionally filtered by workflow name.
     *
     * @param workflowName the name of the workflow to filter by, or null to get all
     *                     schedules
     * @return list of schedules matching the filter
     */
    List<WorkflowSchedule> getAllSchedules(String workflowName);

    /**
     * Deletes a schedule by name.
     * <p>
     * This prevents future executions but does not affect workflows already in
     * progress.
     *
     * @param name the name of the schedule to delete
     */
    void deleteSchedule(String name);

    // Schedule control operations

    /**
     * Pauses a specific schedule, preventing new workflow executions.
     * <p>
     * Workflows already in progress will continue to execute.
     *
     * @param name the name of the schedule to pause
     */
    void pauseSchedule(String name);

    /**
     * Resumes a paused schedule, allowing new workflow executions.
     *
     * @param name the name of the schedule to resume
     */
    void resumeSchedule(String name);

    /**
     * Pauses all schedules on the server.
     * <p>
     * This is typically used for maintenance or debugging and affects all schedules
     * globally.
     */
    void pauseAllSchedules();

    /**
     * Resumes all previously paused schedules on the server.
     */
    void resumeAllSchedules();

    // Bulk operations

    /**
     * Pauses multiple schedules in a single bulk operation.
     *
     * @param schedulerIds list of schedule names to pause
     * @return bulk response with success/failure status for each schedule
     */
    BulkResponse pauseSchedulers(List<String> schedulerIds);

    /**
     * Resumes multiple schedules in a single bulk operation.
     *
     * @param schedulerIds list of schedule names to resume
     * @return bulk response with success/failure status for each schedule
     */
    BulkResponse resumeSchedulers(List<String> schedulerIds);

    // Utility operations

    /**
     * Calculates and returns the next few execution times for a cron expression.
     * <p>
     * This is useful for validating cron expressions and previewing when scheduled
     * workflows will run.
     *
     * @param cronExpression    the cron expression to evaluate
     * @param scheduleStartTime optional start time in epoch milliseconds (null for
     *                          now)
     * @param scheduleEndTime   optional end time in epoch milliseconds (null for
     *                          unbounded)
     * @param limit             maximum number of execution times to return (default
     *                          is 3, max is 5)
     * @return list of epoch milliseconds representing the next execution times
     */
    List<Long> getNextFewSchedules(String cronExpression, Long scheduleStartTime, Long scheduleEndTime, Integer limit);

    /**
     * Requeues all execution records for processing.
     * <p>
     * This is an administrative operation typically used for recovery scenarios.
     */
    void requeueAllExecutionRecords();

    // Search operations

    /**
     * Searches for schedule executions with pagination support.
     * <p>
     * Supports free-text search and structured queries to find specific schedule
     * executions.
     *
     * @param start    starting index for pagination (0-based)
     * @param size     number of results to return
     * @param sort     sort order (e.g., "startTime:DESC")
     * @param freeText free-text search query
     * @param query    structured query string for advanced filtering
     * @return paginated search results containing schedule execution information
     */
    SearchResultWorkflowScheduleExecution search(Integer start, Integer size, String sort, String freeText,
            String query);

    // Tag management

    /**
     * Adds or updates tags for a schedule.
     * <p>
     * Tags are useful for organizing and categorizing schedules.
     *
     * @param body list of tags to add or update
     * @param name the name of the schedule
     */
    void setSchedulerTags(List<TagObject> body, String name);

    /**
     * Deletes specific tags from a schedule.
     *
     * @param body list of tags to delete
     * @param name the name of the schedule
     */
    void deleteSchedulerTags(List<TagObject> body, String name);

    /**
     * Retrieves all tags associated with a schedule.
     *
     * @param name the name of the schedule
     * @return list of tags for the schedule
     */
    List<TagObject> getSchedulerTags(String name);
}
