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
package returnvalues;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What code returns, and what it gets back: a lane that returns null, a value recorded with
 * {@code once}, and results that are neither an object nor one of the usual scalars. Outside the
 * SDK's packages, as application code is.
 */
@SuppressWarnings("unchecked")
class ReturnValuesTest {

    public static class Tasks {
        @WorkerTask("rv_audit")
        public String audit(@InputParam("x") String x) {
            return x;
        }
    }

    // ------------------------------------------------------------------ executions, as the server keeps them

    private static WorkflowSpec spec(Class<?> type) {
        return WorkflowSpec.of(type).get(0);
    }

    /** A marker as the server keeps it: leased, or applied {@code endTime}-th with what it recorded. */
    private static Map<String, Object> marker(long endTime, Object recorded) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (endTime > 0) {
            m.put("endTime", endTime);
        }
        m.put("leased", endTime == 0);
        m.put("resumedWith", Map.of());
        m.put("recorded", recorded == null ? Map.of() : recorded);
        return m;
    }

    private static Execution execution(Map<String, Object> input, Object defTasks, List<Map<String, Object>> tasks,
                                       Map<String, Object> bootstrap) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", input);
        json.put("workflowDefinition", Map.of("name", "rv", "tasks", defTasks));
        json.put("tasks", tasks);
        json.put("nativeState", Map.of("bootstrap", bootstrap));
        return new Execution(json);
    }

    /** A finished task the code paused on in {@code lane}, whose continuation {@code n} is leased. */
    private static Map<String, Object> done(String ref, int seq, Map<String, Object> output, String lane, int n) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", "id-" + ref);
        t.put("referenceTaskName", ref);
        t.put("taskType", ref);
        t.put("status", "COMPLETED");
        t.put("seq", seq);
        t.put("outputData", output);
        t.put("workflowTask", Map.of("type", "SIMPLE", "metadata", Map.of("lane", lane, "continuation", n)));
        t.put("continuation", marker(0, null));
        return t;
    }

    private static Replayer.Outcome first(Class<?> type, Map<String, Object> input) {
        WorkflowSpec spec = spec(type);
        return new Replayer(spec, spec.newInstance(), execution(input, List.of(), List.of(), marker(0, null)))
                .process("main", 1);
    }

    /** What the server stores of a completion and returns with the execution: its JSON. */
    private static <T> T asStored(Object value) throws Exception {
        return (T) Json.MAPPER.readValue(Json.MAPPER.writeValueAsString(value), Object.class);
    }

    // ------------------------------------------------------------------ a lane that returns null

    public static class NullLane {
        private final Tasks tasks;

        public NullLane(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "rv_null_lane")
        public String run(@InputParam("id") String id) {
            Future<Object> audited = Conductor.async(() -> {
                tasks.audit(id);
                return null;
            });
            Conductor.join(audited);
            return "ok";
        }
    }

    @Test
    void aLaneThatPausedOnATaskAndReturnsNullCompletes() throws Exception {
        Replayer.Outcome started = first(NullLane.class, Map.of("id", "42"));
        // the first continuation drew the fork; its task finished, and the lane resumes after it
        Execution exec = execution(Map.of("id", "42"), asStored(started.output.get("fragment")),
                List.of(done("rv_audit", 2, Map.of("result", "42"), "fork_1.0", 1)), marker(1, null));
        WorkflowSpec spec = spec(NullLane.class);

        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), exec).process("fork_1.0", 1);

        assertEquals(true, out.output.get("done"));
        assertNull(out.output.get("error"));
    }

    // ------------------------------------------------------------------ once

    public record Order(int id) {}

    public static class Recorded {
        private final Tasks tasks;

        public Recorded(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "rv_recorded")
        public String run(@InputParam("x") String x) {
            List<Order> orders = Conductor.once("orders", () -> List.of(new Order(7)));
            tasks.audit(x);
            return "order " + orders.get(0).id();
        }
    }

    @Test
    void aListRecordedWithOnceIsTheSameListAfterTheNextTask() throws Exception {
        Replayer.Outcome started = first(Recorded.class, Map.of("x", "a"));
        assertEquals(false, started.output.get("done"));
        // the server applied the first continuation, with what it recorded; the task finished
        Execution exec = execution(Map.of("x", "a"), asStored(started.output.get("fragment")),
                List.of(done("rv_audit", 1, Map.of("result", "a"), "main", 2)),
                marker(1, asStored(started.output.get("recorded"))));
        WorkflowSpec spec = spec(Recorded.class);

        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), exec).process("main", 2);

        assertNull(out.output.get("error"), "the workflow failed reading back what once recorded");
        assertEquals(Map.of("result", "order 7"), out.output.get("result"));
    }

    // ------------------------------------------------------------------ results that are one value

    public static class Reserve {
        @Workflow(name = "rv_reserve")
        public UUID run(@InputParam("id") String id) {
            return UUID.fromString(id);
        }
    }

    @Test
    void aWorkflowReturningAUuidCompletesWithItAsItsResult() {
        String id = "123e4567-e89b-12d3-a456-426614174000";

        Replayer.Outcome out = first(Reserve.class, Map.of("id", id));

        assertEquals(true, out.output.get("done"));
        assertEquals(Map.of("result", id), out.output.get("result"));
        assertEquals(UUID.fromString(id), Json.decodeOutput((Map<String, Object>) out.output.get("result"), UUID.class));
    }

    @Test
    void aDurationAndADateEncodeAsOneValueAndDecodeBack() {
        Duration wait = Duration.ofMinutes(5);
        assertEquals(wait, Json.decodeOutput(Json.encodeOutput(wait), Duration.class));
        Date at = new Date(1_700_000_000_000L);
        assertEquals(at, Json.decodeOutput(Json.encodeOutput(at), Date.class));
    }

    public static class ByUuid {
        @Workflow(name = "rv_by_uuid")
        public String run(@InputParam("id") UUID id) {
            return id.toString();
        }
    }

    @Test
    void aSingleUuidParameterIsSentByNameAndReadBack() throws Exception {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Method run = ByUuid.class.getMethod("run", UUID.class);

        Map<String, Object> input = WorkflowSpec.encodeInput(run, new Object[] {id});

        assertEquals(Map.of("id", id.toString()), input);
        assertEquals(id, WorkflowSpec.decodeInput(run, input)[0]);
    }

    public enum Size {
        SMALL,
        LARGE {
            @Override
            public String toString() {
                return "large";
            }
        }
    }

    @Test
    void recordedValuesReadBackAsWhatTheyWere() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        List<Object> values = List.of(
                "text", 7, 2.5, true, new Order(1), Size.LARGE,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                Instant.parse("2026-01-02T03:04:05.123456789Z"),
                Duration.ofSeconds(90),
                List.of(new Order(1), new Order(2)),
                List.of(List.of(new Order(1)), List.of(new Order(2), new Order(3))),
                new LinkedHashSet<>(List.of(Size.SMALL, Size.LARGE)),
                Map.of("a", new Order(1)),
                List.of(),
                Arrays.asList("mixed", 1));
        for (Object value : values) {
            // through the server and back: only the JSON survives
            Map<String, Object> stored = asStored(Json.record(value));
            assertEquals(value, Json.recorded(stored, loader), "recorded as " + stored);
        }
        String[] array = {"a", "b"};
        assertEquals(List.of(array), List.of((String[]) Json.recorded(asStored(Json.record(array)), loader)));
        assertNull(Json.recorded(asStored(Json.record(null)), loader));
    }

    @Test
    void aValueRecordedByAnEarlierBuildByItsClassNameStillReadsBack() {
        Map<String, Object> earlier = Map.of("value", "2026-01-02T03:04:05Z", "type", "java.time.Instant");
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), Json.recorded(earlier, getClass().getClassLoader()));
    }

    public static class RecordedInMemory {
        @Workflow(name = "rv_recorded_in_memory")
        public String run(@InputParam("x") String x) {
            Object first = Conductor.once("orders", () -> List.of(new Order(7)));
            return first.getClass().getSimpleName() + " of " + ((List<?>) first).get(0).getClass().getSimpleName();
        }
    }

    @Test
    void onceGivesTheCodeTheSameValueOnItsFirstRunAsOnAReRunAndInMemory() {
        // the first run, against a server: what was recorded, read back
        assertEquals(Map.of("result", "ArrayList of Order"), first(RecordedInMemory.class, Map.of("x", "a")).output.get("result"));
        try (DurableTestEnvironment env =
                DurableTestEnvironment.inMemory()) {
            env.workflows(RecordedInMemory.class);
            assertEquals("ArrayList of Order", env.start(RecordedInMemory::run, "a").result());
        }
    }
}
