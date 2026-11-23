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
package strict;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.NonDeterminismException;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workers;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.CodeReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.conductoross.conductor.sdk.Conductor.doWhile;
import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.random;
import static org.conductoross.conductor.sdk.Conductor.switchOn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workflow code that asks the JVM itself for a random value or the time is found when the workers
 * start: warned about, or refused when the workflow is hosted with strictDeterminism.
 */
class StrictDeterminismTest {

    public static class Tasks {
        @WorkerTask("sd_step")
        public String step(@InputParam("x") String x) {
            return x + System.currentTimeMillis(); // a task runs once: not workflow code
        }
    }

    /** A random source or a clock read everywhere workflow code can be. */
    public static class Raw {
        private final Tasks tasks;
        @Variable private String note;

        public Raw(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "sd_raw")
        public String run(@InputParam("x") String x) {
            double d = Math.random(); // RAW_DIRECT
            String at = stamp();
            String r = switchOn("route", x)
                    .when("a", () -> tasks.step(UUID.randomUUID().toString())) // RAW_CASE
                    .otherwise(() -> "none")
                    .get();
            int[] n = {0};
            doWhile("poll", () -> {
                n[0] += 1 + new Random().nextInt(3); // RAW_BODY
                tasks.step(r);
            }, () -> n[0] < 2);
            List.of(x).forEach(i -> note = i + System.nanoTime()); // RAW_LAMBDA
            return tasks.step(d + at);
        }

        private static String stamp() {
            return Instant.now().toString(); // RAW_HELPER
        }

        @Signal("poke")
        public void poke(String v) {
            note = v + System.currentTimeMillis(); // a handler is never replayed: not checked
        }
    }

    /** The same needs met with values that are recorded, seeded, or read from a clock given to them. */
    public static class Clean {
        private final Tasks tasks;

        public Clean(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "sd_clean")
        public String run(@InputParam("x") String x) {
            String id = once("id", () -> UUID.randomUUID().toString());
            double d = random();
            Instant at = now();
            int n = new Random(42).nextInt(10);
            LocalDate day = LocalDate.now(Clock.fixed(at, ZoneOffset.UTC));
            return tasks.step(id + d + at + n + day);
        }
    }

