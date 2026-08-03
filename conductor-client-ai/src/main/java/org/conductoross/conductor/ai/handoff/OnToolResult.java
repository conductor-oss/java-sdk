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
package org.conductoross.conductor.ai.handoff;

/**
 * Triggers a handoff when a specific tool returns a result (optionally containing text).
 *
 * <p>Example:
 * <pre>{@code
 * OnToolResult.of("check_eligibility", "refund_specialist")
 * OnToolResult.of("check_eligibility", "refund_specialist", "eligible")
 * }</pre>
 */
public class OnToolResult extends Handoff {
    private final String toolName;
    private final String resultContains;

    public OnToolResult(String toolName, String target, String resultContains) {
        super(target);
        this.toolName = toolName;
        this.resultContains = resultContains;
    }

    public static OnToolResult of(String toolName, String target) {
        return new OnToolResult(toolName, target, null);
    }

    public static OnToolResult of(String toolName, String target, String resultContains) {
        return new OnToolResult(toolName, target, resultContains);
    }

    public String getToolName() {
        return toolName;
    }

    public String getResultContains() {
        return resultContains;
    }
}
