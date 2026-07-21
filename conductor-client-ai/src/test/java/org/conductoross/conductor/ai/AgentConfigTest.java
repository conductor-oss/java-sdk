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
package org.conductoross.conductor.ai;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec R4 (T7): runtime option knobs with documented defaults, environment
 * names, and lenient parsing — an invalid or empty value falls back to the
 * default instead of crashing runtime construction.
 */
class AgentConfigTest {

    private static AgentConfig fromEnv(Map<String, String> env) {
        return AgentConfig.fromEnv(env::get);
    }

    @Test
    void defaultsWhenEnvEmpty() {
        AgentConfig config = fromEnv(Map.of());

        assertEquals(100, config.getWorkerPollIntervalMs());
        assertEquals(1, config.getWorkerThreadCount());
        assertTrue(config.isAutoStartWorkers());
        assertTrue(config.isDaemonWorkers());
        assertTrue(config.isStreamingEnabled());
        assertTrue(config.isLivenessEnabled());
        assertEquals(30.0, config.getLivenessStallSeconds());
        assertEquals(10.0, config.getLivenessCheckIntervalSeconds());
    }

    @Test
    void parsesAllKnobsFromEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("CONDUCTOR_AGENT_WORKER_POLL_INTERVAL", "250");
        env.put("CONDUCTOR_AGENT_WORKER_THREADS", "4");
        env.put("CONDUCTOR_AGENT_AUTO_START_WORKERS", "false");
        env.put("CONDUCTOR_AGENT_DAEMON_WORKERS", "false");
        env.put("CONDUCTOR_AGENT_STREAMING_ENABLED", "false");
        env.put("CONDUCTOR_AGENT_LIVENESS_ENABLED", "false");
        env.put("CONDUCTOR_AGENT_LIVENESS_STALL_SECONDS", "45.5");
        env.put("CONDUCTOR_AGENT_LIVENESS_CHECK_INTERVAL_SECONDS", "2.5");

        AgentConfig config = fromEnv(env);

        assertEquals(250, config.getWorkerPollIntervalMs());
        assertEquals(4, config.getWorkerThreadCount());
        assertFalse(config.isAutoStartWorkers());
        assertFalse(config.isDaemonWorkers());
        assertFalse(config.isStreamingEnabled());
        assertFalse(config.isLivenessEnabled());
        assertEquals(45.5, config.getLivenessStallSeconds());
        assertEquals(2.5, config.getLivenessCheckIntervalSeconds());
    }

    @Test
    void invalidNumberFallsBackToDefault() {
        AgentConfig config = fromEnv(Map.of(
                "CONDUCTOR_AGENT_WORKER_POLL_INTERVAL", "not-a-number",
                "CONDUCTOR_AGENT_LIVENESS_STALL_SECONDS", "soon"));

        assertEquals(
                100,
                config.getWorkerPollIntervalMs(),
                "invalid int must fall back to the default, not throw NumberFormatException");
        assertEquals(30.0, config.getLivenessStallSeconds());
    }

    @Test
    void emptyStringFallsBackToDefault() {
        Map<String, String> env = new HashMap<>();
        env.put("CONDUCTOR_AGENT_WORKER_THREADS", "");
        env.put("CONDUCTOR_AGENT_STREAMING_ENABLED", "  ");

        AgentConfig config = fromEnv(env);

        assertEquals(1, config.getWorkerThreadCount());
        assertTrue(config.isStreamingEnabled());
    }

    @Test
    void booleanVariantsParse() {
        assertFalse(fromEnv(Map.of("CONDUCTOR_AGENT_AUTO_START_WORKERS", "0")).isAutoStartWorkers());
        assertTrue(fromEnv(Map.of("CONDUCTOR_AGENT_AUTO_START_WORKERS", "TRUE")).isAutoStartWorkers());
        assertFalse(fromEnv(Map.of("CONDUCTOR_AGENT_AUTO_START_WORKERS", "no")).isAutoStartWorkers());
        assertTrue(
                fromEnv(Map.of("CONDUCTOR_AGENT_AUTO_START_WORKERS", "banana")).isAutoStartWorkers(),
                "unrecognized boolean falls back to the default");
    }

    @Test
    void fluentSettersOverrideDefaults() {
        AgentConfig config = new AgentConfig()
                .autoStartWorkers(false)
                .streamingEnabled(false)
                .livenessStallSeconds(5.0)
                .livenessCheckIntervalSeconds(1.0);

        assertFalse(config.isAutoStartWorkers());
        assertFalse(config.isStreamingEnabled());
        assertEquals(5.0, config.getLivenessStallSeconds());
        assertEquals(1.0, config.getLivenessCheckIntervalSeconds());
    }
}
