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
package org.conductoross.conductor.ai.exceptions;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.exceptions.AgentspanException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the credential exception hierarchy (typed fields, base type).
 * The agent-client exceptions ({@code AgentAPIException} et al.) are tested in
 * {@code conductor-client}'s {@code AgentExceptionsTest}.
 *
 * <p>Only {@link CredentialNotFoundException} remains: the fetch-transport
 * exceptions (auth / rate-limit / service) died with the {@code /workers/secrets}
 * fetcher — credentials now arrive on the wire via {@code Task.runtimeMetadata}
 * (spec R6/R12).
 */
class ExceptionsTest {

    @Test
    void credentialNotFoundListsMissingNames() {
        CredentialNotFoundException e = new CredentialNotFoundException(List.of("A", "B"));
        assertEquals(List.of("A", "B"), e.getMissingNames());
        CredentialNotFoundException single = new CredentialNotFoundException("ONLY");
        assertTrue(single.getMissingNames().contains("ONLY"));
    }

    @Test
    void credentialNotFoundIsAnAgentspanException() {
        assertInstanceOf(AgentspanException.class, new CredentialNotFoundException("ONLY"));
    }
}
