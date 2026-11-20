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
package org.conductoross.conductor.sdk.internal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import static org.conductoross.conductor.sdk.Conductor.fanOut;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.switchOn;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Steps come from the compiled code: nothing runs, so no value can stop the reading. */
class CodeReaderTest {

    /** The task stub the workflows below call: its calls schedule the tasks. */
    static final Tasks TASKS = org.conductoross.conductor.sdk.Conductor.stub(Tasks.class);

    public record Plan(List<String> items, boolean needsApproval, Map<String, String> owner) {}

    public enum Tier {
        GOLD
    }

    static final AtomicInteger PLAIN_JAVA = new AtomicInteger();

    public static class Tasks {
        @WorkerTask("plan")
        public Plan plan(@InputParam("x") String x) {
            return null;
        }

        @WorkerTask("approve")
        public String approve(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("ship")
        public String ship(@InputParam("x") String x) {
            return x;
        }

        @WorkerTask("notify")
        public String tell(@InputParam("x") String x) {
            return x;
        }
    }

    public static class Flows {
        @Workflow(name = "needs_real_values")
        public String needsRealValues(String x) {
            Plan plan = TASKS.plan(x);
            String first = plan.items().get(0);
            int size = Integer.parseInt(plan.owner().get("size").trim()) / plan.items().size();
            TASKS.ship(first + size);
            return TASKS.tell(x);
        }

        @Workflow(name = "negated_if")
        public String negatedIf(String x) {
            Plan plan = TASKS.plan(x);
            if (!plan.needsApproval()) {
                return TASKS.ship(x);
            }
            return TASKS.approve(x);
        }

        @Workflow(name = "branches_and_loops")
        public String branchesAndLoops(String x) {
            Plan plan = TASKS.plan(x);
            if (plan.needsApproval()) {
                TASKS.approve(x);
            } else {
                TASKS.tell(x);
            }
            for (String item : plan.items()) {
                TASKS.ship(item);
            }
            return "done";
        }

        @Workflow(name = "no_plain_java")
        public String noPlainJava(String x) {
            PLAIN_JAVA.incrementAndGet();
            String v = once("v", () -> "id-" + PLAIN_JAVA.incrementAndGet());
            return TASKS.ship(v);
        }

        @Workflow(name = "finally_once")
        public String finallyOnce(String x) {
            try {
                if (x.isEmpty()) {
                    return TASKS.ship(x);
                }
                return TASKS.approve(x);
            } finally {
                TASKS.tell(x);
            }
        }

        @Workflow(name = "helpers")
        public String helpers(String x) {
            List.of("a").forEach(i -> TASKS.ship(i));
            return helper(x);
        }

        private String helper(String x) {
            return TASKS.tell(x);
        }

        @Workflow(name = "recursive")
        public String recursive(String x) {
            TASKS.ship(x);
            return x.isEmpty() ? "done" : recursive(x.substring(1));
        }

        @Workflow(name = "keys")
        public String keys(String x) {
            switchOn("s", x)
                    .when("text", () -> TASKS.ship(x))
                    .when(5, () -> TASKS.approve(x))
                    .when(true, () -> TASKS.tell(x))
                    .when(Tier.GOLD, () -> {
                        sleep(Duration.ofSeconds(1));
                    })
                    .otherwise(() -> TASKS.plan(x))
                    .get();
            return "done";
        }

        @Workflow(name = "fans")
        public String fans(String x) {
            fanOut("spread", List.of("a"), i -> TASKS.ship(i));
            return "done";
        }

        @Workflow(name = "joined_at_once")
        public String joinedAtOnce(String x) {
            java.util.concurrent.Future<String> p = Conductor.async(() -> TASKS.ship(x));
            Conductor.join(p);
            return p.resultNow();
        }

        @Workflow(name = "never_joined")
        public String neverJoined(String x) {
            Conductor.async(() -> TASKS.ship(x));
            return TASKS.tell(x);
        }

        @Workflow(name = "joined_later")
        public String joinedLater(String x) {
            java.util.concurrent.Future<String> p = Conductor.async(() -> TASKS.ship(x));
            TASKS.tell(x);
            Conductor.join(p);
            return p.resultNow();
        }

        @Workflow(name = "registers")
        public String registers(String x) {
            Conductor.http("get_it").get("http://example.test/a").as(String.class);
            Conductor.event("fire").to("kafka:topic").send(x);
            return "done";
        }
    }

    private static List<Map<String, Object>> discover(String workflow) {
        WorkflowSpec spec = WorkflowSpec.of(Flows.class).stream().filter(s -> s.name.equals(workflow)).findFirst().orElseThrow();
        return Replayer.discover(spec);
    }

