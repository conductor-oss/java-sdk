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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Entries;

/**
 * Calls an AI agent from the Conductor server (an AGENT task): a remote agent over the A2A
 * protocol, or a Conductor agent by name. The server holds the conversation, polls long-running
 * agent tasks, and records the result; the workflow waits without a worker.
 */
public final class AgentCall {

    /** What the agent returned: its text reply, final state, and any structured output. */
    public record AgentResult(String text, String state, Map<String, Object> output, String taskId, String contextId) {}

    private final Command command;
    private int retries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);

    AgentCall(String ref) {
        command = new Command(Command.Kind.AGENT, ref, ref);
        command.explicitRef = ref;
    }

    /**
     * A remote A2A agent: its JSON-RPC endpoint, or its agent card URL ending in {@code .json}, from
     * which the endpoint is discovered.
     */
    public AgentCall remote(String agentUrl) {
        command.input.put("agentType", "a2a");
        command.input.put("agentUrl", agentUrl);
        return this;
    }

    /** A Conductor agent by name and version. */
    public AgentCall conductor(String agentName, int version) {
        command.input.put("agentType", "conductor");
        command.input.put("agentName", agentName);
        command.input.put("agentVersion", version);
        return this;
    }

    public AgentCall header(String name, String value) {
        @SuppressWarnings("unchecked")
        Map<String, String> headers =
                (Map<String, String>) command.input.computeIfAbsent("headers", k -> new LinkedHashMap<String, String>());
        headers.put(name, value);
        return this;
    }

    /** Continues an earlier conversation with the agent. */
    public AgentCall context(String contextId) {
        command.input.put("contextId", contextId);
        return this;
    }

    /** Longest time to wait for a long-running agent task. */
    public AgentCall maxDuration(Duration duration) {
        command.input.put("maxDurationSeconds", (int) duration.toSeconds());
        return this;
    }

    public AgentCall retries(int count, Duration delay) {
        this.retries = count;
        this.retryDelay = delay;
        return this;
    }

    /** Sends the message and waits for the agent's result. */
    public AgentResult ask(String text) {
        command.input.put("text", text);
        command.fields.put(
                "taskDefinition", Entries.retryingDefinition(command.name, retries, Math.max(1, retryDelay.toSeconds())));
        command.resultType = AgentResult.class;
        return (AgentResult) Engine.current().call(command);
    }
}
