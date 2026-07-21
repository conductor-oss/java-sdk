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
package org.conductoross.conductor.ai.execution;

/** The result of a code execution. */
public class ExecutionResult {

    private final String output;
    private final String error;
    private final int exitCode;
    private final boolean timedOut;

    public ExecutionResult(String output, String error, int exitCode, boolean timedOut) {
        this.output = output != null ? output : "";
        this.error = error != null ? error : "";
        this.exitCode = exitCode;
        this.timedOut = timedOut;
    }

    /** Standard output from the execution. */
    public String getOutput() {
        return output;
    }

    /** Standard error output (if any). */
    public String getError() {
        return error;
    }

    /** Process exit code (0 = success). */
    public int getExitCode() {
        return exitCode;
    }

    /** {@code true} if execution was killed due to timeout. */
    public boolean isTimedOut() {
        return timedOut;
    }

    /** {@code true} if the execution succeeded (exit code 0, no timeout). */
    public boolean isSuccess() {
        return exitCode == 0 && !timedOut;
    }
}
