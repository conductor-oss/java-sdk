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
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPropertiesBindingTest {

    @Test
    void binds_kebabCaseProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.client.root-uri", "http://localhost:8080/api");
        env.setProperty("conductor.client.thread-count", "5");
        env.setProperty("conductor.client.update-retry-count", "9");
        env.setProperty("conductor.client.shutdown-grace-period-seconds", "30");
        env.setProperty("conductor.client.task-poll-timeout", "150");

        ClientProperties props = bind(env);

        assertThat(props.getRootUri()).isEqualTo("http://localhost:8080/api");
        assertThat(props.getThreadCount()).isEqualTo(5);
        assertThat(props.getUpdateRetryCount()).isEqualTo(9);
        assertThat(props.getShutdownGracePeriodSeconds()).isEqualTo(30);
        assertThat(props.getTaskPollTimeout()).isEqualTo(150);
    }

    @Test
    void binds_camelCaseProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.client.rootUri", "http://camel:8080/api");
        env.setProperty("conductor.client.threadCount", "5");

        ClientProperties props = bind(env);

        assertThat(props.getRootUri()).isEqualTo("http://camel:8080/api");
        assertThat(props.getThreadCount()).isEqualTo(5);
    }

    @Test
    void binds_basePathAlias_kebab() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.client.base-path", "http://legacy/api");
        ClientProperties props = bind(env);
        assertThat(props.getBasePath()).isEqualTo("http://legacy/api");
    }

    @Test
    void binds_basePathAlias_camelCase() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("conductor.client.basePath", "http://camel-legacy/api");
        ClientProperties props = bind(env);
        assertThat(props.getBasePath()).isEqualTo("http://camel-legacy/api");
    }

    private static ClientProperties bind(MockEnvironment env) {
        return Binder.get(env)
                .bind("conductor.client", ClientProperties.class)
                .orElseGet(ClientProperties::new);
    }
}
