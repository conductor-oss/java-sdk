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
package com.netflix.conductor.client.spring;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import com.netflix.conductor.sdk.workflow.executor.task.AnnotatedWorkerExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class ConductorClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(ConductorClientAutoConfiguration.class));

    @Test
    void conductorClient_isCreated_whenRootUriIsSet() {
        contextRunner
                .withPropertyValues("conductor.client.root-uri=http://localhost:8080/api")
                .withBean(NoopWorker.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ConductorClient.class);
                            assertThat(context).hasSingleBean(TaskClient.class);
                            assertThat(context).hasSingleBean(WorkflowClient.class);
                            assertThat(context).hasSingleBean(AnnotatedWorkerExecutor.class);
                            assertThat(context).hasSingleBean(WorkflowExecutor.class);
                            assertThat(context).hasSingleBean(TaskRunnerConfigurer.class);
                        });
    }

    @Test
    void conductorClient_isCreated_whenLegacyBasePathIsSet() {
        contextRunner
                .withPropertyValues("conductor.client.base-path=http://legacy:8080/api")
                .withBean(NoopWorker.class)
                .run(context -> assertThat(context).hasSingleBean(ConductorClient.class));
    }

    @Test
    void conductorClient_isNotCreated_whenNoBasePathConfigured() {
        contextRunner
                .withBean(NoopWorker.class)
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ConductorClient.class);
                            assertThat(context).doesNotHaveBean(TaskClient.class);
                            assertThat(context).doesNotHaveBean(WorkflowClient.class);
                        });
    }

    @Test
    void userProvidedConductorClient_overridesAutoConfiguration() {
        contextRunner
                .withPropertyValues("conductor.client.root-uri=http://localhost:8080/api")
                .withBean(NoopWorker.class)
                .withUserConfiguration(UserClientConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ConductorClient.class);
                            ConductorClient bean = context.getBean(ConductorClient.class);
                            assertThat(bean).isSameAs(UserClientConfig.USER_CLIENT);
                        });
    }

    @Test
    void clientProperties_areBoundFromEnvironment() {
        contextRunner
                .withPropertyValues(
                        "conductor.client.root-uri=http://localhost:8080/api",
                        "conductor.client.thread-count=7",
                        "conductor.client.update-retry-count=4",
                        "conductor.client.shutdown-grace-period-seconds=15",
                        "conductor.client.task-poll-timeout=250",
                        "conductor.client.timeout.connect=1000",
                        "conductor.client.timeout.read=2000",
                        "conductor.client.timeout.write=3000",
                        "conductor.client.verifying-ssl=false",
                        "conductor.client.task-to-domain.taskA=domainX",
                        "conductor.client.sleep-when-retry-duration=PT1S")
                .withBean(NoopWorker.class)
                .run(
                        context -> {
                            ClientProperties props = context.getBean(ClientProperties.class);
                            assertThat(props.getRootUri()).isEqualTo("http://localhost:8080/api");
                            assertThat(props.getThreadCount()).isEqualTo(7);
                            assertThat(props.getUpdateRetryCount()).isEqualTo(4);
                            assertThat(props.getShutdownGracePeriodSeconds()).isEqualTo(15);
                            assertThat(props.getTaskPollTimeout()).isEqualTo(250);
                            assertThat(props.getTimeout().getConnect()).isEqualTo(1000);
                            assertThat(props.getTimeout().getRead()).isEqualTo(2000);
                            assertThat(props.getTimeout().getWrite()).isEqualTo(3000);
                            assertThat(props.isVerifyingSsl()).isFalse();
                            assertThat(props.getTaskToDomain()).containsEntry("taskA", "domainX");
                            assertThat(props.getSleepWhenRetryDuration())
                                    .isEqualTo(Duration.ofSeconds(1));
                        });
    }

    /**
     * Guards against accidental removal of the {@link ConditionalOnExpression} on the
     * {@code conductorClient} bean factory. Spring Framework 7 no longer treats null-returning
     * {@code @Bean} methods as 'bean absent' — without this guard the bean definition would be
     * registered and downstream injection would fail with UnsatisfiedDependencyException when no
     * URL is configured.
     */
    @Test
    void conductorClient_beanMethod_hasConditionalOnExpressionGuard() throws Exception {
        Method method =
                ConductorClientAutoConfiguration.class.getDeclaredMethod(
                        "conductorClient", ClientProperties.class);
        assertThat(method.isAnnotationPresent(ConditionalOnExpression.class)).isTrue();
    }

    static class NoopWorker implements Worker {
        @Override
        public String getTaskDefName() {
            return "noop";
        }

        @Override
        public TaskResult execute(Task task) {
            return new TaskResult(task);
        }
    }

    @Configuration
    static class UserClientConfig {
        static final ConductorClient USER_CLIENT =
                ConductorClient.builder().basePath("http://user:9999/api").build();

        @Bean
        ConductorClient conductorClient() {
            return USER_CLIENT;
        }
    }
}
