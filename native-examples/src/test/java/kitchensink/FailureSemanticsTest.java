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
package kitchensink;

import java.util.concurrent.atomic.AtomicInteger;

import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowFailedException;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Failure semantics: retries happen before a failure reaches the code; a terminal error is not
 * retried; a caught failure does not fail the workflow; an uncaught one does, also from a joined
 * async lane.
 */
class FailureSemanticsTest {

    public static class Tasks {
        final AtomicInteger flakyCalls = new AtomicInteger();
        final AtomicInteger terminalCalls = new AtomicInteger();

        @WorkerTask("flaky")
        public String flaky(int failures) {
            if (flakyCalls.incrementAndGet() <= failures) {
                throw new IllegalStateException("try again");
            }
            return "ok after " + flakyCalls.get();
        }

        @WorkerTask("terminal")
        public String terminal(String why) {
            terminalCalls.incrementAndGet();
            throw new NonRetryableException(why);
        }
    }

    public static class Flows {

        // Conductor passes a stand-in whose calls schedule the tasks; the real Tasks runs on the workers.
        private final Tasks tasks;

        public Flows(Tasks tasks) {
            this.tasks = tasks;
        }
        @Workflow(name = "retries_then_succeeds")
        public String retries(int failures) {
            return tasks.flaky(failures);
        }

        @Workflow(name = "catches_terminal")
        public String catches(String why) {
            try {
                return tasks.terminal(why);
            } catch (TaskFailure e) {
                return "handled " + e.getStatus();
            }
        }

        @Workflow(name = "uncaught_terminal")
        public String uncaught(String why) {
            return tasks.terminal(why);
        }

        @Workflow(name = "lane_fails_joined_without_get")
        public String laneFails(String why) {
            Promise<String> a = async(() -> tasks.flaky(0));
            Promise<String> b = async(() -> tasks.terminal(why));
            join(a, b);
            return "should not get here";
        }
    }

    private final Tasks tasks = new Tasks();

    private DurableTestEnvironment env() {
        return DurableTestEnvironment.inMemory().workflows(Flows.class).tasks(tasks);
    }

    @Test
    void retriesBeforeTheFailureReachesTheCode() {
        try (DurableTestEnvironment env = env()) {
            assertEquals("ok after 3", env.start(Flows::retries, 2).result());
            assertEquals(3, tasks.flakyCalls.get());
        }
    }

    @Test
    void terminalErrorIsNotRetriedAndCanBeHandled() {
        try (DurableTestEnvironment env = env()) {
            assertEquals("handled FAILED_WITH_TERMINAL_ERROR", env.start(Flows::catches, "no").result());
            assertEquals(1, tasks.terminalCalls.get());
        }
    }

    @Test
    void uncaughtTerminalErrorFailsTheWorkflow() {
        try (DurableTestEnvironment env = env()) {
            WorkflowFailedException e =
                    assertThrows(WorkflowFailedException.class, () -> env.start(Flows::uncaught, "over limit").result());
            assertTrue(e.getMessage().contains("over limit"));
        }
    }

    @Test
    void uncaughtFailureInAJoinedLaneFailsTheWorkflow() {
        try (DurableTestEnvironment env = env()) {
            assertThrows(WorkflowFailedException.class, () -> env.start(Flows::laneFails, "lane broke").result());
        }
    }
}
