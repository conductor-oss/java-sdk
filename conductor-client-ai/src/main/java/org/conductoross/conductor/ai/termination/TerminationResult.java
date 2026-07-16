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
package org.conductoross.conductor.ai.termination;

/**
 * The result of evaluating a termination condition.
 *
 * <pre>{@code
 * TerminationResult result = new TerminationResult(true, "Max messages reached");
 * if (result.isShouldTerminate()) { ... }
 * }</pre>
 */
public class TerminationResult {

    private final boolean shouldTerminate;
    private final String reason;

    public TerminationResult(boolean shouldTerminate) {
        this(shouldTerminate, null);
    }

    public TerminationResult(boolean shouldTerminate, String reason) {
        this.shouldTerminate = shouldTerminate;
        this.reason = reason;
    }

    public static TerminationResult stop(String reason) {
        return new TerminationResult(true, reason);
    }

    public static TerminationResult continueRunning() {
        return new TerminationResult(false);
    }

    public boolean isShouldTerminate() {
        return shouldTerminate;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "TerminationResult{shouldTerminate=" + shouldTerminate + ", reason='" + reason + "'}";
    }
}
