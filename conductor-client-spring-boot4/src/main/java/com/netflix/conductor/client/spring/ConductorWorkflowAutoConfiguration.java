/*
 * Copyright 2020 Conductor Authors.
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
package com.netflix.conductor.client.spring;

import org.conductoross.conductor.sdk.Conductor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Hosts the application's native workflows: every class under the application's packages with a
 * {@code @Workflow} method. Set {@code conductor.workflows.enabled=false} in a service that only
 * starts workflows.
 */
// Last, like the other Conductor auto-configurations: an earlier position would also move the
// client auto-configuration ahead of the Orkes one, and both would then define a TaskClient.
@AutoConfiguration(after = ConductorClientAutoConfiguration.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnBean(Conductor.class)
@ConditionalOnProperty(prefix = "conductor.workflows", name = "enabled", matchIfMissing = true)
public class ConductorWorkflowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConductorWorkflowHost conductorWorkflowHost(Conductor conductor,
                                                       ConfigurableListableBeanFactory beanFactory,
                                                       Environment environment) {
        return new ConductorWorkflowHost(conductor, beanFactory, environment);
    }
}
