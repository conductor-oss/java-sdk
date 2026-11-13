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
package operator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowFailedException;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import operator.ShipmentWorkflow.Order;
import operator.ShipmentWorkflow.Shipment;

/**
 * Operator actions on native executions, against a running server:
 *
 * <ol>
 *   <li>retry: the execution fails because the label task's failure is uncaught; after the carrier
 *       recovers, retry reruns from the label task, without packing again;
 *   <li>rerun from a task: rerun the completed execution from the pack task;
 *   <li>restart: restart the completed execution from the beginning;
 *   <li>every rerun of a decision made in code ({@link RouteWorkflow});
 *   <li>reruns inside a fork ({@link ForkedWorkflow});
 *   <li>reruns inside the loops the server runs ({@link LoopedWorkflow});
 *   <li>a join of a lane that ends in a decision waits for the chosen case ({@link SwitchLaneWorkflow}).
 * </ol>
 */
public class OperatorChecks {

    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.host()
                .workflows(ShipmentWorkflow.class, LaneFailureWorkflow.class, ShipmentFailureWorkflow.class, PingWorkflow.class, RouteWorkflow.class, ForkedWorkflow.class, LoopedWorkflow.class, SwitchLaneWorkflow.class, FleetWorkflow.class)
                .tasks(new ShipmentTasks(), new FleetTasks())
                .start();

        // an uncaught failure in an async lane, joined without get(), fails the workflow
        try {
            conductor.start(LaneFailureWorkflow::run, "trk-lane").result(Duration.ofMinutes(2));
            check("an uncaught failure in a joined lane fails the workflow", false);
        } catch (WorkflowFailedException e) {
            check("an uncaught failure in a joined lane fails the workflow", e.getMessage().contains("carrier unavailable"));
        }
        ShipmentTasks.packCalls.set(0);

        WorkflowHandle<Shipment> run = conductor.start(ShipmentWorkflow::ship, new Order("o-1", 25));
        System.out.println("started " + run.workflowId());
        try {
            run.result(Duration.ofMinutes(2));
            check("the execution fails while the carrier is down", false);
        } catch (WorkflowFailedException e) {
            System.out.println("failed as expected: " + e.getMessage());
            check("the execution fails while the carrier is down", e.getMessage().contains("carrier unavailable"));
        }
        // the failure workflow named in @Workflow(failureWorkflow) ran for the failed execution
        // the server saves the execution FAILED, then starts its failure workflow and records it
        Object failureId = null;
        for (int i = 0; i < 50 && failureId == null; i++) {
            failureId = output(conductor, run.workflowId()).get("conductor.failure_workflow");
            if (failureId == null) {
                Thread.sleep(100);
            }
        }
        check("the failed execution started its failure workflow", failureId != null);
        check("the failure workflow received the failed execution", waitFor(() -> ShipmentTasks.failedShipments.contains(run.workflowId()), 60));

        int packsBefore = ShipmentTasks.packCalls.get();
        check("a transient task failure is retried, not raised", attempts(conductor, run.workflowId(), "shipment_pack") == 2);

        // 1. retry
        ShipmentTasks.carrierDown.set(false);
        post(conductor, "/workflow/" + run.workflowId() + "/retry", null);
        Shipment retried = run.result(Duration.ofMinutes(2));
        System.out.println("after retry: " + retried);
        check("retry completes the execution", retried.label() != null && retried.label().startsWith("LABEL-"));
        check("retry did not pack again", ShipmentTasks.packCalls.get() == packsBefore);
        String tracking = retried.trackingId();

        // 2. rerun from the pack task
        String packTaskId = taskId(conductor, run.workflowId(), "shipment_pack");
        post(conductor, "/workflow/" + run.workflowId() + "/rerun",
                Map.of("reRunFromWorkflowId", run.workflowId(), "reRunFromTaskId", packTaskId));
        Shipment rerun = run.result(Duration.ofMinutes(2));
        System.out.println("after rerun: " + rerun);
        check("rerun from a task completes", rerun.label() != null);
        check("rerun packed again, retrying the dropped request", ShipmentTasks.packCalls.get() == packsBefore + 2);
        check("rerun kept the value recorded before the task", tracking.equals(rerun.trackingId()));