    private static List<Object> refs(List<Map<String, Object>> entries) {
        return entries.stream().map(e -> e.get("taskReferenceName")).toList();
    }

    @Test
    void codeThatWouldNeedRealValuesIsReadToTheEnd() {
        assertEquals(List.of("plan", "ship", "notify"), refs(discover("needs_real_values")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bothSidesOfAnIfAndTheBodyOfALoopAreSeen() {
        List<Map<String, Object>> entries = discover("branches_and_loops");
        assertEquals(List.of("plan", "if_needsApproval", "ship"), refs(entries));
        Map<String, Object> sw = entries.get(1);
        assertEquals("SWITCH", sw.get("type"), "an if whose arms call the server is drawn with both arms");
        assertEquals(List.of("approve"), refs(((Map<String, List<Map<String, Object>>>) sw.get("decisionCases")).get("true")));
        assertEquals(List.of("notify"), refs((List<Map<String, Object>>) sw.get("defaultCase")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aNegatedIfKeysItsArmsByTheValueItIsNamedAfter() {
        Map<String, Object> sw = discover("negated_if").get(1);
        assertEquals("if_needsApproval", sw.get("taskReferenceName"));
        Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) sw.get("decisionCases");
        assertEquals(List.of("false"), List.copyOf(cases.keySet()), "if (!needsApproval) runs its body when needsApproval is false");
        assertEquals(List.of("ship"), refs(cases.get("false")));
        assertEquals(List.of("approve"), refs((List<Map<String, Object>>) sw.get("defaultCase")));
    }

    @Test
    void noPlainJavaRuns() {
        int before = PLAIN_JAVA.get();
        assertEquals(List.of("ship"), refs(discover("no_plain_java")));
        assertEquals(before, PLAIN_JAVA.get());
    }

    @Test
    void aFinallyBlockCopiedToEveryExitIsOneStep() {
        assertEquals(List.of("ship", "notify", "approve"), refs(discover("finally_once")));
    }

    @Test
    void callsAreFollowedIntoLambdasAndHelpers() {
        assertEquals(List.of("ship", "notify"), refs(discover("helpers")));
    }

    @Test
    void recursionEnds() {
        assertEquals(List.of("ship"), refs(discover("recursive")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void everyKindOfConstantCaseKeyIsRead() {
        Map<String, Object> sw = discover("keys").get(0);
        assertEquals(List.of("text", "5", "true", "GOLD"), List.copyOf(((Map<String, Object>) sw.get("decisionCases")).keySet()));
        assertEquals(List.of("plan"), refs((List<Map<String, Object>>) sw.get("defaultCase")));
    }

    @Test
    void aFanOutIsADynamicForkAndJoin() {
        List<Map<String, Object>> entries = discover("fans");
        assertEquals(List.of("spread", "spread_join"), refs(entries));
        assertEquals("FORK_JOIN_DYNAMIC", entries.get(0).get("type"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadata(Map<String, Object> entry) {
        return (Map<String, Object>) entry.get("metadata");
    }

    @Test
    void aForkTheCodeJoinsHasAWaitingJoinAndOneGetAddsNothing() {
        List<Map<String, Object>> entries = discover("joined_at_once");
        assertEquals(List.of("fork", "fork_join"), refs(entries));
        assertEquals(null, metadata(entries.get(1)).get("detached"));
    }

    @Test
    void aForkNothingJoinsIsMarkedSoItDrawsAsJustAFork() {
        List<Map<String, Object>> entries = discover("never_joined");
        assertEquals(List.of("fork", "fork_join", "notify"), refs(entries));
        assertEquals(true, metadata(entries.get(1)).get("detached"));
    }

    @Test
    void aJoinAfterOtherCallsIsItsOwnStepNamingTheFork() {
        List<Map<String, Object>> entries = discover("joined_later");
        assertEquals(List.of("fork", "fork_join", "notify", "fork_wait"), refs(entries));
        assertEquals(true, metadata(entries.get(1)).get("detached"));
        assertEquals("fork", metadata(entries.get(3)).get("waitForFork"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void constantBuilderArgumentsAreKept() {
        List<Map<String, Object>> entries = discover("registers");
        Map<String, Object> request = (Map<String, Object>) ((Map<String, Object>) entries.get(0).get("inputParameters")).get("http_request");
        assertEquals("http://example.test/a", request.get("uri"));
        assertEquals("kafka:topic", entries.get(1).get("sink"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void stepsRecordTheirSourceLine() {
        Map<String, Object> metadata = (Map<String, Object>) discover("fans").get(0).get("metadata");
        assertEquals(true, metadata.get("site").toString().startsWith("CodeReaderTest$Flows.fans:"), metadata.toString());
    }
}
