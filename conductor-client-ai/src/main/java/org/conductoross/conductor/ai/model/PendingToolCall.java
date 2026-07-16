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
 * A single tool call awaiting human approval inside a {@link AgentEvent}
 * of type {@code waiting}.
 *
 * <p>One HUMAN task gates a whole batch of tool calls with a single
 * {@code {approved, reason}} verdict — the array on the event is the
 * load-bearing field. Iterate it to see every tool the LLM proposed in
 * this turn.
 */
public final class PendingToolCall {

    private final String name;
    private final Map<String, Object> args;

    public PendingToolCall(String name, Map<String, Object> args) {
        this.name = name;
        this.args = args != null ? args : Collections.emptyMap();
    }

    /** The tool's registered name (e.g. {@code "publish_article"}). */
    public String getName() {
        return name;
    }

    /** The LLM-generated arguments for this tool call. */
    public Map<String, Object> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "PendingToolCall{name=" + name + ", args=" + args + "}";
    }
}
