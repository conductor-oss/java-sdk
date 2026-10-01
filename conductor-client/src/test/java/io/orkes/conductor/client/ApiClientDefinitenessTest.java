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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy {@link ApiClient} duplicates {@code execute(Call, Type)} rather than delegating, so the
 * definiteness contract has to be pinned on its own code path, not through {@code WorkflowClient}.
 *
 * @see <a href="https://github.com/orkes-io/jepsen-conductor/issues/6">jepsen-conductor#6</a>
 */
class ApiClientDefinitenessTest {

    private MockWebServer server;
    private ApiClient apiClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        apiClient = new ApiClient(server.url("/api").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        Thread.interrupted();
    }

    @Test
    @DisplayName("A rejected request is definite on the legacy client's own execute path")
    void badRequest_isDefinite() {
        server.enqueue(new MockResponse().setResponseCode(400).setBody("bad request"));

        var e = assertThrows(ConductorClientException.class, this::executeOnApiClient);

        assertEquals(400, e.getStatus());
        assertTrue(e.isDefinite(), "a 400 means the request was never applied");
    }

    @Test
    @DisplayName("A dropped connection stays indeterminate on the legacy client's own execute path")
    void connectionDroppedAfterRequest_isNotDefinite() throws InterruptedException {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));

        var e = assertThrows(ConductorClientException.class, this::executeOnApiClient);

        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS), "the server received the request");
        assertEquals(0, e.getStatus(), "no response status ever came back");
        assertFalse(e.isDefinite(), "the server may have applied it before the connection dropped");
    }

    // Builds and runs the call on the ApiClient itself, so its duplicated execute() is the one under test.
    private void executeOnApiClient() {
        var call = apiClient.buildCall("/workflow", "POST", List.of(), List.of(), "{}", Map.of());
        apiClient.execute(call, String.class);
    }
}
