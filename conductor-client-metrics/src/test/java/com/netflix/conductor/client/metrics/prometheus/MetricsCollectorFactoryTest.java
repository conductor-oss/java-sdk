/*
 * Copyright 2025 Conductor Authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorFactoryTest {

    // --- Factory selection ---

    @Test
    void createReturnsCanonicalWhenEnvIsTrue() {
        Map<String, String> env = Map.of("WORKER_CANONICAL_METRICS", "true");
        var collector = MetricsCollectorFactory.create(env::get);

        assertInstanceOf(CanonicalPrometheusMetricsCollector.class, collector);
    }

    @Test
    void createReturnsLegacyWhenEnvNotSet() {
        Map<String, String> env = Map.of();
        var collector = MetricsCollectorFactory.create(env::get);

        assertInstanceOf(LegacyPrometheusMetricsCollector.class, collector);
    }

    @Test
    void createReturnsLegacyWhenEnvIsFalse() {
        Map<String, String> env = Map.of("WORKER_CANONICAL_METRICS", "false");
        var collector = MetricsCollectorFactory.create(env::get);

        assertInstanceOf(LegacyPrometheusMetricsCollector.class, collector);
    }

    @Test
    void createReturnsLegacyWhenEnvIsArbitraryString() {
        Map<String, String> env = Map.of("WORKER_CANONICAL_METRICS", "enabled");
        var collector = MetricsCollectorFactory.create(env::get);

        assertInstanceOf(LegacyPrometheusMetricsCollector.class, collector);
    }

    @Test
    void canonicalTakesPriorityOverLegacy() {
        Map<String, String> env = Map.of(
                "WORKER_CANONICAL_METRICS", "true",
                "WORKER_LEGACY_METRICS", "true");
        var collector = MetricsCollectorFactory.create(env::get);

        assertInstanceOf(CanonicalPrometheusMetricsCollector.class, collector);
    }

    // --- Env var truthiness ---

    @Test
    void envBoolTrueString() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "true"));
    }

    @Test
    void envBoolOneString() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "1"));
    }

    @Test
    void envBoolYesString() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "yes"));
    }

    @Test
    void envBoolTrueUpperCase() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "TRUE"));
    }

    @Test
    void envBoolTrueMixedCase() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "True"));
    }

    @Test
    void envBoolYesUpperCase() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "YES"));
    }

    @Test
    void envBoolTrueWithWhitespace() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "  true  "));
    }

    @Test
    void envBoolOneWithWhitespace() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> " 1 "));
    }

    @Test
    void envBoolFalseString() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> "false"));
    }

    @Test
    void envBoolZeroString() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> "0"));
    }

    @Test
    void envBoolNoString() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> "no"));
    }

    @Test
    void envBoolArbitraryStringIsFalse() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> "enabled"));
    }

    @Test
    void envBoolEmptyStringUsesDefault() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> ""));
        assertTrue(MetricsCollectorFactory.envBool("X", true, name -> ""));
    }

    @Test
    void envBoolBlankStringUsesDefault() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> "   "));
        assertTrue(MetricsCollectorFactory.envBool("X", true, name -> "   "));
    }

    @Test
    void envBoolNullUsesDefault() {
        assertFalse(MetricsCollectorFactory.envBool("X", false, name -> null));
        assertTrue(MetricsCollectorFactory.envBool("X", true, name -> null));
    }

    @Test
    void envBoolExplicitTrueOverridesDefaultFalse() {
        assertTrue(MetricsCollectorFactory.envBool("X", false, name -> "true"));
    }

    @Test
    void envBoolExplicitFalseOverridesDefaultTrue() {
        assertFalse(MetricsCollectorFactory.envBool("X", true, name -> "false"));
    }

    @Test
    void envBoolReadsCorrectEnvVarName() {
        Map<String, String> env = new HashMap<>();
        env.put("MY_VAR", "true");
        env.put("OTHER_VAR", "false");

        assertTrue(MetricsCollectorFactory.envBool("MY_VAR", false, env::get));
        assertFalse(MetricsCollectorFactory.envBool("OTHER_VAR", false, env::get));
        assertFalse(MetricsCollectorFactory.envBool("MISSING_VAR", false, env::get));
    }
}
