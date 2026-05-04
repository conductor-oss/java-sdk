/*
 * Copyright 2024 Conductor Authors.
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

import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that selects the correct Prometheus {@link AbstractPrometheusMetricsCollector}
 * based on environment variables.
 *
 * <ul>
 *   <li>{@code WORKER_CANONICAL_METRICS=true} &rarr; {@link CanonicalPrometheusMetricsCollector}</li>
 *   <li>{@code WORKER_LEGACY_METRICS=true} (default during deprecation) &rarr; {@link LegacyPrometheusMetricsCollector}</li>
 * </ul>
 *
 * If {@code WORKER_CANONICAL_METRICS} is true it takes priority regardless of
 * the value of {@code WORKER_LEGACY_METRICS}.
 */
public final class MetricsCollectorFactory {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorFactory.class);

    private MetricsCollectorFactory() { }

    /**
     * Create the metrics collector selected by environment variables.
     */
    public static AbstractPrometheusMetricsCollector create() {
        return create(System::getenv);
    }

    static AbstractPrometheusMetricsCollector create(Function<String, String> envReader) {
        if (envBool("WORKER_CANONICAL_METRICS", false, envReader)) {
            log.info("WORKER_CANONICAL_METRICS is true — using CanonicalPrometheusMetricsCollector");
            return new CanonicalPrometheusMetricsCollector();
        }
        log.info("Using LegacyPrometheusMetricsCollector (set WORKER_CANONICAL_METRICS=true for canonical metrics)");
        return new LegacyPrometheusMetricsCollector();
    }

    private static final Set<String> TRUTHY_VALUES = Set.of("true", "1", "yes");

    static boolean envBool(String name, boolean defaultValue, Function<String, String> envReader) {
        String value = envReader.apply(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return TRUTHY_VALUES.contains(value.trim().toLowerCase());
    }
}
