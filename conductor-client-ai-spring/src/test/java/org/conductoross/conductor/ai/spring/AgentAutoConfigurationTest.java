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

import org.conductoross.conductor.ai.AgentConfig;
import org.conductoross.conductor.ai.AgentRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.orkes.conductor.client.ApiClient;

import static org.junit.jupiter.api.Assertions.*;

class AgentAutoConfigurationTest {

    /**
     * Provide a minimal ApiClient so tests don't need a live conductor-client-spring
     * auto-configuration (which would require a real server URL to be set).
     * In production, OrkesConductorClientAutoConfiguration wires this from
     * conductor.* properties.
     */
    private static ApiClient stubApiClient() {
        return ApiClient.builder().basePath("http://localhost:6767/api").build();
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentAutoConfiguration.class))
            .withBean(ApiClient.class, AgentAutoConfigurationTest::stubApiClient);

    @Test
    void wiresConfigAndRuntimeWithDefaults() {
        runner.run(ctx -> {
            assertTrue(ctx.containsBean("conductorAgentConfig"), "AgentConfig bean must be present");
            assertTrue(ctx.containsBean("agentRuntime"), "AgentRuntime bean must be present");

            // No agent-specific client bean — ApiClient comes from conductor-client-spring.
            assertFalse(
                    ctx.containsBean("conductorAgentClient"),
                    "auto-config must NOT create its own ApiClient — "
                            + "that is OrkesConductorClientAutoConfiguration's job");

            AgentConfig config = ctx.getBean(AgentConfig.class);
            assertEquals(100, config.getWorkerPollIntervalMs(), "default poll interval");
            assertEquals(1, config.getWorkerThreadCount(), "default thread count");
        });
    }

    @Test
    void respectsWorkerTuningProperties() {
        runner.withPropertyValues("conductor.agent.worker-thread-count=4", "conductor.agent.worker-poll-interval-ms=250")
                .run(ctx -> {
                    AgentConfig config = ctx.getBean(AgentConfig.class);
                    assertEquals(4, config.getWorkerThreadCount());
                    assertEquals(250, config.getWorkerPollIntervalMs());
                });
    }

    @Test
    void serverUrlPropertiesAreNotAccepted() {
        // conductor.agent.server-url does not exist — setting it must not cause an error
        // (Spring ignores unknown properties by default) and must not affect the client.
        runner.withPropertyValues("conductor.agent.server-url=http://ignored:9090")
                .run(ctx -> assertFalse(
                        ctx.getStartupFailure() != null, "unknown property must not break context startup"));
    }

    @Test
    void doesNotOverrideUserDefinedAgentConfigBean() {
        AgentConfig custom = new AgentConfig(50, 2);
        runner.withBean(AgentConfig.class, () -> custom).run(ctx -> {
            AgentConfig config = ctx.getBean(AgentConfig.class);
            assertSame(custom, config, "@ConditionalOnMissingBean must yield to user-provided AgentConfig");
            assertEquals(50, config.getWorkerPollIntervalMs());
        });
    }

    @Test
    void doesNotOverrideUserDefinedAgentRuntimeBean() {
        AgentRuntime customRuntime = new AgentRuntime(stubApiClient(), new AgentConfig());
        runner.withBean(AgentRuntime.class, () -> customRuntime).run(ctx -> {
            AgentRuntime runtime = ctx.getBean(AgentRuntime.class);
            assertSame(customRuntime, runtime, "@ConditionalOnMissingBean must yield to user-provided AgentRuntime");
            customRuntime.shutdown();
        });
    }
}
