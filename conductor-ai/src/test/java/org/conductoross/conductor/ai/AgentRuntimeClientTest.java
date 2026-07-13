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

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.http.OrkesAgentClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The runtime's control-plane transport is the {@link AgentClient} it exposes —
 * spec R5 acceptance: {@code getClient()} returns the very instance the runtime
 * uses, so callers can mix runtime verbs with direct control-plane calls on a
 * single token authority.
 */
class AgentRuntimeClientTest {

    @Test
    void getClientReturnsTheRuntimeClientInstance() {
        AgentRuntime runtime = new AgentRuntime(AgentRuntime.client("http://localhost:8080"));

        AgentClient client = runtime.getClient();
        assertInstanceOf(OrkesAgentClient.class, client);
        assertSame(client, runtime.getClient(), "accessor must be stable across calls");
    }
}
