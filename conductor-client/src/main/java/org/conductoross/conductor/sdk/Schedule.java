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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a {@link Workflow} method on a schedule. Registered with the Conductor scheduler when the
 * host starts, and updated in place on every start, so the annotation is the source of truth.
 * Repeat it for several schedules.
 *
 * <pre>{@code
 * @Schedule(cron = "0 0 9 * * MON-FRI", zone = "America/New_York", input = "{\"region\": \"us\"}")
 * @Workflow(name = "daily_report")
 * public Report report(ReportRequest request) { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Schedules.class)
public @interface Schedule {

    /** Spring cron expression with six fields, seconds first: {@code "0 0 9 * * MON-FRI"}. */
    String cron();

    /** Time zone the expression is evaluated in. */
    String zone() default "UTC";

    /** Schedule name; defaults to {@code <workflow>_schedule}, then {@code _2}, {@code _3}. */
    String name() default "";

    /** Workflow input for each run, as JSON. */
    String input() default "{}";

    /** Register the schedule paused. */
    boolean paused() default false;

    String description() default "";
}
