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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.netflix.conductor.client.http.TaskClient;

import static org.assertj.core.api.Assertions.assertThat;

class ConductorWorkerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(ConductorWorkerAutoConfiguration.class));

    @Test
    void autoConfig_isInactive_whenNoTaskClientBeanExists() {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .doesNotHaveBean(ConductorWorkerAutoConfiguration.class));
    }

    @Test
    void autoConfig_isActive_whenTaskClientBeanExists() {
        TaskClient taskClient = Mockito.mock(TaskClient.class);
        contextRunner
                .withBean(TaskClient.class, () -> taskClient)
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(ConductorWorkerAutoConfiguration.class));
    }
}
