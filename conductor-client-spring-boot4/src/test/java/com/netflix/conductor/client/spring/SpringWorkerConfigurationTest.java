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
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SpringWorkerConfigurationTest {

    @Test
    void getPollingInterval_readsTaskSpecificProperty() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.worker.taskA.pollingInterval", "200");
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(env);

        assertThat(cfg.getPollingInterval("taskA")).isEqualTo(200);
    }

    @Test
    void getPollingInterval_fallsBackToAllScope() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.worker.all.pollingInterval", "150");
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(env);

        assertThat(cfg.getPollingInterval("anyTask")).isEqualTo(150);
    }

    @Test
    void getDomain_returnsConfiguredDomain() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.worker.taskA.domain", "domain-x");
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(env);

        assertThat(cfg.getDomain("taskA")).isEqualTo("domain-x");
    }

    @Test
    void getDomain_returnsNullByDefault() {
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(new MockEnvironment());

        assertThat(cfg.getDomain("taskA")).isNull();
    }

    @Test
    void getThreadCount_returnsZeroByDefault() {
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(new MockEnvironment());

        assertThat(cfg.getThreadCount("taskA")).isZero();
    }

    @Test
    void getPollerCount_readsTaskSpecific() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.worker.taskA.pollerCount", "3");
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(env);

        assertThat(cfg.getPollerCount("taskA")).isEqualTo(3);
    }

    @Test
    void getPollTimeout_readsTaskSpecific() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.worker.taskA.pollTimeout", "500");
        SpringWorkerConfiguration cfg = new SpringWorkerConfiguration(env);

        assertThat(cfg.getPollTimeout("taskA")).isEqualTo(500);
    }
}
