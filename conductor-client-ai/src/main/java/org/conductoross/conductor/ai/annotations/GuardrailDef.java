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

import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.enums.Position;

/**
 * Marks a method as a guardrail function.
 *
 * <p>Guardrail methods must accept a {@code String} argument (the content to check)
 * and return a {@link org.conductoross.conductor.ai.model.GuardrailResult}.
 *
 * <p>Example:
 * <pre>{@code
 * public class SafetyGuardrails {
 *     @GuardrailDef(name = "no_pii", position = Position.OUTPUT, onFail = OnFail.RAISE)
 *     public GuardrailResult noPii(String output) {
 *         if (output.contains("SSN")) {
 *             return GuardrailResult.fail("Output contains PII");
 *         }
 *         return GuardrailResult.pass();
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GuardrailDef {
    /** Guardrail name. Defaults to method name. */
    String name() default "";

    /** Whether to check the agent's input or output. */
    Position position() default Position.OUTPUT;

    /** What to do when the guardrail fails. */
    OnFail onFail() default OnFail.RAISE;

    /** Maximum number of retries when onFail is RETRY. */
    int maxRetries() default 3;
}
