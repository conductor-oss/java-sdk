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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.spring.OrkesConductorClientAutoConfiguration;

/**
 * Spring Boot auto-configuration for the Conductor agent SDK.
 *
 * <p>This configuration wires two beans from {@code conductor.agent.*} properties:
 * <ul>
 *   <li>{@link AgentConfig} — worker-runner tuning (poll interval, thread count)</li>
 *   <li>{@link AgentRuntime} — the SDK entry point</li>
 * </ul>
 *
 * <p>The {@link ApiClient} (server URL + auth) is <em>not</em> created here. It
 * comes from {@link OrkesConductorClientAutoConfiguration}, which is pulled in
 * transitively via {@code conductor-client-spring} and reads {@code conductor.*}
 * properties. Users configure connectivity once in that namespace:
 * <pre>{@code
 * conductor.root-uri=http://localhost:6767/api
 * conductor.security.client.key-id=my-key       # optional
 * conductor.security.client.secret=my-secret    # optional
 * }</pre>
 *
 * <p>All three beans are conditional — define your own to override any of them.
 */
@AutoConfiguration(after = OrkesConductorClientAutoConfiguration.class)
@EnableConfigurationProperties(AgentProperties.class)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentConfig conductorAgentConfig(AgentProperties props) {
        return new AgentConfig(props.getWorkerPollIntervalMs(), props.getWorkerThreadCount());
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentRuntime agentRuntime(ApiClient conductorClient, AgentConfig agentConfig) {
        return new AgentRuntime(conductorClient, agentConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentCatalog agentCatalog(ApplicationContext context) {
        return new AgentCatalog(context);
    }
}
