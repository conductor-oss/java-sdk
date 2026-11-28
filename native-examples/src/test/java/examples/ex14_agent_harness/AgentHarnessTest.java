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
package examples.ex14_agent_harness;

import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.McpCall.McpResult;
import org.conductoross.conductor.sdk.McpCall.McpTool;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import examples.ex14_agent_harness.AgentHarness.Approval;
import examples.ex14_agent_harness.AgentHarness.Goal;
import examples.ex14_agent_harness.AgentHarness.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The model's steps are given by the test, one per pass of the loop. */
class AgentHarnessTest {

    private static final Goal GOAL = new Goal("Plan a day in Oslo", "http://mcp", "http://agent", 10);

    private final DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(AgentHarness.class);

    @Test
    void carriesOutEachStepTheModelChooses() {
        env.mcp("list_tools").respond(List.of(new McpTool("get_weather", "weather", Map.of())));
        env.llm("plan").respond("{\"action\": \"tool\", \"tool\": \"get_weather\", \"args\": {\"city\": \"Oslo\"}}");
        env.mcp("call_tool").respond(new McpResult(List.of(Map.of("type", "text", "text", "Rain")), false));
        env.llm("plan_2").respond("{\"action\": \"delegate\", \"task\": \"find museums\"}");
        env.llm("plan_3").respond("{\"action\": \"ask_human\", \"question\": \"Buy tickets?\"}");
        env.human("ask_user").answer(new Approval(true, "yes please"));
        env.llm("plan_4").respond("{\"action\": \"finish\", \"answer\": \"too early\"}");
        env.agent("helper").respond(new AgentResult("the Munch museum", "completed", List.of(), "t", "c"));
        env.llm("plan_5").respond("{\"action\": \"finish\", \"answer\": \"Munch museum, bring an umbrella\"}");

        TestRun<Outcome> run = env.start(AgentHarness::pursue, GOAL);

        assertEquals(new Outcome("Munch museum, bring an umbrella", 5, List.of(
                "tool get_weather: Rain",
                "delegated: find museums",
                "person answered: yes, yes please",
                "helper found: the Munch museum")), run.result());
    }

    @Test
    void stopsWhenTheStepsRunOut() {
        env.mcp("list_tools").respond(List.of());
        env.llm("plan").respond("{\"action\": \"wander\"}");
        env.llm("plan_2").respond("{\"action\": \"wander\"}");

        Outcome outcome = env.start(AgentHarness::pursue, new Goal("Plan", "http://mcp", "http://agent", 2)).result();

        assertEquals("no answer within 2 steps", outcome.answer());
        assertEquals(List.of("unknown action: wander", "unknown action: wander"), outcome.observations());
    }
}