        // 3. restart
        post(conductor, "/workflow/" + run.workflowId() + "/restart", null);
        Shipment restarted = run.result(Duration.ofMinutes(2));
        System.out.println("after restart: " + restarted);
        check("restart completes", restarted.label() != null);
        check("restart recorded a new tracking id", !tracking.equals(restarted.trackingId()));
        check("stage variable is shipped", "shipped".equals(run.variables().get("stage")));

        checkReruns(conductor);
        checkForkReruns(conductor);
        checkLoopReruns(conductor);
        checkSwitchLane(conductor);
        checkFleet(conductor);

        // @Schedule registered the workflow with the scheduler, which runs it every 5 seconds
        check("the @Schedule workflow ran on the scheduler", waitFor(() -> ShipmentTasks.pings.get() >= 2, 60));
        post(conductor, "/scheduler/schedules/native_ping_every_5s/pause", null, ConductorClientRequest.Method.PUT);

        conductor.close();
        client.shutdown();
        System.out.println();
        System.out.println(failures.isEmpty() ? "OPERATOR CHECKS PASSED" : "OPERATOR CHECKS FAILED: " + failures);
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private static final String FIRST = "x/ran-x/after-ran-x";
    private static final String DECIDED_AGAIN = "y/ran-y/after-ran-y";

    /** Every way to run a decision in code again: the code decides, or the decision stands. */
    private static void checkReruns(Conductor conductor) throws InterruptedException {
        WorkflowHandle<String> whole = routed(conductor);
        rerun(conductor, whole, null);
        check("rerun with no task runs the code again", DECIDED_AGAIN.equals(outcome(whole)));

        WorkflowHandle<String> before = routed(conductor);
        rerun(conductor, before, taskId(conductor, before.workflowId(), "route_decide"));
        check("rerun from before a switch decides again", DECIDED_AGAIN.equals(outcome(before)));
        check("the rerun switch still draws the other case", drawsCase(conductor, before, "x"));

        WorkflowHandle<String> inside = routed(conductor);
        rerun(conductor, inside, taskId(conductor, inside.workflowId(), "route_x"));
        check("rerun from inside a case keeps the decision", FIRST.equals(outcome(inside)));
        check("rerun from inside a case still draws the other case", drawsCase(conductor, inside, "y"));

        WorkflowHandle<String> after = routed(conductor);
        rerun(conductor, after, taskId(conductor, after.workflowId(), "route_after"));
        check("rerun from after a switch keeps the decision", FIRST.equals(outcome(after)));
        check("rerun from after a switch still draws the other case", drawsCase(conductor, after, "y"));

        WorkflowHandle<String> decision = routed(conductor);
        boolean refused = false;
        try {
            rerun(conductor, decision, taskId(conductor, decision.workflowId(), "SWITCH"));
        } catch (ConductorClientException e) {
            refused = e.getStatus() == 400 && e.getMessage().contains("belongs to the code");
        }
        check("rerun from the switch itself is refused", refused);
        check("a refused rerun leaves the execution as it was", FIRST.equals(outcome(decision)) && attempts(conductor, decision.workflowId(), "route_x") == 1);

        WorkflowHandle<String> restarted = routed(conductor);
        post(conductor, "/workflow/" + restarted.workflowId() + "/restart", null);
        check("restart decides again", DECIDED_AGAIN.equals(outcome(restarted)));
    }

    /** Reruns inside a fork: a branch runs again with what followed it; the other branch does not. */
    private static void checkForkReruns(Conductor conductor) throws InterruptedException {
        String[][] cases = {
            {"fork_left", "rerun from a branch task", "2", "1", "2"},
            {"fork_right", "rerun from the other branch's task", "1", "2", "2"},
            {"fork_after", "rerun from the task after the join", "1", "1", "2"},
        };
        for (String[] c : cases) {
            String id = java.util.UUID.randomUUID().toString();
            WorkflowHandle<String> run = conductor.start(ForkedWorkflow::run, id);
            check("the forked run completes", "L+R joined".equals(outcome(run)));
            rerun(conductor, run, taskId(conductor, run.workflowId(), c[0]));
            check(c[1] + " completes", "L+R joined".equals(outcome(run)));
            check(c[1] + " runs left " + c[2] + ", right " + c[3] + ", after " + c[4] + " times in all",
                    runs("left", id) == Integer.parseInt(c[2])
                            && runs("right", id) == Integer.parseInt(c[3])
                            && runs("after", id) == Integer.parseInt(c[4]));
        }
        for (String structure : new String[] {"FORK", "JOIN"}) {
            WorkflowHandle<String> run = conductor.start(ForkedWorkflow::run, java.util.UUID.randomUUID().toString());
            outcome(run);
            boolean refused = false;
            try {
                rerun(conductor, run, taskId(conductor, run.workflowId(), structure));
            } catch (ConductorClientException e) {
                refused = e.getStatus() == 400 && e.getMessage().contains("belongs to the code");
            }
            check("rerun from the " + structure + " task is refused", refused);
            check("a refused rerun leaves the forked run completed", "L+R joined".equals(outcome(run)));
        }
    }

