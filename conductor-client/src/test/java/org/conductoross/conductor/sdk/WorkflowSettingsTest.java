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

import java.util.HashMap;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Per-workflow settings: the named property, else the one for all workflows, else the annotation. */
class WorkflowSettingsTest {

    public static class Tuned {
        @Workflow(name = "tuned", threadCount = 3)
        public void run() {}
    }

    public static class Plain {
        @Workflow(name = "plain")
        public void run() {}
    }

    private MockWebServer server;
    private Conductor conductor;
    private final Map<String, String> settings = new HashMap<>();

    @BeforeEach
    void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                String body = path.startsWith("/api/tasks/poll") || path.startsWith("/api/metadata/taskdefs")
                        || path.startsWith("/api/schema") ? "[]" : "{}";
                return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
            }
        });
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        conductor.close();
        server.shutdown();
    }

    private Map<String, Integer> threadCounts() {
        Workers host = conductor.host().settings(settings::get).workflows(Tuned.class, Plain.class).start();
        try {
            return host.workflowRunner().getTaskThreadCount();
        } finally {
            host.shutdown();
        }
    }

    @Test
    void threadCountsComeFromTheNamedPropertyThenAllThenTheAnnotation() {
        assertEquals(Map.of("tuned.v1.code", 3, "plain.v1.code", 20), threadCounts());

        settings.put("conductor.workflow.all.threadCount", "7");
        assertEquals(Map.of("tuned.v1.code", 7, "plain.v1.code", 7), threadCounts());

        settings.put("conductor.workflow.tuned.threadCount", "5");
        assertEquals(Map.of("tuned.v1.code", 5, "plain.v1.code", 7), threadCounts());
    }

    @Test
    void theContinuationTimeoutIsSecondsOrAnIsoDurationAndAnHourInDevMode() {
        WorkflowSpec tuned = WorkflowSpec.of(Tuned.class).get(0);
        WorkflowSpec plain = WorkflowSpec.of(Plain.class).get(0);
        Workers host = conductor.host().settings(settings::get);
        assertEquals(30, host.continuationTimeoutSeconds(tuned));

        settings.put("conductor.workflow.all.continuationTimeout", "45");
        settings.put("conductor.workflow.tuned.continuationTimeout", "PT2M");
        assertEquals(120, host.continuationTimeoutSeconds(tuned));
        assertEquals(45, host.continuationTimeoutSeconds(plain));

        assertEquals(3600, host.devMode(true).continuationTimeoutSeconds(plain));
    }
}
