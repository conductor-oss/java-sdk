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
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Entries;

/**
 * A conversation with an AI agent: a remote agent over the A2A protocol, or a Conductor agent by
 * name. Each {@link #ask} is an AGENT task: the server sends the message, waits for the agent, and
 * records the reply; the workflow is suspended meanwhile, holding no thread.
 *
 * <pre>{@code
 * AgentCall support = agent("support").remote(url);
 * AgentResult reply = support.ask("Refund ticket T-1");
 * if (reply.needsInput()) {
 *     reply = support.ask(answer);          // answers the agent's question, in the same agent task
 * }
 * support.ask("Anything else?");             // the next turn of the same conversation
 * }</pre>
 *
 * <p>Asking again continues the conversation: the next message carries the conversation's id, and,
 * when the agent asked for input, the id of the agent task it answers. A new {@code agent(..)}
 * starts a new conversation. Use one conversation from one lane at a time.
 *
 * <p>An agent may work for hours, and the workflow waits as long as it takes: the server polls the
 * agent's task every {@link #pollInterval} (5 seconds by default), or, with {@link
 * #pushNotifications}, waits for the agent to call back, for at most {@link #maxDuration} (24
 * hours by default). To keep working meanwhile, ask inside {@code async(() -> ...)}, as with any
 * other call, and {@code join} its Future when the answer is needed.
 */
public final class AgentCall {

    /**
     * What the agent returned. {@code state} is the agent task's state: {@code completed}, {@code
     * input-required} or {@code auth-required} when the agent waits for an answer, or {@code
     * message} for a direct reply with no agent task. {@code artifacts} are what the agent
     * produced, each with its {@code parts} (text, or {@code data} for structured output); {@code
     * text} joins their text parts.
     */
    public record AgentResult(String text, String state, List<Map<String, Object>> artifacts, String taskId, String contextId) {

        /** Whether the agent is waiting for an answer: the conversation's next {@link #ask} sends it. */
        public boolean needsInput() {
            return "input-required".equals(state) || "auth-required".equals(state);
        }
    }

    private final String ref;
    /** What every message of the conversation carries: the agent, headers and how to wait. */
    private final Map<String, Object> input = new LinkedHashMap<>();
    private int retries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);
    private String contextId;
    /** The agent task waiting for an answer, which the next message answers. */
    private String waitingTaskId;

    AgentCall(String ref) {
        this.ref = ref;
    }

    /**
     * A remote A2A agent: its JSON-RPC endpoint, or its agent card URL ending in {@code .json}, from
     * which the endpoint is discovered.
     */
    public AgentCall remote(String agentUrl) {
        input.put("agentType", "a2a");
        input.put("agentUrl", agentUrl);
        return this;
    }

    /** A Conductor agent by name and version. */
    public AgentCall conductor(String agentName, int version) {
        input.put("agentType", "conductor");
        input.put("agentName", agentName);
        input.put("agentVersion", version);
        return this;
    }

    public AgentCall header(String name, String value) {
        @SuppressWarnings("unchecked")
        Map<String, String> headers =
                (Map<String, String>) input.computeIfAbsent("headers", k -> new LinkedHashMap<String, String>());
        headers.put(name, value);
        return this;
    }

    /** Continues a conversation started elsewhere, such as in an earlier execution. */
    public AgentCall context(String contextId) {
        this.contextId = contextId;
        return this;
    }

    /**
     * Longest time to wait for a long-running agent task, 24 hours by default; past it the remote
     * task is cancelled and the call fails.
     */
    public AgentCall maxDuration(Duration duration) {
        input.put("maxDurationSeconds", (int) Math.max(1, duration.toSeconds()));
        return this;
    }

    /** How often the server asks a long-running agent task how it is doing; 5 seconds by default. */
    public AgentCall pollInterval(Duration interval) {
        input.put("pollIntervalSeconds", (int) Math.max(1, interval.toSeconds()));
        return this;
    }

    /**
     * Asks the agent to call the server back when its task changes, instead of being polled; the
     * server still polls rarely ({@code backstop}), in case a callback is lost. Needs {@code
     * conductor.a2a.callback.url} on the server, the URL the agent reaches it at; without it the
     * server polls as usual. Give the agent's JSON-RPC endpoint to {@link #remote}, not its card
     * URL: on a callback the server reads the agent's task from that URL as given.
     */
    public AgentCall pushNotifications(Duration backstop) {
        input.put("pushNotification", true);
        input.put("pushBackstopPollSeconds", (int) Math.max(1, backstop.toSeconds()));
        return this;
    }

    /**
     * Retries after the agent fails, with exponential backoff from {@code delay}; default 3 and 1s.
     * The server retries an AI task at least once: a count below 1 becomes 3, with linear backoff
     * from 2 seconds ({@code AIModelTaskMapper}).
     */
    public AgentCall retries(int count, Duration delay) {
        this.retries = count;
        this.retryDelay = delay;
        return this;
    }

    /**
     * Sends the message, in this conversation, and waits for the agent's reply. When the reply
     * {@link AgentResult#needsInput() needs input}, the next {@code ask} answers it.
     */
    public AgentResult ask(String text) {
        Command command = new Command(Command.Kind.AGENT, ref);
        command.explicitRef = ref;
        command.input.putAll(input);
        if (input.get("headers") instanceof Map<?, ?> headers) {
            command.input.put("headers", new LinkedHashMap<>(headers)); // a later header() changes only later asks
        }
        command.input.put("text", text);
        if (contextId != null) {
            command.input.put("contextId", contextId);
        }
        if (waitingTaskId != null) {
            command.input.put("taskId", waitingTaskId);
        }
        command.fields.put("taskDefinition", Entries.retryingDefinition(ref, retries, Math.max(1, retryDelay.toSeconds())));
        command.resultType = AgentResult.class;
        AgentResult reply = (AgentResult) Engine.current().call(command);
        if (reply != null) {
            if (reply.contextId() != null) {
                contextId = reply.contextId();
            }
            waitingTaskId = reply.needsInput() ? reply.taskId() : null;
        }
        return reply;
    }
}
