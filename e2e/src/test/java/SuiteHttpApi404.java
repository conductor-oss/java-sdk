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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.exceptions.AgentException;
import io.orkes.conductor.client.exceptions.AgentNotFoundException;
import io.orkes.conductor.client.http.OrkesAgentClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live 404 round-trip — proves {@link AgentClient} maps server 404 responses
 * (raised by the Conductor client as {@code ConductorClientException}) to the
 * narrower {@link AgentNotFoundException} subtype (Python-SDK parity).
 *
 * <p>Counterfactual: if AgentClient raised the generic {@link AgentAPIException}
 * for every 4xx (or leaked Conductor's own exception), the {@code assertInstanceOf}
 * check below would fail.
 */
@Tag("e2e")
class SuiteHttpApi404 extends BaseTest {

    @Test
    void getStatusOnMissingExecutionIdRaisesAgentNotFoundException() {
        ConductorClient cc = new ApiClient(
                (BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL) + "/api");
        AgentClient api = new OrkesAgentClient(cc);

        AgentAPIException ex =
                assertThrows(AgentAPIException.class, () -> api.getAgentStatus("does-not-exist-" + System.nanoTime()));

        assertInstanceOf(
                AgentNotFoundException.class,
                ex,
                "404 must surface as AgentNotFoundException, not generic AgentAPIException");
        assertInstanceOf(
                AgentException.class, ex, "AgentNotFoundException must remain catchable as the SDK base type");
        assertTrue(ex.getStatusCode() == 404, "Expected statusCode=404, got " + ex.getStatusCode());
    }
}