    private MockWebServer server;
    private Conductor conductor;
    private final Map<String, String> settings = new HashMap<>();
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

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
        logged.start();
        ((Logger) LoggerFactory.getLogger(Workers.class)).addAppender(logged);
    }

    @AfterEach
    void stop() throws Exception {
        ((Logger) LoggerFactory.getLogger(Workers.class)).detachAppender(logged);
        conductor.close();
        server.shutdown();
    }

    private static int line(String mark) throws Exception {
        List<String> source = java.nio.file.Files.readAllLines(
                java.nio.file.Path.of("src/test/java/strict/StrictDeterminismTest.java"));
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).trim().endsWith("// " + mark)) {
                return i + 1;
            }
        }
        throw new AssertionError(mark);
    }

    /** Each place as {@code line call -> replacement}, by line. */
    private static List<String> places(Class<?> workflow) throws Exception {
        return CodeReader.rawSources(workflow.getMethod("run", String.class)).stream()
                .map(r -> r.site().substring(r.site().lastIndexOf(':') + 1) + " " + r.call() + " -> " + r.replacement())
                .sorted()
                .toList();
    }

    private List<String> logged(Level level) {
        return logged.list.stream().filter(e -> e.getLevel() == level).map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.contains("sd_")).toList();
    }

    /** Whether the server hears from the workers within a few seconds: they register and poll at once. */
    private boolean heardFromWithin(int before, long millis) throws InterruptedException {
        long until = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < until) {
            if (server.getRequestCount() > before) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    @Test
    void everyRandomSourceAndClockReadTheWorkflowCanReachIsFoundWithItsLine() throws Exception {
        assertEquals(List.of(
                        line("RAW_DIRECT") + " Math.random() -> Conductor.random()",
                        line("RAW_CASE") + " UUID.randomUUID() -> Conductor.once(name, () -> UUID.randomUUID())",
                        line("RAW_BODY") + " new Random() -> Conductor.random()",
                        line("RAW_LAMBDA") + " System.nanoTime() -> Conductor.now()",
                        line("RAW_HELPER") + " Instant.now() -> Conductor.now()").stream().sorted().toList(),
                places(Raw.class));
    }

    @Test
    void recordedSeededAndGivenSourcesAreNotReported() throws Exception {
        assertEquals(List.of(), places(Clean.class));
    }

    @Test
    void byDefaultTheWorkflowIsHostedAndEachPlaceIsAWarning() throws Exception {
        int before = server.getRequestCount();
        Workers host = conductor.workflows(Raw.class, Clean.class).settings(settings::get).start();
        try {
            assertTrue(heardFromWithin(before, 5000), "the workers registered and polled");
            List<String> warnings = logged(Level.WARN);
            assertEquals(5, warnings.size(), warnings.toString());
            assertTrue(warnings.stream().allMatch(w -> w.contains("sd_raw") && w.contains("strictDeterminism=true")), warnings.toString());
            String direct = "Raw.run:" + line("RAW_DIRECT");
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Math.random()") && w.contains(direct)
                    && w.contains("Conductor.random()")), warnings.toString());
            assertEquals(List.of(), logged(Level.ERROR));
        } finally {
            host.close();
        }
    }

    @Test
    void withTheFlagTheWorkersDoNotStartAndTheErrorNamesEveryPlace() throws Exception {
        settings.put("conductor.workflow.all.strictDeterminism", "true");
        int before = server.getRequestCount();
        NonDeterminismException e = assertThrows(NonDeterminismException.class,
                () -> conductor.workflows(Raw.class, Clean.class).settings(settings::get).start());

        String message = e.getMessage();
        assertTrue(message.startsWith("Workflow sd_raw cannot be hosted: 5 places in its code are not deterministic."), message);
        for (String mark : List.of("RAW_DIRECT", "RAW_CASE", "RAW_BODY", "RAW_LAMBDA", "RAW_HELPER")) {
            assertTrue(message.contains(":" + line(mark) + " "), mark + " at line " + line(mark) + " in:\n" + message);
        }
        assertTrue(message.contains("Math.random()") && message.contains("use Conductor.random()"), message);
        assertTrue(message.contains("Instant.now()") && message.contains("use Conductor.now()"), message);
        assertTrue(message.contains("use Conductor.once(name, () -> UUID.randomUUID())"), message);
        assertTrue(message.contains("To host it anyway: conductor.workflow.sd_raw.strictDeterminism=false"), message);
        assertFalse(message.contains("sd_clean"), "a workflow with nothing to report is not named: " + message);
        assertEquals(List.of(message), logged(Level.ERROR));
        assertFalse(heardFromWithin(before, 500), "nothing was registered and nothing polls");
    }

    @Test
    void theFlagOfOneWorkflowOverridesTheOneForAll() throws Exception {
        settings.put("conductor.workflow.all.strictDeterminism", "true");
        settings.put("conductor.workflow.sd_raw.strictDeterminism", "false");
        int before = server.getRequestCount();
        Workers host = conductor.workflows(Raw.class, Clean.class).settings(settings::get).start();
        try {
            assertTrue(heardFromWithin(before, 5000), "the workers registered and polled");
            assertEquals(5, logged(Level.WARN).size(), logged(Level.WARN).toString());
        } finally {
            host.close();
        }

        settings.clear();
        settings.put("conductor.workflow.sd_raw.strictDeterminism", "true");
        assertThrows(NonDeterminismException.class,
                () -> conductor.workflows(Raw.class, Clean.class).settings(settings::get).start());
    }

    @Test
    void aWorkflowWithNothingToReportStartsUnderTheFlag() throws Exception {
        settings.put("conductor.workflow.all.strictDeterminism", "true");
        int before = server.getRequestCount();
        Workers host = conductor.workflows(Clean.class).settings(settings::get).start();
        try {
            assertTrue(heardFromWithin(before, 5000), "the workers registered and polled");
            assertEquals(List.of(), logged(Level.WARN));
        } finally {
            host.close();
        }
    }
}