    /** A join of a lane that ends in a decision waits for the task of the chosen case. */
    @SuppressWarnings("unchecked")
    private static void checkSwitchLane(Conductor conductor) {
        WorkflowHandle<String> run = conductor.start(SwitchLaneWorkflow::run, java.util.UUID.randomUUID().toString());
        check("the lane with a decision completes with the chosen case's result", "slow+R".equals(outcome(run)));
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + run.workflowId())
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) wf.get("tasks");
        long slowEnd = endTime(tasks, "lane_slow");
        long joinEnd = endTime(tasks, "JOIN");
        check("the join ended after the chosen case's task", slowEnd > 0 && joinEnd >= slowEnd);
        Map<String, Object> def = (Map<String, Object>) wf.get("workflowDefinition");
        check("the decision in the lane shows its other case", nestedCaseDrawn((List<Map<String, Object>>) def.get("tasks"), "fast"));
    }

    /** Whether a SWITCH at any depth has a non-empty case under the key. */
    @SuppressWarnings("unchecked")
    private static boolean nestedCaseDrawn(List<Map<String, Object>> tasks, String caseKey) {
        for (Map<String, Object> t : tasks) {
            if ("SWITCH".equals(t.get("type"))) {
                List<?> drawn = ((Map<String, List<?>>) t.get("decisionCases")).get(caseKey);
                if (drawn != null && !drawn.isEmpty()) {
                    return true;
                }
            }
            for (Object branch : (List<?>) t.getOrDefault("forkTasks", List.of())) {
                if (nestedCaseDrawn((List<Map<String, Object>>) branch, caseKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long endTime(List<Map<String, Object>> tasks, String taskType) {
        return tasks.stream()
                .filter(t -> taskType.equals(t.get("taskType")))
                .mapToLong(t -> ((Number) t.get("endTime")).longValue())
                .max()
                .orElse(0);
    }

    /** Reruns inside the loops the server runs: the loop goes on from the rerun iteration. */
    private static void checkLoopReruns(Conductor conductor) throws InterruptedException {
        String expected = "3 labels/true/after";
        // forEach: rerun from the second item; the second and third items run again, the first does not
        String id = java.util.UUID.randomUUID().toString();
        WorkflowHandle<String> items = conductor.start(LoopedWorkflow::run, id);
        check("the looped run completes", expected.equals(outcome(items)));
        rerun(conductor, items, taskIdAt(conductor, items.workflowId(), "loop_item", 2));
        check("rerun from a forEach item completes", expected.equals(outcome(items)));
        check("rerun from the second item runs the second and third items again, not the first",
                loopRuns("item/" + id + "-p") == 1 && loopRuns("item/" + id + "-q") == 2 && loopRuns("item/" + id + "-r") == 2);
        check("rerun from a forEach item runs the polling and the last task again",
                loopRuns("poll/" + id) == 6 && loopRuns("after/" + id) == 2);

        // repeatUntil: rerun from the first check; the loop polls until done again
        id = java.util.UUID.randomUUID().toString();
        WorkflowHandle<String> polled = conductor.start(LoopedWorkflow::run, id);
        check("the looped run completes", expected.equals(outcome(polled)));
        rerun(conductor, polled, taskIdAt(conductor, polled.workflowId(), "loop_poll", 1));
        check("rerun from a repeatUntil check completes", expected.equals(outcome(polled)));
        check("rerun from the first check polls until the next done check", loopRuns("poll/" + id) == 6);
        check("rerun from a repeatUntil check leaves the items alone", loopRuns("item/" + id + "-p") == 1);

        // the last task, after both loops
        id = java.util.UUID.randomUUID().toString();
        WorkflowHandle<String> last = conductor.start(LoopedWorkflow::run, id);
        outcome(last);
        rerun(conductor, last, taskId(conductor, last.workflowId(), "loop_after"));
        check("rerun from the task after the loops completes", expected.equals(outcome(last)));
        // the result can be read before the rerun reopens the execution: wait for the task to run again
        String rerunId = id;
        check("rerun from the task after the loops runs only it again",
                waitFor(() -> loopRuns("after/" + rerunId) == 2, 30) && loopRuns("poll/" + id) == 3);
    }

    /**
     * The fleet: loops nested in a loop, switches and if / else if / else inside them, a while loop
     * and a do-while. A rerun from a host's probe runs that host and everything after it again; a
     * patch failing deep in the nested loop fails the run at that task, and a retry continues from it;
     * a restart runs it all again.
     */
    private static void checkFleet(Conductor conductor) throws InterruptedException {
        String id = java.util.UUID.randomUUID().toString();
        WorkflowHandle<String> run = conductor.start(FleetWorkflow::run, id);
        check("the fleet run completes", FleetWorkflow.EXPECTED.equals(outcome(run)));
        check("the fleet run took every path it should",
                FleetTasks.runs(id, "approve", "us") == 1 && FleetTasks.runs(id, "announce", "eu") == 1
                        && FleetTasks.runs(id, "drain", "db-1") == 1 && FleetTasks.runs(id, "verify", "db-1") == 2
                        && FleetTasks.runs(id, "page", "cache-2") == 1 && FleetTasks.runs(id, "warm", "cache-1") == 1
                        && FleetTasks.runs(id, "backup", "db-1") == 1 && FleetTasks.runs(id, "rollback", "eu") == 1
                        && FleetTasks.runs(id, "report", "fleet") == 2);

        // rerun from db-1's probe: db-1 and every host after it run again; web-1 and the gate do not
        Map<String, Object> probe = taskOf(conductor, run.workflowId(), "fleet_probe", "db-1");
        rerun(conductor, run, (String) probe.get("taskId"));
        check("rerun from a task in the nested loop completes", FleetWorkflow.EXPECTED.equals(outcome(run)));
        check("rerun from db-1's probe runs db-1 and the hosts after it again, not web-1 or the gate",
                FleetTasks.runs(id, "probe", "web-1") == 1 && FleetTasks.runs(id, "approve", "us") == 1
                        && FleetTasks.runs(id, "probe", "db-1") == 2 && FleetTasks.runs(id, "verify", "db-1") == 4
                        && FleetTasks.runs(id, "probe", "web-3") == 2 && FleetTasks.runs(id, "report", "fleet") == 4);

        // a patch failing in the second region fails the run at that task; retry continues from it
        String broken = java.util.UUID.randomUUID().toString();
        FleetTasks.broken.put(broken + "/web-2", true);
        WorkflowHandle<String> failing = conductor.start(FleetWorkflow::run, broken);
        String failure = outcome(failing);
        check("a patch failing in the nested loop fails the run", failure.contains("patch repository unreachable from web-2"));
        Map<String, Object> failedPatch = taskOf(conductor, failing.workflowId(), "fleet_patch", "web-2");
        check("the failed run names the patch of web-2 as its failed task",
                failedPatch.get("taskId").equals(workflow(conductor, failing.workflowId()).get("failedTaskId")));
        FleetTasks.broken.remove(broken + "/web-2");
        post(conductor, "/workflow/" + failing.workflowId() + "/retry", null);
        check("retry after the failure completes the run", FleetWorkflow.EXPECTED.equals(outcome(failing)));
        check("retry runs the failed patch again and nothing before it",
                FleetTasks.runs(broken, "patch", "web-2") == 2 && FleetTasks.runs(broken, "patch", "web-1") == 1
                        && FleetTasks.runs(broken, "announce", "eu") == 1 && FleetTasks.runs(broken, "inventory", "fleet") == 1);

        // restart runs the whole fleet again
        post(conductor, "/workflow/" + failing.workflowId() + "/restart", null);
        check("restart completes the fleet run", FleetWorkflow.EXPECTED.equals(outcome(failing)));
        check("restart ran everything again",
                FleetTasks.runs(broken, "inventory", "fleet") == 2 && FleetTasks.runs(broken, "patch", "web-1") == 2
                        && FleetTasks.runs(broken, "patch", "web-2") == 3);
    }

    /** The latest run of a task given this host or region. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> taskOf(Conductor conductor, String workflowId, String taskType, String on) {
        return ((List<Map<String, Object>>) workflow(conductor, workflowId).get("tasks")).stream()
                .filter(t -> taskType.equals(t.get("taskType")))
                .filter(t -> on.equals(((Map<String, Object>) t.get("inputData")).get("host"))
                        || on.equals(((Map<String, Object>) t.get("inputData")).get("region")))
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    private static Map<String, Object> workflow(Conductor conductor, String workflowId) {
        return conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
    }

    private static int loopRuns(String key) {
        java.util.concurrent.atomic.AtomicInteger n = ShipmentTasks.loopRuns.get(key);
        return n == null ? 0 : n.get();
    }

    @SuppressWarnings("unchecked")
    private static String taskIdAt(Conductor conductor, String workflowId, String taskType, int iteration) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return ((List<Map<String, Object>>) wf.get("tasks")).stream()
                .filter(t -> taskType.equals(t.get("taskType")) && ((Number) t.get("iteration")).intValue() == iteration)
                .map(t -> (String) t.get("taskId"))
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    private static int runs(String task, String id) {
        java.util.concurrent.atomic.AtomicInteger n = ShipmentTasks.forkRuns.get(task + "/" + id);
        return n == null ? 0 : n.get();
    }

    private static WorkflowHandle<String> routed(Conductor conductor) {
        WorkflowHandle<String> run = conductor.start(RouteWorkflow::route, java.util.UUID.randomUUID().toString());
        check("the first run takes case x", FIRST.equals(outcome(run)));
        return run;
    }

    private static void rerun(Conductor conductor, WorkflowHandle<String> run, String taskId) throws InterruptedException {
        Map<String, Object> request = new java.util.HashMap<>(Map.of("reRunFromWorkflowId", run.workflowId()));
        if (taskId != null) {
            request.put("reRunFromTaskId", taskId);
        }
        post(conductor, "/workflow/" + run.workflowId() + "/rerun", request);
    }

    /** The result, or why there is none: a rerun that gets stuck must fail the check, not hang it. */
    private static String outcome(WorkflowHandle<String> run) {
        try {
            return run.result(Duration.ofSeconds(30));
        } catch (RuntimeException e) {
            return "no result: " + e;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean drawsCase(Conductor conductor, WorkflowHandle<String> run, String caseKey) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + run.workflowId())
                        .addQueryParam("includeTasks", false).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        Map<String, Object> def = (Map<String, Object>) wf.get("workflowDefinition");
        return ((List<Map<String, Object>>) def.get("tasks")).stream()
                .filter(t -> "SWITCH".equals(t.get("type")))
                .anyMatch(t -> !((Map<String, List<?>>) t.get("decisionCases")).get(caseKey).isEmpty());
    }

    private static void post(Conductor conductor, String path, Object body) throws InterruptedException {
        post(conductor, path, body, ConductorClientRequest.Method.POST);
    }

    private static void post(Conductor conductor, String path, Object body, ConductorClientRequest.Method method)
            throws InterruptedException {
        ConductorClientRequest.Builder b = ConductorClientRequest.builder().method(method).path(path);
        if (body != null) {
            b.body(body);
        }
        conductor.client().execute(b.build());
        Thread.sleep(500);
    }

    @SuppressWarnings("unchecked")
    private static String taskId(Conductor conductor, String workflowId, String taskType) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return ((List<Map<String, Object>>) wf.get("tasks")).stream()
                .filter(t -> taskType.equals(t.get("taskType")))
                .map(t -> (String) t.get("taskId"))
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> output(Conductor conductor, String workflowId) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", false).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        Object out = wf.get("output");
        return out == null ? Map.of() : (Map<String, Object>) out;
    }

    private static boolean waitFor(java.util.function.BooleanSupplier condition, int seconds) throws InterruptedException {
        for (int i = 0; i < seconds * 2; i++) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(500);
        }
        return condition.getAsBoolean();
    }

    @SuppressWarnings("unchecked")
    private static long attempts(Conductor conductor, String workflowId, String taskType) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return ((List<Map<String, Object>>) wf.get("tasks")).stream().filter(t -> taskType.equals(t.get("taskType"))).count();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what);
        if (!ok) {
            failures.add(what);
        }
    }
}
