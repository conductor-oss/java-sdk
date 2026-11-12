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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.McpCall;
import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.Query;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowFailedException;

import com.fasterxml.jackson.databind.JsonNode;
import kitchensink.KitchenSinkModel.Approval;
import kitchensink.KitchenSinkModel.Callback;
import kitchensink.KitchenSinkModel.CancelRequest;
import kitchensink.KitchenSinkModel.FanoutPlan;
import kitchensink.KitchenSinkModel.JobStatus;
import kitchensink.KitchenSinkModel.KitchenSinkInput;
import kitchensink.KitchenSinkModel.KitchenSinkResult;
import kitchensink.KitchenSinkModel.OddEven;
import kitchensink.KitchenSinkModel.Receipt;
import kitchensink.KitchenSinkModel.Stats;
import kitchensink.KitchenSinkModel.SubFlowInput;
import kitchensink.KitchenSinkModel.SubFlowResult;
import kitchensink.KitchenSinkModel.Summary;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.awaitCondition;
import static org.conductoross.conductor.sdk.Conductor.awaitExternal;
import static org.conductoross.conductor.sdk.Conductor.child;
import static org.conductoross.conductor.sdk.Conductor.event;
import static org.conductoross.conductor.sdk.Conductor.fanOut;
import static org.conductoross.conductor.sdk.Conductor.http;
import static org.conductoross.conductor.sdk.Conductor.human;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.llm;
import static org.conductoross.conductor.sdk.Conductor.mcp;
import static org.conductoross.conductor.sdk.Conductor.now;
import static org.conductoross.conductor.sdk.Conductor.once;
import static org.conductoross.conductor.sdk.Conductor.sleep;
import static org.conductoross.conductor.sdk.Conductor.sleepUntil;
import static org.conductoross.conductor.sdk.Conductor.startWorkflow;
import static org.conductoross.conductor.sdk.Conductor.task;

/**
 * The kitchensink workflow, written as code. A plain class: the @Workflow method is the workflow,
 * and any method on any class can be one. No base class, no interface.
 *
 * Section 1 mirrors rest/src/main/resources/kitchensink/kitchensink.json task for task, with the same
 * task names and reference names, so the two diagrams can be compared side by side. The decision is
 * a plain Java switch, so it is named after what it switches on (switch_oddEven, oddEvenDecision in
 * the JSON).
 * Section 2 adds what the JSON version has no equivalent for: timers and a polling loop in plain
 * loops, signals, updates, queries, human tasks, external callbacks, compensation and an LLM call.
 * Section 3 covers iteration, timers until an instant, MCP and A2A agents.
 * Section 4 composes the constructs inside each other: nested ifs and switches, ifs and forks in
 * loops, loops and sub workflows in forks, lanes started inside an if.
 *
 * The one sentence: every static call imported from Conductor goes to the server and shows in the
 * graph; everything else is just Java.
 *
 *   tasks.x(...)                     SIMPLE task, run by whichever worker polls that name
 *   task("name")...                  the same, when the name is only known at runtime
 *   if / switch                      a SWITCH when its arms call the server: every arm shows, and the
 *                                    one taken; named after what it decides on (if_odd, switch_lane)
 *   for / while / do-while           plain loops; the diagram draws a loop around the calls inside
 *   async, fanOut, child, sleep,     server-side tasks: FORK_JOIN, FORK_JOIN_DYNAMIC, SUB_WORKFLOW, WAIT,
 *   http, human, ...                 HTTP, HUMAN, EVENT, START_WORKFLOW, LLM_*
 *   once(name, () -> ...)            Java executed here, once, with the result recorded on the execution
 *   everything else                  plain Java, invisible to the server
 *
 * Conductor's own switchOn, forEach and repeatUntil are in the switches and operator examples.
 *
 * Three rules:
 *   1. anything non-deterministic or with a side effect that is not a task goes through once(), now()
 *      or random()
 *   2. calls are matched by task name on a re-run, so logging, reordering independent calls and
 *      adding calls in code that running executions have not reached yet are all safe
 *   3. blocking I/O belongs in tasks, or in once() when it is small
 *
 * Failures: a task whose retries are exhausted throws TaskFailure at the call. Catch it to
 * compensate or continue; let it propagate and the workflow fails with that reason. The task
 * itself shows FAILED in the UI either way.
 */
public class KitchenSinkWorkflow {

    // The tasks, taken as a constructor parameter. Conductor creates the workflow for each re-run
    // and passes a stand-in whose calls schedule the tasks: tasks.charge(...) runs once, on a
    // worker, however often the code re-runs, and a re-run gets the recorded result back. A
    // KitchenSinkTasks created here instead would run its methods in the workflow on every re-run,
    // so Conductor rejects it.
    private final KitchenSinkTasks tasks;

