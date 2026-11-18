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
package org.conductoross.conductor.sdk.internal;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** An agent handle is a conversation: asking again continues it. */
class AgentConversationTest {

    @Test
    void askingAgainContinuesTheConversationAndAnswersAPendingQuestion() {
        List<Command> sent = new ArrayList<>();
        Deque<AgentResult> replies = new ArrayDeque<>(List.of(
                new AgentResult("Card or store credit?", "input-required", List.of(), "task-1", "ctx-1"),
                new AgentResult("Refunded to the card", "completed", List.of(), "task-1", "ctx-1"),
                new AgentResult("You are welcome", "message", List.of(), null, "ctx-1")));
        Engine engine = (Engine) Proxy.newProxyInstance(Engine.class.getClassLoader(), new Class<?>[] {Engine.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("call")) {
                        sent.add((Command) args[0]);
                        return replies.poll();
                    }
                    if (method.getReturnType() == void.class) {
                        return null; // checks such as assertRunning
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        Engine previous = Engine.CURRENT.get();
        Engine.CURRENT.set(engine);
        try {
            AgentCall support = Conductor.agent("support").remote("http://agent/a2a").header("Authorization", "Bearer bot");
            support.ask("Refund ticket T-1");
            support.ask("card");
            support.ask("Thanks");
        } finally {
            Engine.CURRENT.set(previous);
        }

        assertEquals(3, sent.size());
        for (Command c : sent) {
            assertEquals("support", c.explicitRef);
            assertEquals("http://agent/a2a", c.input.get("agentUrl"));
            assertEquals(Map.of("Authorization", "Bearer bot"), c.input.get("headers"));
        }
        // the first message starts the conversation
        assertNull(sent.get(0).input.get("contextId"));
        assertNull(sent.get(0).input.get("taskId"));
        // the agent asked for input: the next message answers it, in its task
        assertEquals("ctx-1", sent.get(1).input.get("contextId"));
        assertEquals("task-1", sent.get(1).input.get("taskId"));
        // that task is done: the next message is a new turn of the same conversation
        assertEquals("ctx-1", sent.get(2).input.get("contextId"));
        assertNull(sent.get(2).input.get("taskId"));
    }

    public static class Chat {
        @Workflow(name = "agent_chat")
        public String run(String ticket) {
            AgentCall support = Conductor.agent("support").remote("http://agent/a2a");
            support.ask("Refund " + ticket);
            return support.ask("Thanks").text();
        }
    }

    @Test
    void eachAskOnAConversationIsDrawnAsAnAgentStepNamedByTheConversation() {
        List<Map<String, Object>> tasks = Replayer.discover(WorkflowSpec.of(Chat.class).get(0));
        List<Object> agents = tasks.stream().filter(t -> "AGENT".equals(t.get("type"))).map(t -> t.get("taskReferenceName")).toList();
        assertEquals(List.of("support", "support_2"), agents);
    }
}
