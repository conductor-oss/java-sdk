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

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.ApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spec R3 (T6): connection environment resolution — standard Conductor
 * variables win, legacy Agentspan names are honored as fallbacks, and the
 * default host is the Conductor-standard {@code http://localhost:8080}.
 * Exercised through the injectable env seam so process env is never mutated.
 */
class AgentRuntimeEnvTest {

    private static ApiClient fromEnv(Map<String, String> env) {
        return AgentRuntime.clientFromEnv(env::get);
    }

    @Test
    void conductorServerUrlWinsOverAgentspan() {
        ApiClient client = fromEnv(Map.of(
                "CONDUCTOR_SERVER_URL", "http://conductor-host:9999",
                "AGENTSPAN_SERVER_URL", "http://agentspan-host:1111"));

        assertEquals("http://conductor-host:9999/api", client.getBasePath());
    }

    @Test
    void agentspanServerUrlUsedWhenConductorUnset() {
        ApiClient client = fromEnv(Map.of("AGENTSPAN_SERVER_URL", "http://agentspan-host:1111"));

        assertEquals("http://agentspan-host:1111/api", client.getBasePath());
    }

    @Test
    void defaultHostIsConductorStandard8080() {
        ApiClient client = fromEnv(Map.of());

        assertEquals(
                "http://localhost:8080/api",
                client.getBasePath(),
                "no env → Conductor-standard default (spec R3), not the legacy agentspan port 6767");
    }

    @Test
    void blankConductorUrlFallsThroughToAgentspan() {
        ApiClient client = fromEnv(Map.of(
                "CONDUCTOR_SERVER_URL", "   ",
                "AGENTSPAN_SERVER_URL", "http://agentspan-host:1111"));

        assertEquals(
                "http://agentspan-host:1111/api",
                client.getBasePath(),
                "an exported-but-blank variable must not clobber the chain");
    }

    @Test
    void trailingSlashAndApiSuffixNormalized() {
        ApiClient client = fromEnv(Map.of("CONDUCTOR_SERVER_URL", "http://conductor-host:9999/api/"));

        assertEquals("http://conductor-host:9999/api", client.getBasePath());
    }
}
