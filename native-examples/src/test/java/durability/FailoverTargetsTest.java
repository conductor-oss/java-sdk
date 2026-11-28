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
package durability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import durability.FailoverTargets.Fault;
import durability.FailoverTargets.Handover;
import durability.FailoverTargets.Outcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the failover bench reads from an execution's records, on records made up for each case. */
class FailoverTargetsTest {

    private static final long KILL = 100_000;

    private static Map<String, Object> task(String type, String ref, String status, Map<String, Object> meta) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("taskId", ref + "-id");
        t.put("taskType", type);
        t.put("referenceTaskName", ref);
        t.put("status", status);
        t.put("workflowTask", Map.of("metadata", meta));
        return t;
    }

    private static Map<String, Object> wf(String status, Object pinnedTo, List<Map<String, Object>> tasks,
            List<Object> definition) {
        Map<String, Object> wf = new LinkedHashMap<>();
        wf.put("status", status);
        wf.put("createTime", 1_000L);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("pinnedTo", pinnedTo);
        wf.put("nativeState", state);
        wf.put("tasks", tasks);
        wf.put("workflowDefinition", Map.of("tasks", definition));
        return wf;
    }

    private static Set<String> featuresOf(Map<String, Object> task) {
        return FailoverTargets.features(wf("RUNNING", null, List.of(task), List.of()));
    }

    @Test
    void eachTargetIsReadFromTheTaskInFlight() {
        assertEquals(Set.of("long-task"), featuresOf(task("slow_step", "slow_step", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("java-loop"), featuresOf(task("step", "step_3", "SCHEDULED",
                Map.of("loops", List.of("KitchenSinkWorkflow_loop_343")))));
        assertEquals(Set.of("doWhile"), featuresOf(task("label_item", "label_item_9", "SCHEDULED",
                Map.of("loops", List.of("label_rounds")))));
        assertEquals(Set.of("server-loop"), featuresOf(task("DO_WHILE", "poll", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("server-loop"), featuresOf(task("WAIT", "poll_wait__2", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("fork"), featuresOf(task("step", "step_5", "SCHEDULED", Map.of("lane", "fork_2.1"))));
        assertEquals(Set.of("timer"), featuresOf(task("WAIT", "sleep_2", "IN_PROGRESS", Map.of("lane", "main"))));
        assertEquals(Set.of("timer"), featuresOf(task("WAIT", "sleep_until", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("external"), featuresOf(task("WAIT", "callback", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("human"), featuresOf(task("HUMAN", "approval", "IN_PROGRESS", Map.of())));
        assertEquals(Set.of("child"), featuresOf(task("SUB_WORKFLOW", "sub_flow_1", "IN_PROGRESS", Map.of())));
        // done, or only scheduled to run: nothing in flight there
        assertEquals(Set.of(), featuresOf(task("HUMAN", "approval", "COMPLETED", Map.of())));
        assertEquals(Set.of(), featuresOf(task("slow_step", "slow_step", "SCHEDULED", Map.of())));
    }

    @Test
    void aTaskInsideASwitchCaseOrAForkLaneOfTheDefinitionIsThere() {
        List<Object> definition = List.of(
                Map.of("taskReferenceName", "if_pass", "type", "SWITCH",
                        "decisionCases", Map.of("true", List.of(Map.of("taskReferenceName", "step_2"))),
                        "defaultCase", List.of(Map.of("taskReferenceName", "step_9"))),
                Map.of("taskReferenceName", "fork_3", "type", "FORK_JOIN",
                        "forkTasks", List.of(List.of(Map.of("taskReferenceName", "label_item_4")))),
                Map.of("taskReferenceName", "poll", "type", "DO_WHILE",
                        "loopOver", List.of(Map.of("taskReferenceName", "check_status_7"))));
        assertEquals(Set.of("switch"), FailoverTargets.features(wf("RUNNING", null,
                List.of(task("step", "step_2", "SCHEDULED", Map.of())), definition)));
        assertEquals(Set.of("switch"), FailoverTargets.features(wf("RUNNING", null,
                List.of(task("step", "step_9", "IN_PROGRESS", Map.of())), definition)));
        assertEquals(Set.of("fork"), FailoverTargets.features(wf("RUNNING", null,
                List.of(task("label_item", "label_item_4", "SCHEDULED", Map.of())), definition)));
        // an iteration's copy of a task the server loop runs
        assertEquals(Set.of("server-loop"), FailoverTargets.features(wf("RUNNING", null,
                List.of(task("check_status", "check_status_7__3", "SCHEDULED", Map.of())), definition)));
    }

    @Test
    void aLeasedContinuationIsTheCodeRunning() {
        Map<String, Object> waited = task("step", "step", "COMPLETED", Map.of());
        waited.put("continuation", Map.of("leased", true));
        assertEquals(Set.of("continuation"), featuresOf(waited));
        Map<String, Object> starting = wf("RUNNING", null, List.of(), List.of());
        ((Map<String, Object>) starting.get("nativeState")).put("bootstrap", Map.of("leased", true));
        assertEquals(Set.of("continuation"), FailoverTargets.features(starting));
    }

    @Test
    void theHolderIsTheWorkerRunningTheSlowStepOrTheOneItIsPinnedTo() {
        assertEquals("w1-g3", FailoverTargets.pinnedWorker("pin-w1-g3-abc123"));
        assertEquals("w0", FailoverTargets.pinnedWorker("pin-w0-x9"));
        assertNull(FailoverTargets.pinnedWorker("pin-abc123")); // no worker id in it
        assertNull(FailoverTargets.pinnedWorker(null));
        assertNull(FailoverTargets.pinnedWorker("blue"));

        Map<String, Object> wf = wf("RUNNING", "pin-w0-aaaa",
                List.of(task("slow_step", "slow_step", "IN_PROGRESS", Map.of())), List.of());
        assertEquals("w2", FailoverTargets.holder(wf, "long-task", Map.of("slow_step-id", "w2")));
        assertEquals("w0", FailoverTargets.holder(wf, "timer", Map.of()));
        assertNull(FailoverTargets.holder(wf("RUNNING", null, List.of(), List.of()), "timer", Map.of()));
    }

    /** An execution that paused on a timer at 90 s, which fired at 110 s; the next continuation applied at {@code applied}. */
    private static Map<String, Object> pausedOnTimer(String status, long applied) {
        Map<String, Object> timer = task("WAIT", "sleep", "COMPLETED", Map.of("lane", "main"));
        timer.put("endTime", 110_000L);
        timer.put("continuation", Map.of("endTime", applied));
        return wf(status, null, new ArrayList<>(List.of(timer)), List.of());
    }

    private static Fault kill(String target, String worker, String slowStep) {
        return new Fault("kill", KILL, Long.MAX_VALUE, "wf", target, worker, slowStep, Set.of(target));
    }

    /** {@code type} on w0 from the kill time for 100 s. */
    private static Fault withheld(String type) {
        return new Fault(type, KILL, KILL + 100_000, "wf", "timer", "w0", null, Set.of("timer"));
    }

    private static List<Map<String, Object>> pins(Object... samples) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < samples.length; i += 2) {
            out.add(Processes.mapOf("t", samples[i], "pinnedTo", samples[i + 1]));
        }
        return out;
    }

    @Test
    void aKillIsHandedOverWhenAnotherWorkerContinuedAndTheExecutionCompleted() {
        Fault kill = kill("timer", "w0", null);
        Handover h = FailoverTargets.handover(kill, pausedOnTimer("COMPLETED", 112_000),
                pins(90_000L, "pin-w0-aaaa", 112_100L, "pin-w2-bbbb"), Map.of(), List.of());
        assertTrue(h.ok(), h.why());
        assertEquals("w2", h.continuedBy());
        // the timer fired 10 s after the kill: only the 2 s after it count as the failover's
        assertEquals(2_000L, h.delayMs());
    }

    @Test
    void aKillIsNotHandedOverWithoutAContinuationAfterIt() {
        Fault kill = kill("timer", "w0", null);
        // nothing applied after the kill, and the execution is still pinned to the dead worker
        Handover stuck = FailoverTargets.handover(kill, pausedOnTimer("RUNNING", 95_000),
                pins(90_000L, "pin-w0-aaaa"), Map.of(), List.of());
        assertFalse(stuck.ok());
        assertEquals("no continuation applied after the kill", stuck.why());
        // a completion the dead worker sent as it was killed does not count
        assertFalse(FailoverTargets.handover(kill, pausedOnTimer("RUNNING", KILL + 500), pins(), Map.of(), List.of()).ok());
    }

    @Test
    void aKillIsNotHandedOverWhenTheExecutionDidNotComplete() {
        Fault kill = kill("timer", "w0", null);
        Handover h = FailoverTargets.handover(kill, pausedOnTimer("FAILED", 112_000),
                pins(112_100L, "pin-w1-cccc"), Map.of(), List.of());
        assertFalse(h.ok());
        assertEquals("taken over by w1, but ended FAILED", h.why());
    }

    @Test
    void aKilledSlowStepMustRunAgainOnAnotherWorker() {
        Map<String, Object> killed = task("slow_step", "slow_step", "TIMED_OUT", Map.of());
        killed.put("taskId", "attempt-1");
        Map<String, Object> retried = task("slow_step", "slow_step", "COMPLETED", Map.of());
        retried.put("taskId", "attempt-2");
        retried.put("startTime", KILL + 60_000);
        Map<String, Object> wf = wf("COMPLETED", null, List.of(killed, retried), List.of());
        Fault kill = kill("long-task", "w1", "attempt-1");

        Handover h = FailoverTargets.handover(kill, wf, pins(), Map.of("attempt-1", "w1", "attempt-2", "w0"), List.of());
        assertTrue(h.ok(), h.why());
        assertEquals("w0", h.continuedBy());
        assertEquals(60_000L, h.delayMs());

        // the same worker ran it again: it was not killed, or this is not a failover
        assertFalse(FailoverTargets.handover(kill, wf, pins(), Map.of("attempt-1", "w1", "attempt-2", "w1"), List.of()).ok());
    }

    @Test
    void aPauseIsHandedOverWhenWhatCameDueDidNotWaitForTheWorker() {
        // the timer fired 10 s into a 100 s pause; another worker applied the continuation 30 s later
        Handover h = FailoverTargets.handover(withheld("pause"), pausedOnTimer("COMPLETED", 140_000),
                pins(141_000L, "pin-w1-dddd"), Map.of(), List.of());
        assertEquals(Outcome.HANDED_OVER, h.outcome(), h.why());
        assertEquals("w1", h.continuedBy());
        assertEquals(30_000L, h.delayMs());
        // a partition is judged the same way
        assertEquals(Outcome.HANDED_OVER, FailoverTargets.handover(withheld("partition"),
                pausedOnTimer("COMPLETED", 140_000), pins(), Map.of(), List.of()).outcome());
    }

    @Test
    void aPauseFailsWhenAContinuationWaitedForThePausedWorker() {
        // due 10 s in, applied only when the worker came back at 200 s
        Handover h = FailoverTargets.handover(withheld("pause"), pausedOnTimer("COMPLETED", 201_000), pins(), Map.of(), List.of());
        assertEquals(Outcome.FAILED, h.outcome());
        assertEquals("a continuation due 10 s into it waited 91 s, for the paused worker to come back", h.why());
    }

    @Test
    void aWaitWhileTheOtherWorkersHadFaultsTooIsNeitherAHandoverNorAFailure() {
        Fault pause = withheld("pause");
        Fault killOfTheOther = new Fault("kill", 105_000, Long.MAX_VALUE, "wf-2", "timer", "w1", null, Set.of());
        Handover h = FailoverTargets.handover(pause, pausedOnTimer("COMPLETED", 201_000), pins(), Map.of(),
                List.of(pause, killOfTheOther));
        assertEquals(Outcome.OVERLAPPED, h.outcome(), h.why());
        assertTrue(h.why().endsWith("kill of w1"), h.why());
        // a kill long over by then does not excuse it
        Fault longAgo = new Fault("kill", 10_000, Long.MAX_VALUE, "wf-2", "timer", "w1", null, Set.of());
        assertEquals(Outcome.FAILED, FailoverTargets.handover(pause, pausedOnTimer("COMPLETED", 201_000), pins(),
                Map.of(), List.of(pause, longAgo)).outcome());
    }

    @Test
    void aContinuationDueLateInThePauseMustNotWaitForTheWorkerEither() {
        // due 30 s before the worker came back: 20 s is enough for another to take it
        Map<String, Object> wf = pausedOnTimer("COMPLETED", 201_000);
        ((Map<String, Object>) ((List<Object>) wf.get("tasks")).get(0)).put("endTime", 170_000L);
        assertEquals(Outcome.FAILED, FailoverTargets.handover(withheld("pause"), wf, pins(), Map.of(), List.of()).outcome());
        // one already due when the pause began may have been leased by the worker: 45 s
        Map<String, Object> leased = pausedOnTimer("COMPLETED", 140_000);
        ((Map<String, Object>) ((List<Object>) leased.get("tasks")).get(0)).put("endTime", 95_000L);
        Handover h = FailoverTargets.handover(withheld("pause"), leased, pins(), Map.of(), List.of());
        assertEquals(Outcome.HANDED_OVER, h.outcome(), h.why());
        assertEquals(40_000L, h.delayMs());
    }

    @Test
    void theMarkerOfARetriedAttemptIsNotAContinuationThatWaited() {
        // the first attempt of a task timed out on the paused worker: its marker never applies, and
        // the retry's applied 30 s after it was due, well before the worker came back
        Map<String, Object> wf = pausedOnTimer("COMPLETED", 140_000);
        Map<String, Object> timedOut = task("slow_step", "slow_step", "TIMED_OUT", Map.of());
        timedOut.put("endTime", 50_000L);
        timedOut.put("continuation", Map.of("endTime", 0));
        ((List<Map<String, Object>>) wf.get("tasks")).add(timedOut);
        assertEquals(Outcome.HANDED_OVER, FailoverTargets.handover(withheld("pause"), wf, pins(), Map.of(), List.of()).outcome());
    }

    @Test
    void aPauseWithNothingDueHasNothingToHandOver() {
        // the timer fired 10 s before the worker came back: within the time a takeover may take
        Map<String, Object> wf = pausedOnTimer("COMPLETED", 205_000);
        ((Map<String, Object>) ((List<Object>) wf.get("tasks")).get(0)).put("endTime", 190_000L);
        assertEquals(Outcome.NOTHING_DUE, FailoverTargets.handover(withheld("pause"), wf, pins(), Map.of(), List.of()).outcome());
        // applied before the pause began: not the pause's to hand over either
        assertEquals(Outcome.NOTHING_DUE, FailoverTargets.handover(
                new Fault("pause", 115_000, 215_000, "wf", "timer", "w0", null, Set.of()),
                pausedOnTimer("COMPLETED", 112_000), pins(), Map.of(), List.of()).outcome());
    }

    @Test
    void aGcPauseIsRiddenOutWhenTheExecutionCompletes() {
        Fault gc = new Fault("gc-pause", KILL, KILL + 8_000, "wf", "random", "w0", null, Set.of());
        assertEquals(Outcome.RODE_OUT, FailoverTargets.handover(gc, pausedOnTimer("COMPLETED", 112_000), pins(), Map.of(), List.of()).outcome());
        assertEquals(Outcome.FAILED, FailoverTargets.handover(gc, pausedOnTimer("FAILED", 112_000), pins(), Map.of(), List.of()).outcome());
    }

    @Test
    void theChildrenAreTheSubWorkflowsAndTheWorkflowsStartedByName() {
        Map<String, Object> sub = task("SUB_WORKFLOW", "sub_flow_1", "COMPLETED", Map.of());
        sub.put("subWorkflowId", "child-1");
        Map<String, Object> started = task("START_WORKFLOW", "start_sub_flow_1", "COMPLETED", Map.of());
        started.put("outputData", Map.of("workflowId", "child-2"));
        assertEquals(List.of("child-1", "child-2"),
                FailoverTargets.children(wf("COMPLETED", null, List.of(sub, started), List.of())));
    }
}
