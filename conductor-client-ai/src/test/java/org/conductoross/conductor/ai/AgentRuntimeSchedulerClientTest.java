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

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.SchedulerClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentRuntimeSchedulerClientTest {

    @Test
    void getSchedulerClientReturnsTheSharedInjectedClient() {
        SchedulerClient scheduler = (SchedulerClient) Proxy.newProxyInstance(
                SchedulerClient.class.getClassLoader(),
                new Class<?>[] {SchedulerClient.class},
                (proxy, method, args) -> null);
        AgentRuntime runtime = new AgentRuntime(
                TestClients.forUrl("http://localhost:8080"), new AgentConfig(), scheduler);

        assertSame(scheduler, runtime.getSchedulerClient());
    }

    @Test
    void deployDoesNotManageSchedules() throws Exception {
        AtomicInteger schedulerCalls = new AtomicInteger();
        SchedulerClient scheduler = (SchedulerClient) Proxy.newProxyInstance(
                SchedulerClient.class.getClassLoader(),
                new Class<?>[] {SchedulerClient.class},
                (proxy, method, args) -> {
                    schedulerCalls.incrementAndGet();
                    return null;
                });

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"agentName\":\"digest\",\"requiredWorkers\":[]}"));
            AgentRuntime runtime = new AgentRuntime(
                    TestClients.forUrl(server.url("/").toString()), new AgentConfig(), scheduler);

            runtime.deploy(Agent.builder().name("digest").model("openai/gpt-4o-mini").build());

            assertEquals(0, schedulerCalls.get());
        }
    }
}
