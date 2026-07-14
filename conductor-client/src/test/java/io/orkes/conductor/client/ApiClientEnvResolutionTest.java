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
package io.orkes.conductor.client;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Env-based client construction (spec R3): standard Conductor variables win,
 * legacy Agentspan names are honored as fallbacks, and the default is the
 * Conductor-standard {@code http://localhost:8080/api}. Exercised through the
 * builder's env seam so process env is never mutated.
 */
class ApiClientEnvResolutionTest {

    private static ApiClient.ApiClientBuilder builderWithEnv(Map<String, String> env) {
        ApiClient.ApiClientBuilder builder = new ApiClient.ApiClientBuilder() {
            @Override
            protected String getEnv(String name) {
                return env.get(name);
            }
        };
        return builder.useEnvVariables(true);
    }

    private static ApiClient fromEnv(Map<String, String> env) {
        return builderWithEnv(env).build();
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
    void defaultIsConductorStandardLocalhost() {
        ApiClient client = fromEnv(Map.of());

        assertEquals(
                "http://localhost:8080/api",
                client.getBasePath(),
                "no env → Conductor-standard default (spec R3), never a throw");
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
    void serverUrlNormalizedToApiSuffix() {
        assertEquals("http://h:1/api", fromEnv(Map.of("CONDUCTOR_SERVER_URL", "http://h:1")).getBasePath());
        assertEquals("http://h:1/api", fromEnv(Map.of("CONDUCTOR_SERVER_URL", "http://h:1/")).getBasePath());
        assertEquals("http://h:1/api", fromEnv(Map.of("CONDUCTOR_SERVER_URL", "http://h:1/api")).getBasePath());
        assertEquals("http://h:1/api", fromEnv(Map.of("CONDUCTOR_SERVER_URL", "http://h:1/api/")).getBasePath());
    }

    @Test
    void agentspanAuthKeysUsedAsFallback() {
        ApiClient.ApiClientBuilder builder = builderWithEnv(Map.of(
                "AGENTSPAN_AUTH_KEY", "legacy-key",
                "AGENTSPAN_AUTH_SECRET", "legacy-secret"));
        builder.build();

        assertNotNull(builder.authentication, "legacy Agentspan credentials must configure authentication");
    }

    @Test
    void missingSecretLeavesClientAnonymous() {
        ApiClient.ApiClientBuilder builder = builderWithEnv(Map.of("CONDUCTOR_AUTH_KEY", "key-only"));
        builder.build();

        assertNull(builder.authentication, "a key without a secret must not configure authentication");
    }
}
