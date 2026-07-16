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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversation memory for stateful / multi-turn agents.
 *
 * <p>Holds accumulated messages and an optional cap. Attached to an agent via
 * {@code Agent.builder().memory(...)}; serialized to the server's {@code MemoryConfig}
 * as {@code {messages, maxMessages}}. Mirrors the Python SDK's {@code ConversationMemory}.
 *
 * <p>Each message is a map of the shape {@code {"role": "user"|"assistant"|"system",
 * "message": "<text>"}}.
 *
 * <pre>{@code
 * ConversationMemory memory = new ConversationMemory(20)   // keep the last 20 messages
 *     .addSystem("You are a concise assistant.")
 *     .addUser("Hello");
 *
 * Agent agent = Agent.builder().name("chat").model("anthropic/claude-sonnet-4-6").memory(memory).build();
 * }</pre>
 */
public class ConversationMemory {

    private final List<Map<String, Object>> messages;
    private final Integer maxMessages;

    /** Empty memory with no cap. */
    public ConversationMemory() {
        this(null);
    }

    /**
     * Empty memory that retains at most {@code maxMessages} messages (oldest trimmed server-side).
     *
     * @param maxMessages maximum messages to retain, or {@code null} for unbounded
     */
    public ConversationMemory(Integer maxMessages) {
        this.messages = new ArrayList<>();
        this.maxMessages = maxMessages;
    }

    /** Append a user message. Returns {@code this} for chaining. */
    public ConversationMemory addUser(String content) {
        return add("user", content);
    }

    /** Append an assistant message. Returns {@code this} for chaining. */
    public ConversationMemory addAssistant(String content) {
        return add("assistant", content);
    }

    /** Append a system message. Returns {@code this} for chaining. */
    public ConversationMemory addSystem(String content) {
        return add("system", content);
    }

    private ConversationMemory add(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("message", content);
        messages.add(m);
        return this;
    }

    /** The accumulated messages (mutable backing list). */
    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    /** Maximum messages to retain, or {@code null} for unbounded. */
    public Integer getMaxMessages() {
        return maxMessages;
    }

    /** Remove all messages. */
    public void clear() {
        messages.clear();
    }
}
