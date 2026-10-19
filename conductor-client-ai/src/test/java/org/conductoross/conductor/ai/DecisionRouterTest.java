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
package org.conductoross.conductor.ai;

import java.util.Map;

import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.internal.AgentConfigSerializer;
import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionRouterTest {
    private static final Map<String, Object> QUESTIONS = Map.of("agent", Map.of("type", "choice",
            "choices", Map.of("billing", "Payments", "technical", "Troubleshooting")));

    private Agent router(ToolDef tool) {
        return Agent.builder().name("support").strategy(Strategy.ROUTER).router(tool)
                .agents(Agent.builder().name("billing").model("openai/gpt-4o-mini").build())
                .synthesize(false).build();
    }

    @Test
    void serializesDecisionSelectorAndKeepsNoModelParentLocal() {
        ToolDef tool = ToolDef.builder().name("select").toolType("decision")
                .config(Map.of("model", "jev-1.13", "provider", "openrouter", "questions", QUESTIONS)).build();
        Agent agent = router(tool);
        Map<String, Object> config = new AgentConfigSerializer().serialize(agent);
        assertFalse(agent.isExternal());
        assertNull(agent.getRouter());
        assertSame(tool, agent.getDecisionRouter());
        assertEquals(false, config.get("external"));
        assertEquals(false, config.get("synthesize"));
        assertEquals(Map.of("name", "select", "kind", "decision", "model", "jev-1.13",
                "provider", "openrouter", "questions", QUESTIONS), config.get("router"));
    }

    @Test
    void omitsUnsetProviderAndPreservesDecisionTool() {
        ToolDef tool = ToolDef.builder().name("classify").toolType("decision")
                .config(Map.of("model", "jev-1.13", "questions", QUESTIONS)).build();
        Map<String, Object> config = new AgentConfigSerializer().serialize(router(tool));
        assertEquals(Map.of("name", "classify", "kind", "decision", "model", "jev-1.13",
                "questions", QUESTIONS), config.get("router"));
        Agent normal = Agent.builder().name("assistant").model("openai/gpt-4o-mini").tools(tool).build();
        Map<?, ?> serializedTool = (Map<?, ?>) ((java.util.List<?>)
                new AgentConfigSerializer().serialize(normal).get("tools")).get(0);
        assertEquals("decision", serializedTool.get("toolType"));
        assertEquals(tool.getConfig(), serializedTool.get("config"));
    }

    @Test
    void rejectsInvalidDecisionSelectors() {
        for (Map<String, Object> config : java.util.List.of(Map.<String, Object>of(),
                Map.<String, Object>of("model", "", "questions", QUESTIONS),
                Map.<String, Object>of("model", "jev-1.13", "questions", Map.of()))) {
            ToolDef tool = ToolDef.builder().name("select").toolType("decision").config(config).build();
            assertThrows(IllegalArgumentException.class, () -> new AgentConfigSerializer().serialize(router(tool)));
        }
        ToolDef worker = ToolDef.builder().name("worker").build();
        assertThrows(IllegalArgumentException.class, () -> new AgentConfigSerializer().serialize(router(worker)));
    }

    @Test
    void preservesAgentRouterAndExternalReferences() {
        Agent selector = Agent.builder().name("selector").model("openai/gpt-4o-mini").build();
        Agent agent = Agent.builder().name("support").strategy(Strategy.ROUTER).router(selector)
                .agents(selector).build();
        assertSame(selector, agent.getRouter());
        assertNull(agent.getDecisionRouter());
        assertEquals(new AgentConfigSerializer().serialize(selector),
                new AgentConfigSerializer().serialize(agent).get("router"));
        assertTrue(Agent.builder().name("existing_workflow").build().isExternal());
    }
}
