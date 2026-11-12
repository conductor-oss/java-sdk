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
package org.conductoross.conductor.sdk;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.client.ApiClient;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** {@link Conductor} runs over a client it does not own, so an injected client keeps working. */
class ConductorClientOwnershipTest {

    @Test
    void closingLeavesTheClientToItsCreator() {
        ConductorClient client = mock(ConductorClient.class);
        Conductor conductor = Conductor.of(client);
        assertSame(client, conductor.client());

        conductor.close();

        verify(client, never()).shutdown();
    }

    @Test
    void aClientIsRequired() {
        assertThrows(NullPointerException.class, () -> Conductor.of(null));
    }

    @Test
    void theEnvironmentClientCanCarryKeys() {
        ConductorClient client = ConductorClient.fromEnvironment();
        try {
            assertTrue(client instanceof ApiClient, "an ApiClient, which reads CONDUCTOR_AUTH_KEY/SECRET");
            assertTrue(client.getBasePath().endsWith("/api"), client.getBasePath());
        } finally {
            client.shutdown();
        }
    }
}
