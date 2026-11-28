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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.TaskFailure;
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

/**
 * A {@code repeatUntil} whose check fails on its last attempt: the code gets a {@link TaskFailure},
 * against a server and in the in-memory test environment alike.
 */
@SuppressWarnings("unchecked")
class RepeatUntilFailureTest {

    public record Status(boolean done) {}

    public static class Tasks {
        @WorkerTask(value = "ru_status", retryCount = 0)
        public Status status(@InputParam("key") String key) {
            throw new IllegalStateException("status service is down");
        }
    }

    public static class Poller {
        private final Tasks tasks;

        public Poller(Tasks tasks) {
            this.tasks = tasks;
        }

        @Workflow(name = "ru_poller")
        public String run(@InputParam("key") String key) {
            try {
                Status status = Conductor.repeatUntil(
                        () -> tasks.status(key), Status::done, Backoff.every(Duration.ofSeconds(1)).attempts(3));
                return "polled: " + status;
            } catch (TaskFailure e) {
                return "the check failed";
            }
        }
    }

    private static Map<String, Object> marker(long endTime) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (endTime > 0) {
            m.put("endTime", endTime);
        }
        m.put("leased", endTime == 0);
        m.put("resumedWith", Map.of());
        m.put("recorded", Map.of());
        return m;
    }

    private static Execution execution(Object defTasks, List<Map<String, Object>> tasks, Map<String, Object> bootstrap) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowId", "wf-1");
        json.put("status", "RUNNING");
        json.put("input", Map.of("key", "k"));
        json.put("workflowDefinition", Map.of("name", "ru", "tasks", defTasks));
        json.put("tasks", tasks);
        json.put("nativeState", Map.of("bootstrap", bootstrap));
        return new Execution(json);
    }

    private static Map<String, Object> task(String ref, String type, String status, int seq) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", "id-" + ref);
        t.put("referenceTaskName", ref);
        t.put("taskType", type);
        t.put("status", status);
        t.put("seq", seq);
        t.put("outputData", Map.of());
        t.put("workflowTask", Map.of("type", type));
        return t;
    }

    @Test
    void againstAServerTheCodeGetsTheFailureOfTheLastCheck() throws Exception {
        WorkflowSpec spec = WorkflowSpec.of(Poller.class).get(0);
        Replayer.Outcome started = new Replayer(spec, spec.newInstance(), execution(List.of(), List.of(), marker(0)))
                .process("main", 1);
        Object drawn = Json.MAPPER.readValue(Json.MAPPER.writeValueAsString(started.output.get("fragment")), Object.class);
        // the loop ran its three attempts and ended; the last check failed
        Map<String, Object> loop = task("poll", "DO_WHILE", "COMPLETED", 1);
        loop.put("workflowTask", Map.of("type", "DO_WHILE", "metadata", Map.of("lane", "main", "continuation", 2)));
        loop.put("continuation", marker(0));
        Map<String, Object> lastCheck = task("poll_check__3", "ru_status", "FAILED", 2);
        lastCheck.put("reasonForIncompletion", "status service is down");

        Replayer.Outcome out = new Replayer(spec, spec.newInstance(), execution(drawn, List.of(loop, lastCheck), marker(1)))
                .process("main", 2);

        assertEquals(Map.of("result", "the check failed"), out.output.get("result"));
    }

    @Test
    void inMemoryTheCodeGetsTheFailureOfTheLastCheckToo() {
        try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
            env.workflows(Poller.class).tasks(new Tasks());
            assertEquals("the check failed", env.start(Poller::run, "k").result());
        }
    }
}
