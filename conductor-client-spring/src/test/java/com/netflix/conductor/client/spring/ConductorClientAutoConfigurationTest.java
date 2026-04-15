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

import org.conductoross.conductor.client.FileClient;
import org.conductoross.conductor.client.FileClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ConductorClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConductorClientAutoConfiguration.class));

    @Test
    void zeroConfigYieldsWorkingFileClientBean() {
        contextRunner
                .withPropertyValues("conductor.client.base-path=http://localhost:8080/api")
                .run(context -> {
                    assertThat(context).hasSingleBean(FileClient.class);
                    FileClientProperties properties = context.getBean(FileClientProperties.class);
                    assertThat(properties.getLocalCacheDirectory())
                            .startsWith(System.getProperty("java.io.tmpdir"));
                });
    }

    @Test
    void cacheDirectoryOverrideIsRespected() {
        contextRunner
                .withPropertyValues(
                        "conductor.client.base-path=http://localhost:8080/api",
                        "conductor.file-client.local-cache-directory=/tmp/custom-cache")
                .run(context -> {
                    FileClientProperties properties = context.getBean(FileClientProperties.class);
                    assertThat(properties.getLocalCacheDirectory()).isEqualTo("/tmp/custom-cache");
                });
    }

}
