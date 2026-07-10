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
package com.netflix.conductor.client.metrics.prometheus;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.netflix.conductor.client.metrics.MetricsCollector;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Spring Boot auto-configuration that exposes the SDK's {@link MetricsCollector}
 * bound to the application's Micrometer {@link MeterRegistry}.
 *
 * <p>When a {@code MeterRegistry} bean exists (e.g. the one Spring Boot Actuator
 * creates to back {@code /actuator/prometheus}), the SDK's metrics are published
 * into it and therefore show up alongside the application's own metrics &mdash;
 * no separate metrics web server is started by the SDK.
 *
 * <p>This class is only loaded in a Spring Boot application (it is referenced
 * from {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}).
 * Plain-Java consumers never trigger it and pull in no Spring dependency.
 */
@AutoConfiguration(
        afterName = {
            "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration"
        })
@ConditionalOnClass(MeterRegistry.class)
public class ConductorMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(MetricsCollector.class)
    public MetricsCollector conductorMetricsCollector(MeterRegistry meterRegistry) {
        return new PrometheusMetricsCollector(meterRegistry);
    }
}
