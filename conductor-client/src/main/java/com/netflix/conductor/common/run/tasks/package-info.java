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

/**
 * Typed task wrappers for convenient access to task-specific properties.
 *
 * <p>This package provides typed wrappers around the runtime
 * {@link com.netflix.conductor.common.metadata.tasks.Task} class, offering type-safe
 * accessors for task-specific input and output data.
 *
 * <h2>Usage</h2>
 *
 * <p>There are two ways to create typed tasks:
 *
 * <h3>1. Using the constructor:</h3>
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myWait");
 * WaitTask wait = new WaitTask(task);  // throws if not a WAIT task
 * }</pre>
 *
 * <h3>2. Using Task.as():</h3>
 * <pre>{@code
 * Task task = workflow.getTaskByRefName("myWait");
 * WaitTask wait = task.as(WaitTask.class);  // throws if not a WAIT task
 * }</pre>
 *
 * <h2>Available Typed Tasks</h2>
 *
 * <ul>
 *   <li>{@link com.netflix.conductor.common.run.tasks.WaitTask} - WAIT task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.HttpTask} - HTTP task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.SwitchTask} - SWITCH task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.SubWorkflowTask} - SUB_WORKFLOW task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.EventTask} - EVENT task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.TerminateTask} - TERMINATE task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.DoWhileTask} - DO_WHILE task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.ForkJoinTask} - FORK_JOIN task wrapper</li>
 *   <li>{@link com.netflix.conductor.common.run.tasks.HumanTask} - HUMAN task wrapper</li>
 * </ul>
 *
 * @see com.netflix.conductor.common.run.tasks.TypedTask
 * @see com.netflix.conductor.common.metadata.tasks.Task#as(Class)
 */
package com.netflix.conductor.common.run.tasks;
