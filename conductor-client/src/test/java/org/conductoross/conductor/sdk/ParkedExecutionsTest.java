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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.conductoross.conductor.sdk.internal.Execution;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskContinuation;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An execution's code stays in memory between its continuations, pinned to the worker that holds
 * it: what a worker fetches, replays and tells the server, against a server that answers by path.
 */
class ParkedExecutionsTest {

    static final Tasks TASKS = Conductor.stub(Tasks.class);
    static final String CODE_TYPE = "pk_two.v1.code";

    public static class Tasks {
        @WorkerTask("pk_one")
        public String one(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("pk_two")
        public String two(@InputParam("x") String x) {
            return x;
        }
    }

    /** How many times the method ran from its first line, in this JVM. */
    static final AtomicInteger RAN_FROM_THE_TOP = new AtomicInteger();

    public static class TwoSteps {
        @Variable
        String stage = "new";

        @Workflow(name = "pk_two")
        public String run(@InputParam("order") String order) {
            RAN_FROM_THE_TOP.incrementAndGet();
            String a = TASKS.one(order);
            String was = stage; // a signal handler may have changed it while the task ran
            stage = "half";
            String b = TASKS.two(a);
            return a + b + ":" + was;
        }
    }

    /** The execution before its code ran at all. */
    private static final String FRESH = "{\"workflowId\":\"wf-1\",\"status\":\"RUNNING\",\"input\":{\"order\":\"o\"},"
            + "\"variables\":{},\"workflowDefinition\":{\"name\":\"pk_two\",\"tasks\":[]},\"tasks\":[],"
            + "\"nativeState\":{\"bootstrap\":{\"leased\":true}}}";

    /** The execution once its first continuation applied and pk_one completed. */
    private static final String AFTER_ONE = "{\"workflowId\":\"wf-1\",\"status\":\"RUNNING\",\"input\":{\"order\":\"o\"},"
            + "\"variables\":{\"stage\":\"new\"},\"workflowDefinition\":{\"name\":\"pk_two\",\"tasks\":["
            + "{\"name\":\"pk_one\",\"taskReferenceName\":\"pk_one\",\"type\":\"SIMPLE\","
            + "\"metadata\":{\"lane\":\"main\",\"continuation\":2}}]},"
            + "\"tasks\":[{\"taskId\":\"id-pk_one\",\"referenceTaskName\":\"pk_one\",\"taskType\":\"pk_one\","
            + "\"status\":\"COMPLETED\",\"seq\":1,\"outputData\":{\"result\":\"A\"},"
            + "\"workflowTask\":{\"type\":\"SIMPLE\",\"metadata\":{\"lane\":\"main\",\"continuation\":2}},"
            + "\"continuation\":{\"leased\":true}}],"
            + "\"nativeState\":{\"bootstrap\":{\"endTime\":1}}}";

    private MockWebServer server;
    private Conductor conductor;
    private Workers workers;
    private volatile String execution = FRESH;
    private final AtomicInteger fetched = new AtomicInteger();
    private final Set<String> polledDomains = ConcurrentHashMap.newKeySet();
    private final Map<String, String> settings = new HashMap<>();