    public KitchenSinkWorkflow(KitchenSinkTasks tasks) {
        this.tasks = tasks;
    }

    // Workflow variables: durable, shown in the UI and the API as workflow.variables, restored on
    // a re-run. Every other field is an ordinary field (injected clients, caches) and is not persisted.
    @Variable private String stage = "new";
    @Variable private boolean cancelled;
    @Variable private List<String> notes = new ArrayList<>();

    // Handlers run between pauses of the workflow method, never concurrently with it.
    @Signal("cancel")
    public void cancel(CancelRequest request) {
        cancelled = true;
        notes.add("cancelled: " + request.reason());
    }

    @Update("addNote")                       // a signal whose return value goes back to the caller
    public int addNote(String note) {
        notes.add(note);
        return notes.size();
    }

    @Query("summary")                        // computed on the worker; variables need no query, read them directly
    public String summary() {
        return stage + " (" + notes.size() + " notes)";
    }

    @Workflow(name = "kitchensink", version = 1, ownerEmail = "example@email.com", timeout = "P3D")
    public KitchenSinkResult kitchenSink(KitchenSinkInput input) {
        Deque<Runnable> compensations = new ArrayDeque<>();

        // once(): execute this Java here, once, and record the result. Ids, config reads, small
        // calls that do not deserve a task. Recorded on the execution; it is not a step in the graph.
        String runId = once("runId", () -> "ks-" + UUID.randomUUID());

        // ---------------------------------------------------------------- section 1: the JSON kitchensink
        // The .ref("...") and "fanout1"-style names below exist only to match the JSON definition.
        // In ordinary code they are omitted and derived from the task name or the call site.
        stage = "classic";

        // task_1 (SIMPLE): a plain call, typed by the worker method; no interface to write
        Stats stats = tasks.task1(input.mod(), input.oddEven());

        // event_0 (EVENT). Sinks "sqs:queue" and "kafka:topic" work the same way.
        event("event_0").to("conductor").send(stats);

        // task_2 (DYNAMIC): the task name is a runtime value, so this is the by-name call
        OddEven oddEven = task(input.task2Name()).ref("task_2")
                .input("mod", stats.mod())
                .input("oddEven", stats.oddEven())
                .as(OddEven.class);

        // switch_oddEven (SWITCH): a plain Java switch whose cases call the server. The execution
        // shows it as a SWITCH with every case and the one the value took; the taken case runs as
        // ordinary code and its calls land inside that branch, the others show what they would call.
        switch (oddEven.oddEven()) {
            case "0" -> {
                // task_4 (SIMPLE) decides what to fan out; fanout1 + join1 (FORK_JOIN_DYNAMIC + JOIN).
                // A case may make several calls; fanOut runs in stages, one dynamic fork per stage.
                FanoutPlan plan = tasks.task4(oddEven);
                List<Map<String, Object>> results = fanOut("fanout1", plan.tasks(),
                        t -> task(t.name()).input(t.input()).asMap()).get();
                notes.add("fanned out " + results.size() + " tasks");
            }
            case "1" -> {
                // forkx + join2 (FORK_JOIN + JOIN). Each async block is a branch that runs on its own:
                // the first child starts the moment task_10 finishes, whatever task_11 is doing.
                // (Child refs derive as sub_flow_1 and sub_flow_1_2; the JSON calls them wf3 and wf4.)
                Promise<SubFlowResult> wf3 = async(() -> {
                    tasks.task10();
                    return child(SubFlow::run, new SubFlowInput(stats.mod(), stats.oddEven()));
                });
                Promise<SubFlowResult> wf4 = async(() -> {
                    tasks.task11();
                    return child(SubFlow::run, new SubFlowInput(stats.mod(), stats.oddEven()));
                });
                join(wf3, wf4);
                notes.add("sub flows: " + wf3.get().task6() + " / " + wf4.get().task6());
            }
            default -> throw new WorkflowFailedException("unexpected oddEven: " + oddEven.oddEven());
        }

        // get_es_1 (HTTP): runs on the server, no worker involved. Needs Elasticsearch on 9200,
        // as the JSON kitchensink does; without it the call fails and we carry on with empty lists.
        List<String> statuses = List.of();
        List<String> workflowIds = List.of();
        try {
            JsonNode search = http("get_es_1")
                    .get("http://localhost:9200/conductor/_search?size=10")
                    .asJson();
            statuses = search.findValuesAsText("status");         // ${get_es_1.output..status}
            workflowIds = search.findValuesAsText("workflowId");   // ${get_es_1.output..workflowId}
        } catch (TaskFailure e) {
            notes.add("search unavailable: " + e.getMessage());
        }

        // task_30 (SIMPLE)
        Summary summary = tasks.task30(statuses, workflowIds);

        // ---------------------------------------------------------------- section 2: beyond the JSON sink
        stage = "extended";

        // A timer in a plain loop. Each sleep is a WAIT task; the code wakes once per iteration.
        for (int tick = 1; tick <= 3 && !cancelled; tick++) {
            sleep(Duration.ofSeconds(tick));
            notes.add("tick " + tick + " at " + now());
        }

        // A polling loop with backoff: check, wait, check again. Each check is a task and each
        // wait a WAIT; the code wakes after each one. (repeatUntil runs the whole loop on the server.)
        JobStatus status = tasks.checkStatus(runId);
        Duration wait = Duration.ofSeconds(5);
        for (int attempt = 1; !status.done() && attempt < 20; attempt++) {
            sleep(wait);
            wait = wait.multipliedBy(2).compareTo(Duration.ofMinutes(5)) < 0 ? wait.multipliedBy(2) : Duration.ofMinutes(5);
            status = tasks.checkStatus(runId);
        }

        // Compensation is plain try/catch. A task whose retries are exhausted throws TaskFailure.
        // If task_30 fails after the charge succeeded, the refund runs.
        Receipt receipt = null;
        try {
            receipt = tasks.charge(runId, input.mod() * 100);
            Receipt charged = receipt;
            compensations.push(() -> tasks.refund(runId, charged.amount()));
            tasks.task30(List.of("after-charge"), workflowIds);
        } catch (TaskFailure e) {
            notes.add("compensating: " + e.getMessage());
            while (!compensations.isEmpty()) {
                compensations.pop().run();
            }
        }

        // HUMAN task: shows up in the UI and the human task API. Empty when nobody answers in time.
        Optional<Approval> approval = human("approval")
                .assignee("finance")
                .input(Map.of("runId", runId, "amount", input.mod() * 100))
                .await(Approval.class, Duration.ofDays(2));

        // External callback: a WAIT that any system completes with
        //   POST /api/tasks/{workflowId}/callback/COMPLETED   {"payload": "..."}
        Optional<Callback> callback = awaitExternal("callback", Callback.class)
                .await(Duration.ofHours(1));

        // Wait for something a handler changes. Handlers mutate variables, this re-checks them.
        boolean enoughNotes = awaitCondition(() -> cancelled || notes.size() >= 5, Duration.ofMinutes(10));

        // Fire-and-forget workflow (START_WORKFLOW): no join, no wait, returns the new workflow id.
        // By name, startWorkflow("audit_log", 1, input) starts any registered definition.
        startWorkflow(SubFlow::run, new SubFlowInput(input.mod(), enoughNotes ? "1" : "0"));

        // LLM call on the server (LLM_CHAT_COMPLETE): no model SDK on the worker. Needs a model
        // provider configured on the server; without one we fall back to a plain note.
        String note;
        try {
            note = llm("delivery_note")
                    .model("openai/gpt-4o-mini")
                    .prompt("Summarize these notes in one sentence: " + String.join("; ", notes))
                    .as(String.class);
        } catch (TaskFailure e) {
            note = String.join("; ", notes);
        }

        // ---------------------------------------------------------------- section 3: iteration, dates, AI
        stage = "ai";

        // One task per item, in order. (forEach runs the loop on the server and wakes the code once.)
        List<String> labels = new ArrayList<>();
        for (String box : List.of("box-1", "box-2", "box-3")) {
            labels.add(tasks.labelItem(box));
        }

        // WAIT until an instant (resolved to the minute by the server).
        sleepUntil(now().plusSeconds(60));

        // MCP: list a server's tools and call one, from the Conductor server.
        List<McpCall.McpTool> tools = mcp("mcp_tools").server(input.mcpServer()).listTools();
        McpCall.McpResult weather =
                mcp("mcp_weather").server(input.mcpServer()).call("get_weather", Map.of("city", "Seattle"));

        // An AI agent over A2A, discovered from its agent card; the server waits for its reply.
        AgentCall.AgentResult reply =
                agent("support_agent").remote(input.agentUrl()).ask("Summarize order " + runId);

        // ---------------------------------------------------------------- section 4: composition
        stage = "composition";
        String lane = oddEven.oddEven();
        List<String> composed = new ArrayList<>();

        // if -> if / switch -> task: the if's true arm holds another if, its else a switch.
        boolean odd = lane.equals("1");
        String nested;
        if (odd) {
            boolean big = input.mod() > 3;
            if (big) {
                nested = tasks.step("odd-big");
            } else {
                nested = tasks.step("odd-small");
            }
        } else {
            switch (lane) {
                case "0" -> nested = tasks.step("even-only");
                default -> nested = "no task";
            }
        }
        composed.add(nested);

        // while -> if: the diagram draws a loop around whatever a Java loop runs, and the if once
        // per pass (if_pass, if_pass_2).
        int pass = 1;
        while (pass <= 2) {
            if (pass % 2 == 1) {
                composed.add(tasks.step("while-odd"));
            } else {
                composed.add(tasks.step("while-even"));
            }
            pass++;
        }

        // loop -> fork -> if: a fork and its join each round, with an if in one branch.
        for (int round = 1; round <= 2; round++) {
            String name = "r" + round;
            boolean first = round == 1;
            Promise<String> left = async(() -> {
                String picked;
                if (first) {
                    picked = tasks.step(name + "-first");
                } else {
                    picked = tasks.step(name + "-later");
                }
                return picked;
            });
            Promise<String> right = async(() -> tasks.step(name + "-right"));
            join(left, right);
            composed.add(left.get());
            composed.add(right.get());
        }

        // fork -> loop: a loop in each branch, each lane running its own.
        Promise<List<String>> labelLoop = async(() -> {
            List<String> labelled = new ArrayList<>();
            for (String item : List.of("x", "y")) {
                labelled.add(tasks.labelItem(item));
            }
            return labelled;
        });
        Promise<String> codeLoop = async(() -> {
            String last = "";
            for (int i = 1; i <= 2; i++) {
                last = tasks.step("code-loop-" + i);
            }
            return last;
        });
        join(labelLoop, codeLoop);
        composed.addAll(labelLoop.get());
        composed.add(codeLoop.get());

        // fork -> sub workflow -> join: a child next to a branch with a switch and a task after it.
        SubFlowInput childInput = new SubFlowInput(input.mod(), lane);
        Promise<SubFlowResult> childLane = async(() -> child(SubFlow::run, childInput));
        Promise<String> beside = async(() -> {
            String picked;
            String side = childInput.oddEven();
            switch (side) {
                case "1" -> picked = tasks.step("beside-odd");
                default -> picked = tasks.step("beside-even");
            }
            return tasks.step(picked + "-then");
        });
        join(childLane, beside);
        composed.add(beside.get());

        // if -> fork: a lane started inside an arm, and joined after the if.
        Promise<String> fromArm;
        if (lane.equals("1")) {
            fromArm = async(() -> tasks.step("case-lane-odd"));
        } else {
            fromArm = async(() -> tasks.step("case-lane-even"));
        }
        composed.add(fromArm.get());

        // fork -> if -> fork: a lane starts another inside an if and does not join it.
        // Joining the outer lane still waits for the inner one, and its slow task.
        Promise<String> outerLane = async(() -> {
            boolean startInner = input.mod() >= 0;
            if (startInner) {
                async(() -> tasks.slowStep("inner-lane"));
            } else {
                notes.add("not taken");
            }
            return tasks.step("after-case");
        });
        join(outerLane);
        composed.add(task("step").ref("after_outer_join").input("label", outerLane.get()).as(String.class));

        // loop -> if -> fork -> (sub workflow, polling loop) -> join: everything at once.
        for (int depth = 1; depth <= 2; depth++) {
            String result;
            if (depth == 1) {
                Promise<SubFlowResult> deepChild = async(() -> child(SubFlow::run, childInput));
                Promise<JobStatus> polled = async(() -> {
                    JobStatus deep = tasks.checkStatus(runId + "-deep");
                    while (!deep.done()) {
                        sleep(Duration.ofSeconds(1));
                        deep = tasks.checkStatus(runId + "-deep");
                    }
                    return deep;
                });
                join(deepChild, polled);
                result = polled.get().done() ? "deep-done" : "deep-not-done";
            } else {
                List<String> items = new ArrayList<>();
                for (String item : List.of("p", "q")) {
                    items.add(tasks.labelItem(item));
                }
                result = tasks.step("deep-" + items.size());
            }
            composed.add(result);
        }

        // for -> for -> if: nested Java loops show one inside the other.
        for (int outer = 1; outer <= 2; outer++) {
            String prefix = "o" + outer;
            for (int inner = 1; inner <= 2; inner++) {
                if (inner == 1) {
                    composed.add(tasks.step(prefix + "-first"));
                } else {
                    composed.add(tasks.step(prefix + "-second"));
                }
            }
        }

        stage = "done";
        return new KitchenSinkResult(
                runId,
                oddEven.oddEven(),
                summary.count(),
                statuses,
                workflowIds,
                receipt != null,
                approval.map(Approval::approver).orElse(null),
                callback.map(Callback::payload).orElse(null),
                status.done() ? note : note + " (job did not finish)",
                cancelled,
                labels,
                tools.size(),
                weather.text(),
                reply.text(),
                composed);
    }
}
