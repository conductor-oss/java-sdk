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

import java.util.Collections;
import java.util.Map;

/**
 * A tool call to execute before the LLM runs.
 *
 * <p>Passed to {@code Agent.Builder.prefillTools()} so the server executes these
 * tools before the first LLM turn and injects results into context.
 */
public class PrefillToolCall {
    private final String toolName;
    private final Map<String, Object> arguments;

    public PrefillToolCall(String toolName, Map<String, Object> arguments) {
        this.toolName = toolName;
        this.arguments = arguments != null ? arguments : Collections.emptyMap();
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Create a PrefillToolCall from a tool name and arguments.
     */
    public static PrefillToolCall of(String toolName, Map<String, Object> arguments) {
        return new PrefillToolCall(toolName, arguments);
    }
}
