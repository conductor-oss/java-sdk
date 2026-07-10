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
package org.conductoross.conductor.ai.model;

/**
 * Result returned from a guardrail function.
 */
public class GuardrailResult {
    private final boolean passed;
    private final String message;
    private final String fixedOutput;

    private GuardrailResult(boolean passed, String message, String fixedOutput) {
        this.passed = passed;
        this.message = message;
        this.fixedOutput = fixedOutput;
    }

    /** Create a passing guardrail result. */
    public static GuardrailResult pass() {
        return new GuardrailResult(true, null, null);
    }

    /** Create a failing guardrail result with a message. */
    public static GuardrailResult fail(String message) {
        return new GuardrailResult(false, message, null);
    }

    /** Create a result with fixed/replacement output. */
    public static GuardrailResult fix(String fixedOutput) {
        return new GuardrailResult(false, null, fixedOutput);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }

    public String getFixedOutput() {
        return fixedOutput;
    }

    @Override
    public String toString() {
        return "GuardrailResult{passed=" + passed + ", message=" + message + "}";
    }
}
