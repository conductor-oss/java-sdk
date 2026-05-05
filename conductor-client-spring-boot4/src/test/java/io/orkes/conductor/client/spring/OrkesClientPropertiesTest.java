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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class OrkesClientPropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(ImportingConfig.class);

    @Test
    void binds_keyIdAndSecret_underNewProperties() {
        contextRunner
                .withPropertyValues(
                        "conductor.client.keyId=my-key", "conductor.client.secret=my-secret")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getKeyId()).isEqualTo("my-key");
                            assertThat(p.getSecret()).isEqualTo("my-secret");
                        });
    }

    @Test
    void binds_keyId_kebab() {
        contextRunner
                .withPropertyValues("conductor.client.key-id=k", "conductor.client.secret=s")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getKeyId()).isEqualTo("k");
                            assertThat(p.getSecret()).isEqualTo("s");
                        });
    }

    @Test
    void binds_legacySecurityKeyId_kebab() {
        contextRunner
                .withPropertyValues(
                        "conductor.security.client.key-id=legacy-k",
                        "conductor.security.client.secret=legacy-s")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getSecurityKeyId()).isEqualTo("legacy-k");
                            assertThat(p.getSecuritySecret()).isEqualTo("legacy-s");
                        });
    }

    @Test
    void binds_legacySecurityKeyId() {
        contextRunner
                .withPropertyValues(
                        "conductor.security.client.keyId=legacy-k",
                        "conductor.security.client.secret=legacy-s")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getSecurityKeyId()).isEqualTo("legacy-k");
                            assertThat(p.getSecuritySecret()).isEqualTo("legacy-s");
                        });
    }

    @Test
    void binds_legacyServerUrl() {
        contextRunner
                .withPropertyValues("conductor.server.url=http://legacy:8080")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getConductorServerUrl()).isEqualTo("http://legacy:8080");
                        });
    }

    @Test
    void allFieldsAreNullOrEmpty_whenNoPropertiesSet() {
        // CI environments may set CONDUCTOR_SERVER_URL etc., which Spring's relaxed binding
        // exposes as conductor.server.url and friends. Override them to empty values so the
        // test asserts the contract of the @Value defaults rather than the CI environment.
        contextRunner
                .withPropertyValues(
                        "conductor.server.url=",
                        "conductor.client.keyId=",
                        "conductor.client.key-id=",
                        "conductor.client.secret=",
                        "conductor.security.client.keyId=",
                        "conductor.security.client.key-id=",
                        "conductor.security.client.secret=")
                .run(
                        context -> {
                            OrkesClientProperties p = context.getBean(OrkesClientProperties.class);
                            assertThat(p.getKeyId()).isNullOrEmpty();
                            assertThat(p.getSecret()).isNullOrEmpty();
                            assertThat(p.getConductorServerUrl()).isNullOrEmpty();
                            assertThat(p.getSecurityKeyId()).isNullOrEmpty();
                            assertThat(p.getSecuritySecret()).isNullOrEmpty();
                        });
    }

    @Configuration
    @Import(OrkesClientProperties.class)
    static class ImportingConfig {}
}