    @BeforeEach
    void start() throws Exception {
        RAN_FROM_THE_TOP.set(0);
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (!request.getMethod().equals("GET")) {
                    return json("\"ok\"");
                }
                if (path.startsWith("/api/workflow/wf-1")) {
                    fetched.incrementAndGet();
                    return json(execution);
                }
                if (path.startsWith("/api/tasks/poll/batch/")) {
                    String domain = request.getRequestUrl().queryParameter("domain");
                    polledDomains.add(domain == null ? "" : domain);
                }
                return json("[]");
            }
        });
        server.start();
        conductor = Conductor.of(ConductorClient.builder().basePath(server.url("/api").toString()).build());
    }

    @AfterEach
    void stop() throws Exception {
        if (workers != null) {
            workers.close();
        }
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    private Workers hosted() {
        workers = conductor.workflows(TwoSteps.class).settings(settings::get).start();
        return workers;
    }

    /** A continuation as a poll hands it out; {@code variables} null is a server that pins nothing. */
    private static Task continuation(String taskId, String ref, int n, String status, Map<String, Object> output,
                                     String stamp, Map<String, Object> variables) {
        WorkflowTask entry = new WorkflowTask();
        entry.setTaskReferenceName(ref);
        entry.setMetadata(new HashMap<>(Map.of("lane", "main", "continuation", n)));
        TaskContinuation marker = new TaskContinuation();
        marker.setLeased(true);
        marker.setStamp(stamp);
        marker.setVariables(variables);
        Task task = new Task();
        task.setWorkflowTask(entry);
        task.setTaskId(taskId);
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName(ref);
        task.setTaskType(ref);
        task.setStatus(Task.Status.valueOf(status));
        task.setOutputData(output);
        task.setContinuation(marker);
        return task;
    }

    private static Task first(Map<String, Object> variables) {
        return continuation("wf-1|main", "main", 1, "IN_PROGRESS", Map.of(), null, variables);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pin(TaskResult result) {
        return (Map<String, Object>) result.getOutputData().get("pin");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> refs(TaskResult result) {
        return ((List<Map<String, Object>>) result.getOutputData().get("fragment")).stream()
                .map(e -> e.get("taskReferenceName")).toList();
    }

    @Test
    void theNextContinuationOfAnExecutionHeldInMemoryFetchesNothingAndRunsNothingAgain() {
        hosted();

        TaskResult one = workers.process(CODE_TYPE, first(Map.of()));

        assertEquals(List.of("pk_one"), refs(one));
        assertEquals(1, fetched.get());
        // the completion tells the server where the execution is held, and names itself
        assertEquals(workers.pinDomain(), pin(one).get("domain"));
        String stamp = (String) pin(one).get("stamp");
        assertEquals(1, workers.parked().held(CODE_TYPE));

        // the server hands the stamp back with the task the code waited for, and the variables: a
        // signal handler set the stage meanwhile
        TaskResult two = workers.process(CODE_TYPE, continuation("id-pk_one", "pk_one", 2, "COMPLETED",
                Map.of("result", "A"), stamp, Map.of("stage", "signalled")));

        assertEquals(TaskResult.Status.COMPLETED, two.getStatus());
        assertEquals(List.of("pk_two"), refs(two));
        assertEquals(1, fetched.get()); // nothing fetched
        assertEquals(1, RAN_FROM_THE_TOP.get()); // nothing run again
        assertEquals(Map.of("stage", "signalled"), two.getOutputData().get("resumedWith"));
        assertEquals(Map.of("stage", "half"), two.getOutputData().get("variables"));
        assertEquals(2, two.getOutputData().get("n"));
        String next = (String) pin(two).get("stamp");
        assertNotEquals(stamp, next);

        TaskResult three = workers.process(CODE_TYPE, continuation("id-pk_two", "pk_two", 3, "COMPLETED",
                Map.of("result", "B"), next, Map.of("stage", "half")));

        assertEquals(true, three.getOutputData().get("done"));
        assertEquals(Map.of("result", "AB:signalled"), three.getOutputData().get("result"));
        assertEquals(1, fetched.get());
        assertEquals(1, RAN_FROM_THE_TOP.get());
        // the code returned: there is nothing to hold, and the completion says so
        assertNull(pin(three));
        assertEquals(0, workers.parked().held(CODE_TYPE));
        assertEquals(2, workers.parked().continued());
        assertEquals(1, workers.parked().replayed());
    }

    @Test
    void anExecutionThatMovedOnWithoutTheCodeHeldInMemoryIsReplayed() {
        hosted();
        workers.process(CODE_TYPE, first(Map.of()));

        // another host continued it (the pin timeout passed), or it was retried or rerun: the
        // server names another stamp
        execution = AFTER_ONE;
        TaskResult two = workers.process(CODE_TYPE, continuation("id-pk_one", "pk_one", 2, "COMPLETED",
                Map.of("result", "A"), "pin-other.7", Map.of("stage", "new")));

        assertEquals(List.of("pk_two"), refs(two));
        assertEquals(2, fetched.get());
        assertEquals(2, RAN_FROM_THE_TOP.get());
        assertEquals(0, workers.parked().continued());
        // it is held again, from the replay
        assertEquals(workers.pinDomain(), pin(two).get("domain"));
        assertEquals(1, workers.parked().held(CODE_TYPE));
    }

    @Test
    void anExecutionWithNoStampIsReplayed() {
        hosted();
        workers.process(CODE_TYPE, first(Map.of()));

        // a retry or rerun clears the stamp
        execution = AFTER_ONE;
        workers.process(CODE_TYPE, continuation("id-pk_one", "pk_one", 2, "COMPLETED",
                Map.of("result", "A"), null, Map.of("stage", "new")));

        assertEquals(2, fetched.get());
        assertEquals(2, RAN_FROM_THE_TOP.get());
    }

    @Test
    void aContinuationTheHeldCodeAlreadyRanIsReplayed() {
        hosted();
        TaskResult one = workers.process(CODE_TYPE, first(Map.of()));

        // the same continuation again, with this process's own stamp: the code in memory is past it
        Task again = first(Map.of());
        again.getContinuation().setStamp((String) pin(one).get("stamp"));
        TaskResult repeated = workers.process(CODE_TYPE, again);

        assertEquals(List.of("pk_one"), refs(repeated));
        assertEquals(2, fetched.get());
        assertEquals(2, RAN_FROM_THE_TOP.get());
    }

    @Test
    void aServerThatDoesNotPinExecutionsHasNothingKeptForIt() {
        hosted();

        TaskResult one = workers.process(CODE_TYPE, first(null)); // no variables with the continuation

        assertEquals(List.of("pk_one"), refs(one));
        assertNull(pin(one));
        assertEquals(0, workers.parked().held(CODE_TYPE));
    }

    @Test
    void aParkTimeoutOfZeroKeepsNothingAndPollsNoQueueOfItsOwn() throws Exception {
        settings.put("conductor.workflow.pk_two.parkTimeout", "0");
        hosted();

        TaskResult one = workers.process(CODE_TYPE, first(Map.of()));

        assertNull(pin(one));
        assertNull(workers.pinDomain());
        assertEquals(0, workers.parked().held(CODE_TYPE));
        Thread.sleep(400);
        assertEquals(Set.of(""), polledDomains);
    }

    @Test
    void devModeKeepsNothingSoThatEveryContinuationReplays() {
        workers = conductor.workflows(TwoSteps.class).settings(settings::get).devMode(true).start();

        TaskResult one = workers.process(CODE_TYPE, first(Map.of()));

        assertEquals(List.of("pk_one"), refs(one));
        assertNull(pin(one));
        assertNull(workers.pinDomain());
        assertEquals(0, workers.parked().held(CODE_TYPE));
    }

    @Test
    void itsOwnQueueIsPolledOnlyWhileItHoldsAnExecution() throws Exception {
        hosted();
        Thread.sleep(400);
        // nothing held: a queue nobody polls reads as a worker that is gone
        assertEquals(Set.of(""), polledDomains);

        TaskResult one = workers.process(CODE_TYPE, first(Map.of()));
        awaitPolled(workers.pinDomain());

        TaskResult two = workers.process(CODE_TYPE, continuation("id-pk_one", "pk_one", 2, "COMPLETED",
                Map.of("result", "A"), (String) pin(one).get("stamp"), Map.of()));
        workers.process(CODE_TYPE, continuation("id-pk_two", "pk_two", 3, "COMPLETED",
                Map.of("result", "B"), (String) pin(two).get("stamp"), Map.of()));
        Thread.sleep(400); // a poll under way when the last one ended
        polledDomains.clear();
        Thread.sleep(500);

        assertEquals(Set.of(""), polledDomains);
    }

    private void awaitPolled(String domain) throws InterruptedException {
        long until = System.currentTimeMillis() + 5000;
        while (!polledDomains.contains(domain) && System.currentTimeMillis() < until) {
            Thread.sleep(20);
        }
        assertTrue(polledDomains.contains(domain), "polled " + polledDomains + ", not " + domain);
    }

    @Test
    void aPinDomainIsThisHostsAndNewOnEveryStart() {
        String a = Workers.newPinDomain("orders host:8080/eu");
        String b = Workers.newPinDomain("orders host:8080/eu");

        // only what a queue name may hold: the server refuses anything else
        assertTrue(a.matches("pin-orders_host_8080_eu-[A-Za-z0-9]{1,8}"), a);
        assertNotEquals(a, b);
        assertTrue(Workers.newPinDomain(null).matches("pin-[A-Za-z0-9]{1,8}"));
        assertTrue(Workers.newPinDomain("x".repeat(300)).length() <= 100);
    }

    // ------------------------------------------------------------------ what is held, and for how long

    private static Replayer paused() {
        WorkflowSpec spec = WorkflowSpec.of(TwoSteps.class).get(0);
        Map<String, Object> json = new HashMap<>(Map.of("workflowId", "wf", "status", "RUNNING",
                "input", Map.of("order", "o"), "workflowDefinition", Map.of("name", "pk_two", "tasks", List.of()),
                "tasks", List.of(), "nativeState", Map.of("bootstrap", Map.of())));
        Replayer replayer = new Replayer(spec, spec.newInstance(), new Execution(json));
        replayer.begin("main", 1);
        assertTrue(replayer.hasPausedLanes());
        return replayer;
    }

    @Test
    void anExecutionThatSatIdleTooLongIsDropped() {
        AtomicLong now = new AtomicLong(10_000);
        ParkedExecutions parked = new ParkedExecutions(now::get);
        Replayer early = paused();
        Replayer late = paused();
        parked.park(CODE_TYPE, "wf-early", early, "s-1", 1_000, 10);
        now.addAndGet(600);
        parked.park(CODE_TYPE, "wf-late", late, "s-2", 1_000, 10);

        now.addAndGet(600); // 1200 ms after the first, 600 after the second
        parked.dropIdle();

        assertNull(parked.take(CODE_TYPE, "wf-early"));
        assertFalse(early.hasPausedLanes()); // its lanes were ended
        assertEquals(1, parked.dropped());
        assertEquals(1, parked.held(CODE_TYPE));
        assertTrue(late.hasPausedLanes());
        parked.closeAll();
        assertFalse(late.hasPausedLanes());
    }

    @Test
    void whenTooManyAreHeldTheOnesIdleLongestAreDropped() {
        ParkedExecutions parked = new ParkedExecutions();
        Replayer a = paused();
        Replayer b = paused();
        Replayer c = paused();
        parked.park(CODE_TYPE, "wf-a", a, "s-1", 60_000, 2);
        parked.park(CODE_TYPE, "wf-b", b, "s-2", 60_000, 2);
        // wf-a continues: it is taken out, and parked again as the most recent
        assertTrue(parked.take(CODE_TYPE, "wf-a").replayer() == a);
        parked.park(CODE_TYPE, "wf-a", a, "s-3", 60_000, 2);

        parked.park(CODE_TYPE, "wf-c", c, "s-4", 60_000, 2);

        assertFalse(b.hasPausedLanes());
        assertTrue(a.hasPausedLanes());
        assertTrue(c.hasPausedLanes());
        assertEquals(2, parked.held(CODE_TYPE));
        assertEquals(0, parked.held("another.v1.code")); // counted by workflow
        parked.closeAll();
    }

    @Test
    void anExecutionIsOutOfTheCacheWhileOneOfItsContinuationsRuns() {
        ParkedExecutions parked = new ParkedExecutions();
        Replayer held = paused();
        parked.park(CODE_TYPE, "wf-1", held, "s-1", 60_000, 10);

        ParkedExecutions.Parked taken = parked.take(CODE_TYPE, "wf-1");

        assertEquals("s-1", taken.stamp());
        // a second thread handed the same continuation finds nothing, and replays
        assertNull(parked.take(CODE_TYPE, "wf-1"));
        assertEquals(0, parked.held(CODE_TYPE));
        held.close();
    }
}
