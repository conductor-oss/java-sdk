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
package com.netflix.conductor.client.spring.compat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.spring.ConductorClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Does a Boot app using the starter start up? ConductorClient builds its Jackson
 * mapper in its constructor, so an incompatible jackson-annotations jar fails here.
 */
class Boot3StartupTest {

    @Test
    void applicationContextStarts() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConductorClientAutoConfiguration.class))
                .withPropertyValues("conductor.client.base-path=http://localhost:8080/api")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ConductorClient.class);
                });
    }
}
