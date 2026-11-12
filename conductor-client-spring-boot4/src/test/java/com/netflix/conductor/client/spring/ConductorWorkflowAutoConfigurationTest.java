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

import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workers;
import org.conductoross.conductor.sdk.internal.TaskStubs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.spring.sample.AutowiredTasksWorkflow;
import com.netflix.conductor.client.spring.sample.SampleApplication;
import com.netflix.conductor.client.spring.sample.SampleTasks;
import com.netflix.conductor.client.spring.sample.SampleWorkflow;

import io.orkes.conductor.client.spring.OrkesConductorClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConductorWorkflowAutoConfigurationTest {

    private final Workers workers = mock(Workers.class, RETURNS_SELF);

    /** A Conductor whose workers are recorded, so no server is needed. */
    @Configuration
    static class RecordingConductor {
        static Workers workers;

        @Bean
        Conductor conductor() {
            Conductor conductor = mock(Conductor.class);
            when(conductor.host()).thenReturn(workers);
            return conductor;
        }
    }

    private ApplicationContextRunner runner() {
        RecordingConductor.workers = workers;
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConductorWorkflowAutoConfiguration.class))
                .withUserConfiguration(SampleApplication.class, RecordingConductor.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void hostsWorkflowClassesThatAreNotBeansWithStubsForTheirTasks() {
        runner().run(context -> {
            assertThat(context).hasSingleBean(ConductorWorkflowHost.class);
            assertThat(context).doesNotHaveBean(SampleWorkflow.class);
            ArgumentCaptor<Supplier> factory = ArgumentCaptor.forClass(Supplier.class);
            verify(workers).workflow(eq(SampleWorkflow.class), factory.capture());
            SampleWorkflow first = (SampleWorkflow) factory.getValue().get();
            assertThat(first).isNotSameAs(factory.getValue().get());
            assertThat(first.tasks()).isInstanceOf(TaskStubs.Stub.class).isNotSameAs(context.getBean(SampleTasks.class));
            assertThat(first.environment()).isSameAs(context.getEnvironment());
            verify(workers).start();
        });
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void rejectsAWorkflowThatGetsTheRealTaskBeanInjected() {
        runner().run(context -> {
            ArgumentCaptor<Supplier> factory = ArgumentCaptor.forClass(Supplier.class);
            verify(workers).workflow(eq(AutowiredTasksWorkflow.class), factory.capture());
            assertThatThrownBy(() -> TaskStubs.check(factory.getValue().get()))
                    .hasMessageContaining("field tasks of");
        });
    }

    @Test
    void registersTheTaskDefinitionsOfTaskBeansWithoutPollingThemTwice() {
        runner().run(context -> {
            verify(workers).taskDefinitions(context.getBean(SampleTasks.class));
            verify(workers, never()).tasks(any());
        });
    }

    @Test
    void stopsTheWorkersWhenTheContextCloses() {
        runner().run(context -> verify(workers, never()).shutdown());
        verify(workers).shutdown();
    }

    @Test
    void aServiceThatOnlyStartsWorkflowsCanTurnHostingOff() {
        runner().withPropertyValues("conductor.workflows.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ConductorWorkflowHost.class));
    }

    @Test
    void leavesASingleTaskClientWhenTheOrkesClientIsConfigured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OrkesConductorClientAutoConfiguration.class,
                        ConductorClientAutoConfiguration.class, ConductorWorkerAutoConfiguration.class,
                        ConductorWorkflowAutoConfiguration.class))
                .withPropertyValues("conductor.client.root-uri=http://localhost:1/api", "conductor.workflows.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(TaskClient.class));
    }
}
