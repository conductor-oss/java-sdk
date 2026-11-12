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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a native workflow. Any method of any class can be a workflow: its body is
 * ordinary Java, and every static call imported from {@link Conductor} goes to the server and shows
 * in the workflow graph.
 *
 * <p>The class must have a no-argument constructor, or be registered with a factory. A new instance
 * serves each execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Workflow {

    /** Workflow name; defaults to the method name in snake case. */
    String name() default "";

    int version() default 1;

    String ownerEmail() default "native@conductor.local";

    /** Execution timeout as an ISO-8601 duration such as {@code PT2H} or {@code P3D}; empty for none. */
    String timeout() default "";

    String description() default "";

    /**
     * A workflow started when this one fails or times out, with the failed execution's id, status
     * and reason as input. Any registered workflow, native or JSON.
     */
    String failureWorkflow() default "";

    /** Version of the failure workflow; 0 means the latest. */
    int failureWorkflowVersion() default 0;
}
