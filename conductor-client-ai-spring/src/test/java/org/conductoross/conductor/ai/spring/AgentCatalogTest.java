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
package org.conductoross.conductor.ai.spring;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.annotations.AgentDef;
import org.conductoross.conductor.ai.annotations.Tool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.orkes.conductor.client.ApiClient;

import static org.junit.jupiter.api.Assertions.*;

class AgentCatalogTest {

    private static ApiClient stubApiClient() {
        return ApiClient.builder().basePath("http://localhost:8080/api").build();
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class))
            .withBean(ApiClient.class, AgentCatalogTest::stubApiClient);

    // ── Fixture beans ───────────────────────────────────────────────────

    static class CrewBean {
        @Tool(description = "Look up an order")
        public String lookupOrder(String orderId) {
            return "order " + orderId;
        }

        @AgentDef(model = "openai/gpt-4o", instructions = "Handle billing questions.")
        public void billing() {}

        @AgentDef(model = "openai/gpt-4o")
        public String support() {
            return "Handle support tickets.";
        }
    }

    static class DuplicateNameBean {
        @AgentDef(model = "openai/gpt-4o", instructions = "Clashes with CrewBean.billing.")
        public void billing() {}
    }

    static class PlainBean {}

    // ── Tests ───────────────────────────────────────────────────────────

    @Test
    void collectsAgentsFromBeans() {
        runner.withBean("crew", CrewBean.class)
                .withBean("plain", PlainBean.class)
                .run(ctx -> {
                    AgentCatalog catalog = ctx.getBean(AgentCatalog.class);
                    assertEquals(2, catalog.all().size());
                    assertEquals(java.util.Set.of("billing", "support"), catalog.names());

                    Agent billing = catalog.get("billing");
                    assertEquals("Handle billing questions.", billing.getInstructions());

                    Agent support = catalog.get("support");
                    assertEquals("Handle support tickets.", support.getInstructions());
                    // @Tool methods on the same bean attach automatically
                    assertEquals(1, support.getTools().size());
                    assertEquals("lookupOrder", support.getTools().get(0).getName());
                });
    }

    @Test
    void duplicateAgentNamesAcrossBeansFailFast() {
        runner.withBean("crew", CrewBean.class)
                .withBean("dup", DuplicateNameBean.class)
                .run(ctx -> {
                    AgentCatalog catalog = ctx.getBean(AgentCatalog.class);
                    IllegalStateException e = assertThrows(IllegalStateException.class, catalog::all);
                    assertTrue(e.getMessage().contains("billing"));
                    assertTrue(e.getMessage().contains("crew"));
                    assertTrue(e.getMessage().contains("dup"));
                });
    }

    @Test
    void emptyCatalogWhenNoAgentBeans() {
        runner.withBean("plain", PlainBean.class).run(ctx -> {
            AgentCatalog catalog = ctx.getBean(AgentCatalog.class);
            assertTrue(catalog.all().isEmpty());
            assertTrue(catalog.find("nope").isEmpty());
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> catalog.get("nope"));
            assertTrue(e.getMessage().contains("nope"));
        });
    }

    @Test
    void userDefinedCatalogWins() {
        runner.withBean(AgentCatalog.class, () -> new AgentCatalog(null) {
                    @Override
                    public java.util.List<Agent> all() {
                        return java.util.List.of();
                    }
                })
                .run(ctx -> {
                    assertTrue(ctx.getBean(AgentCatalog.class).all().isEmpty());
                });
    }
}
