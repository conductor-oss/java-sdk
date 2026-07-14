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
package io.orkes.conductor.client.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the agent-client exception hierarchy (status/message/typed fields). */
class AgentExceptionsTest {

    @Test
    void agentApiCarriesStatusAndBody() {
        AgentAPIException e = new AgentAPIException(500, "boom");
        assertEquals(500, e.getStatusCode());
        assertEquals("boom", e.getResponseBody());
        assertInstanceOf(AgentspanException.class, e);
    }

    @Test
    void notFoundIsApiExceptionWith404() {
        AgentNotFoundException e = new AgentNotFoundException(404, "missing");
        assertEquals(404, e.getStatusCode());
        assertInstanceOf(AgentAPIException.class, e);
        assertInstanceOf(AgentspanException.class, e);
    }

    @Test
    void baseExceptionKeepsMessageAndCause() {
        Throwable cause = new IllegalStateException("c");
        AgentspanException e = new AgentspanException("m", cause);
        assertEquals("m", e.getMessage());
        assertSame(cause, e.getCause());
    }
}
