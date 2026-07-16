/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an agent tool.
 *
 * <p>Use this annotation on methods in a class to register them as callable tools
 * for an agent. Use {@link org.conductoross.conductor.ai.internal.ToolRegistry#fromInstance(Object)}
 * to discover all annotated methods.
 *
 * <p>Example:
 * <pre>{@code
 * public class WeatherTools {
 *     @Tool(name = "get_weather", description = "Get weather for a city")
 *     public String getWeather(String city) {
 *         return "Sunny, 72F in " + city;
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {
    /** Tool name. Defaults to method name if not specified. */
    String name() default "";

    /** Human-readable description of what the tool does. */
    String description() default "";

    /** If true, the tool requires human approval before execution. */
    boolean approvalRequired() default false;

    /** If true, the tool is served externally (not by this SDK). */
    boolean external() default false;

    /** Maximum execution time in seconds. 0 means no explicit timeout (server default applies). */
    int timeoutSeconds() default 0;

    /** Maximum number of times this tool can be called. 0 means unlimited. */
    int maxCalls() default 0;

    /** Credential environment variable names required by this tool. */
    String[] credentials() default {};

    /** Number of times Conductor retries the task on failure. Default matches SDK default of 2. */
    int retryCount() default 2;

    /** Seconds between retries. Default matches SDK default of 2. */
    int retryDelaySeconds() default 2;

    /** Retry strategy: "fixed", "linear_backoff", or "exponential_backoff". */
    String retryPolicy() default "linear_backoff";
}
