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
package io.orkes.conductor.client.spring;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.netflix.conductor.client.spring.ClientProperties;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.AuthorizationClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.SecretClient;
import io.orkes.conductor.client.http.OrkesEventClient;
import io.orkes.conductor.client.http.OrkesMetadataClient;
import io.orkes.conductor.client.http.OrkesTaskClient;
import io.orkes.conductor.client.http.OrkesWorkflowClient;

import static org.assertj.core.api.Assertions.assertThat;

class OrkesConductorClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(OrkesConductorClientAutoConfiguration.class));

    @Test
    void apiClient_andAllOrkesClients_areRegistered_whenRootUriIsSet() {
        contextRunner
                .withPropertyValues("conductor.client.root-uri=http://localhost:8080/api")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ApiClient.class);
                            assertThat(context).hasSingleBean(OrkesClients.class);
                            assertThat(context).hasSingleBean(OrkesTaskClient.class);
                            assertThat(context).hasSingleBean(OrkesMetadataClient.class);
                            assertThat(context).hasSingleBean(OrkesWorkflowClient.class);
                            assertThat(context).hasSingleBean(AuthorizationClient.class);
                            assertThat(context).hasSingleBean(OrkesEventClient.class);
                            assertThat(context).hasSingleBean(SchedulerClient.class);
                            assertThat(context).hasSingleBean(SecretClient.class);
                        });
    }

    @Test
    void apiClient_fallsBackToConductorServerUrl() {
        contextRunner
                .withPropertyValues("conductor.server.url=http://legacy:8080/api")
                .run(context -> assertThat(context).hasSingleBean(ApiClient.class));
    }

    @Test
    void apiClient_isNotRegistered_whenNoUrlConfigured() {
        // CI environments may set CONDUCTOR_SERVER_URL, which Spring's relaxed binding exposes as
        // conductor.server.url and would activate the @ConditionalOnExpression guard. Override to
        // empty so we test the no-URL case deterministically.
        contextRunner
                .withPropertyValues(
                        "conductor.client.root-uri=",
                        "conductor.client.base-path=",
                        "conductor.server.url=")
                .run(context -> assertThat(context).doesNotHaveBean(ApiClient.class));
    }

    /**
     * Guards against accidental removal of the {@link ConditionalOnExpression} on the
     * {@code orkesConductorClient} bean factory. Spring Framework 7 no longer treats
     * null-returning {@code @Bean} methods as 'bean absent' — without this guard the bean
     * definition would be registered and downstream injection would fail with
     * UnsatisfiedDependencyException when no URL is configured.
     */
    @Test
    void orkesConductorClient_beanMethod_hasConditionalOnExpressionGuard() throws Exception {
        Method method =
                OrkesConductorClientAutoConfiguration.class.getDeclaredMethod(
                        "orkesConductorClient", ClientProperties.class, OrkesClientProperties.class);
        assertThat(method.isAnnotationPresent(ConditionalOnExpression.class)).isTrue();
    }
}
