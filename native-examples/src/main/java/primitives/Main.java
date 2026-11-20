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
package primitives;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;
import org.conductoross.conductor.sdk.testing.DurableReplay;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;
import com.netflix.conductor.client.http.WorkflowClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Hosts every primitive's workflow and tasks, runs each against a server and checks what it
 * returned and what the execution shows. One file per primitive; read them in this order:
 *
 * <pre>
 *   TaskCalls     calling tasks: by stub, by name
 *   SwitchCase    decisions: if, switch (plain Java) and switchOn
 *   Loops         for, while, do-while (plain Java), doWhile, forEach, repeatUntil
 *   Parallel      async, join, fanOut
 *   SubWorkflows  child, startWorkflow
 *   Timers        sleep, sleepUntil, awaitCondition with a timeout, @Schedule
 *   Messages      @Variable, @Signal, @Update, @Query
 *   Errors        retries, TaskFailure, compensation, failing the run
 *   SideEffects   once, now, random, workflowId
 *   SystemTasks   http, event, human, external, llm, mcp, agent
 * </pre>
 *
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" primitives.Main [worker]
 * </pre>
 *
 * SystemTasks needs the MCP server and A2A agent of stubs/ai_stub.py (PRIMITIVES_AI_URL, default
 * http://localhost:8765). {@code worker} hosts the code and tasks only, for runs started elsewhere.
 */
public class Main {

    private static final Duration WAIT = Duration.ofMinutes(3);
    private static int passed;
    private static final List<Map.Entry<Class<?>, WorkflowHandle<?>>> runs = new ArrayList<>();
    private static final List<String> failed = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.workflows(TaskCalls.class, SwitchCase.class, Loops.class, Parallel.class, SubWorkflows.class,
                        SubWorkflows.Child.class, Timers.class, Messages.class, Errors.class, SideEffects.class,
                        SystemTasks.class)
                .tasks(new TaskCalls.Tasks(), new SwitchCase.Tasks(), new Loops.Tasks(), new Parallel.Tasks(),
                        new SubWorkflows.Tasks(), new Timers.Tasks(), new Messages.Tasks(), new Errors.Tasks(),
                        new SideEffects.Tasks())
                .start();
        if (args.length > 0 && args[0].equals("worker")) {
            System.out.println("primitives worker started");
            Thread.currentThread().join();
        }
        String stamp = String.valueOf(System.currentTimeMillis());
        String ai = System.getenv().getOrDefault("PRIMITIVES_AI_URL", "http://localhost:8765");

        // ---------------------------------------------------------------- task calls
        WorkflowHandle<String> tasksRun = started(TaskCalls.class, conductor.start(TaskCalls::run, "eu"));
        check("tasks: stub and by-name calls", "1250+125/10".equals(tasksRun.result(WAIT)), tasksRun);
        Execution e = Execution.of(conductor, tasksRun);
        check("tasks: a second call of a task is its own step", e.has("p_quote_2"), tasksRun);
        check("tasks: a reference name of your choosing", e.has("tax_check"), tasksRun);

        // results as futures: two runs waited on together
        WorkflowHandle<String> first = started(TaskCalls.class, conductor.start(TaskCalls::run, "eu"));
        WorkflowHandle<String> second = started(TaskCalls.class, conductor.start(TaskCalls::run, "eu"));
        CompletableFuture<String> a = first.resultAsync();
        CompletableFuture<String> b = second.resultAsync();
        CompletableFuture.allOf(a, b).orTimeout(WAIT.toSeconds(), TimeUnit.SECONDS).join();
        check("futures: resultAsync completes with each run's result", "1250+125/10".equals(a.join()) && "1250+125/10".equals(b.join()), second);

        // ---------------------------------------------------------------- decisions
        for (SwitchCase.Order order : List.of(
                new SwitchCase.Order("gift-1", true, 1, "eu", SwitchCase.Size.SMALL),
                new SwitchCase.Order("o-2", false, 20, "us", SwitchCase.Size.LARGE),
                new SwitchCase.Order("o-3", false, 5, "apac", SwitchCase.Size.MEDIUM))) {
            WorkflowHandle<List<String>> run = started(SwitchCase.class, conductor.start(SwitchCase::run, order));
            List<String> expected = new ArrayList<>();
            expected.add(order.rush() ? "courier" : "post");
            if (order.qty() > 10) {
                expected.add("pallet");
            }
            expected.add(order.qty() == 1 ? "envelope" : order.qty() < 10 ? "box" : "crate");
            expected.add(switch (order.region()) { case "eu" -> "dhl"; case "us" -> "ups"; default -> "local"; });
            expected.add(switch (order.size()) { case SMALL -> "small-van"; case LARGE -> "truck"; default -> "van"; });
            expected.add(order.region().equals("eu") ? "label-eu" : "label-intl");
            expected.add(switch (order.region()) { case "eu" -> "eu-carrier"; case "us" -> "us-carrier"; default -> "any-carrier"; });
            expected.add(order.id().startsWith("gift") ? "wrapped" : "plain");
            check("switch " + order.id() + ": every decision took its arm", expected.equals(run.result(WAIT)), run);
            Execution x = Execution.of(conductor, run);
            check("switch " + order.id() + ": the if is a SWITCH named after its condition",
                    String.valueOf(order.rush()).equals(x.caseTaken("if_rush")) || (!order.rush() && x.ran("if_rush")), run);
            check("switch " + order.id() + ": switchOn is a SWITCH with the name given",
                    order.region().equals(x.caseTaken("carrier")) || x.ran("carrier"), run);
            check("switch " + order.id() + ": every decision whose arms call a task is drawn",
                    x.count("SWITCH") >= 7, run);
        }

        // ---------------------------------------------------------------- loops
        WorkflowHandle<List<String>> loops = started(Loops.class, conductor.start(Loops::run, "loops-" + stamp));
        check("loops: every loop ran its passes", List.of("for-1", "for-2", "each-a", "each-b", "checked 3", "do-1", "do-2",
                "1x1", "1x2", "2x1", "2x2", "attempt-1", "attempt-2", "attempt-3", "x", "y", "z", "polled 3")
                .equals(loops.result(WAIT)), loops);
        Execution l = Execution.of(conductor, loops);
        check("loops: doWhile tags its body with the loop's name", l.loopsOf("p_step_12").contains("retry_until_ok"), loops);
        check("loops: nested Java loops are both on the inner calls", l.loopsOf("p_step_8").size() == 2, loops);
        check("loops: forEach and repeatUntil run on the server", l.count("DO_WHILE") == 2, loops);

        // ---------------------------------------------------------------- parallel
        WorkflowHandle<List<String>> par = started(Parallel.class, conductor.start(Parallel::run, List.of("a", "b")));
        check("parallel: lanes, a fan-out per item", List.of("left", "right-1+right-2", "lone", "lane-a", "lane-b",
                "fan-a", "fan-b", "fan2-a", "fan2-b").equals(par.result(WAIT)), par);
        Execution p = Execution.of(conductor, par);
        check("parallel: async is a FORK_JOIN", p.count("FORK") >= 3, par);
        check("parallel: fanOut is a dynamic fork", p.has("by_item"), par);

        // ---------------------------------------------------------------- sub workflows
        WorkflowHandle<String> subs = started(SubWorkflows.class, conductor.start(SubWorkflows::run, 3));
        check("subworkflows: a child, two side by side", "6/12/14".equals(subs.result(WAIT)), subs);
        Execution s = Execution.of(conductor, subs);
        check("subworkflows: three SUB_WORKFLOW and two START_WORKFLOW",
                s.count("SUB_WORKFLOW") == 3 && s.count("START_WORKFLOW") == 2, subs);

        // ---------------------------------------------------------------- messages
        WorkflowHandle<String> cart = started(Messages.class, conductor.start(Messages::run, "ana"));
        int one = cart.update(Messages::add, "apple");
        int two = cart.update(Messages::add, "pear");
        check("messages: an update returns its handler's value", one == 1 && two == 2, cart);
        check("messages: a query reads the state", cart.query(Messages::count) == 2, cart);
        cart.signal(Messages::checkout);
        check("messages: the signal woke the code", "ana: order of 2".equals(cart.result(WAIT)), cart);
        check("messages: variables are on the execution", Boolean.TRUE.equals(new WorkflowClient(client).getWorkflow(cart.workflowId(), false).getVariables().get("checkedOut")), cart);

        // ---------------------------------------------------------------- errors
        WorkflowHandle<List<String>> paid = started(Errors.class, conductor.start(Errors::run, new Errors.Purchase("ok-" + stamp, 50, false)));
        check("errors: retries absorb a flaky task", List.of("ok after 3", "reserved seat", "reserved meal", "paid 50")
                .equals(paid.result(WAIT)), paid);
        check("errors: the flaky task ran three times", Execution.of(conductor, paid).attempts("p_flaky") == 3, paid);
        WorkflowHandle<List<String>> declined =
                started(Errors.class, conductor.start(Errors::run, new Errors.Purchase("no-" + stamp, 500, false)));
        check("errors: a declined payment is compensated, last first", List.of("ok after 3", "reserved seat", "reserved meal",
                "p_pay FAILED_WITH_TERMINAL_ERROR", "released meal", "released seat").equals(declined.result(WAIT)), declined);
        WorkflowHandle<List<String>> fails =
                started(Errors.class, conductor.start(Errors::run, new Errors.Purchase("fail-" + stamp, 500, true)));
        String reason = failure(fails);
        check("errors: WorkflowFailedException fails the run with its reason",
                reason != null && reason.contains("declined: ok after 3"), fails);
        Throwable failedAsync = fails.resultAsync().handle((result, error) -> error).join();
        check("futures: resultAsync of a failed run completes with WorkflowFailedException",
                failedAsync instanceof org.conductoross.conductor.sdk.WorkflowFailedException, fails);

        // ---------------------------------------------------------------- side effects
        WorkflowHandle<SideEffects.Seen> seen = started(SideEffects.class, conductor.start(SideEffects::run, "ord"));
        SideEffects.Seen v = seen.result(WAIT);
        check("side effects: once is recorded, not run again", v.id().startsWith("ord-") && v.id().equals(v.id2()), seen);
        check("side effects: random is in [0, 1)", v.roll() >= 0 && v.roll() < 1 && v.roll2() >= 0 && v.roll2() < 1, seen);
        check("side effects: now does not go back", !v.at2().isBefore(v.at()), seen);
        check("side effects: workflowId is this run's", seen.workflowId().equals(v.workflowId()), seen);

        // ---------------------------------------------------------------- system tasks
        WorkflowHandle<List<String>> sys = started(SystemTasks.class, conductor.start(SystemTasks::run, new SystemTasks.Ai(ai)));
        sys.completeTask("review", new SystemTasks.Answer("ops-bot", true), WAIT);
        sys.completeTask("delivered", new SystemTasks.Delivery("at the door"), WAIT);
        check("system tasks: each answered", List.of("http: kitchensink-agent", "human: ops-bot", "external: at the door",
                "llm: unavailable", "mcp tools: 2", "mcp call: Sunny and 18C in Paris",
                "agent: Agent reply: Check order 7 has been reviewed.").equals(sys.result(WAIT)), sys);
        check("system tasks: the event was published", Execution.of(conductor, sys).has("announce"), sys);

        // ---------------------------------------------------------------- timers, last: the longest
        WorkflowHandle<String> timers = started(Timers.class, conductor.start(Timers::run, new Timers.Ticks(2)));
        check("timers: slept, then gave up waiting", "waited and timed out".equals(timers.result(WAIT)), timers);
        check("timers: one WAIT per sleep, one for sleepUntil, one for awaitCondition",
                Execution.of(conductor, timers).count("WAIT") == 4, timers);
        check("timers: @Schedule registered the schedule, paused", Boolean.TRUE.equals(schedule(conductor, "p_timers_weekdays").get("paused")), null);

        replayAll(conductor);

        conductor.close();
        client.shutdown();
        System.out.println();
        System.out.println("PRIMITIVES pass=" + passed + " fail=" + failed.size() + (failed.isEmpty() ? "" : " " + failed));
        System.exit(failed.isEmpty() ? 0 : 1);
    }

    private static <R> WorkflowHandle<R> started(Class<?> workflow, WorkflowHandle<R> run) {
        System.out.println("started " + run.workflowId());
        runs.add(Map.entry(workflow, run));
        return run;
    }

    /** Runs the code again against each recorded execution, as a worker does, and fails on any difference. */
    private static void replayAll(Conductor conductor) throws Exception {
        Path dir = Files.createTempDirectory("primitives");
        for (Map.Entry<Class<?>, WorkflowHandle<?>> r : runs) {
            Map<String, Object> wf = conductor.client().execute(
                    ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET)
                            .path("/workflow/" + r.getValue().workflowId()).addQueryParam("includeTasks", true).build(),
                    new TypeReference<Map<String, Object>>() {}).getData();
            Path file = dir.resolve(r.getValue().workflowId() + ".json");
            Files.writeString(file, new ObjectMapper().writeValueAsString(wf));
            String why = null;
            try {
                DurableReplay.assertCompatible(r.getKey(), file);
            } catch (Throwable e) {
                why = e.getMessage();
            }
            check("replay: the code matches what " + r.getKey().getSimpleName() + " recorded" + (why == null ? "" : ": " + why),
                    why == null, r.getValue());
        }
    }

    /** The reason a run failed with, or null when it did not fail. */
    private static String failure(WorkflowHandle<?> run) {
        try {
            run.result(WAIT);
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    private static Map<String, Object> schedule(Conductor conductor, String name) {
        return conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/scheduler/schedules/" + name).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
    }

    private static void check(String what, boolean ok, WorkflowHandle<?> run) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what + (run == null ? "" : "  " + run.workflowId()));
        if (ok) {
            passed++;
        } else {
            failed.add(what);
        }
    }

    /** What the server recorded for a run: its tasks by reference name. */
    private record Execution(List<Map<String, Object>> tasks) {

        @SuppressWarnings("unchecked")
        static Execution of(Conductor conductor, WorkflowHandle<?> run) {
            Map<String, Object> wf = conductor.client().execute(
                    ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + run.workflowId())
                            .addQueryParam("includeTasks", true).build(),
                    new TypeReference<Map<String, Object>>() {}).getData();
            return new Execution((List<Map<String, Object>>) wf.get("tasks"));
        }

        void each(String ref, Consumer<Map<String, Object>> f) {
            tasks.stream().filter(t -> ref.equals(t.get("referenceTaskName"))).forEach(f);
        }

        boolean has(String ref) {
            return tasks.stream().anyMatch(t -> ref.equals(t.get("referenceTaskName")));
        }

        boolean ran(String ref) {
            return tasks.stream().anyMatch(t -> ref.equals(t.get("referenceTaskName")) && "COMPLETED".equals(t.get("status")));
        }

        long attempts(String ref) {
            return tasks.stream().filter(t -> ref.equals(t.get("referenceTaskName"))).count();
        }

        long count(String taskType) {
            return tasks.stream().filter(t -> taskType.equals(t.get("taskType"))).count();
        }

        String caseTaken(String ref) {
            for (Map<String, Object> t : tasks) {
                if (ref.equals(t.get("referenceTaskName")) && t.get("outputData") instanceof Map<?, ?> out
                        && out.get("selectedCase") != null) {
                    return String.valueOf(out.get("selectedCase"));
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        List<String> loopsOf(String ref) {
            for (Map<String, Object> t : tasks) {
                if (ref.equals(t.get("referenceTaskName")) && t.get("workflowTask") instanceof Map<?, ?> entry
                        && entry.get("metadata") instanceof Map<?, ?> metadata && metadata.get("loops") instanceof List<?> loops) {
                    return (List<String>) loops;
                }
            }
            return List.of();
        }
    }
}
