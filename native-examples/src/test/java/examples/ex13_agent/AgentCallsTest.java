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
package examples.ex13_agent;

import java.util.List;

import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import examples.ex13_agent.AgentCalls.Request;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentCallsTest {

    @Test
    void aConversationAndALongPieceOfWork() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(AgentCalls.class);
        env.agent("research").respond(reply("three similar orders"));
        env.agent("support").respond(reply("a duplicate payment"));
        env.agent("support_2").respond(reply("refund the second charge"));

        List<String> answers = env.start(AgentCalls::ask, new Request("http://agent", "Why?")).result();

        assertEquals(List.of("a duplicate payment", "refund the second charge", "three similar orders"), answers);
    }

    private static AgentResult reply(String text) {
        return new AgentResult(text, "completed", List.of(), "task-1", "context-1");
    }
}
