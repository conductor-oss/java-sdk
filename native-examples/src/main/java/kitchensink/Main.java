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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import kitchensink.KitchenSinkModel.Approval;
import kitchensink.KitchenSinkModel.Callback;
import kitchensink.KitchenSinkModel.CancelRequest;
import kitchensink.KitchenSinkModel.KitchenSinkInput;
import kitchensink.KitchenSinkModel.KitchenSinkResult;

/**
 * Hosts the workers and drives three executions end to end against a running server, playing the
 * outside world too: a finance user answering the human task and a carrier posting the callback.
 *
 * <pre>
 *   odd path   (oddEven "1"): two async lanes, each a task then a child workflow, joined
 *   even path  (oddEven "0"): a task plans a fan-out, one lane per planned task
 *   failure    (mod 11):      the charge fails terminally, the code catches TaskFailure
 * </pre>
 *
 * ConductorClient.fromEnvironment() reads CONDUCTOR_SERVER_URL and CONDUCTOR_AUTH_*, falling back
 * to the local server at http://localhost:8080/api. Set KITCHENSINK_EXPORT to a file path to save the odd-path
 * execution, which KitchenSinkTest replays.
 */
public class Main {

    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // The client is this program's to shut down; Conductor uses it and leaves it open.
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);

        // KITCHENSINK_HOST=false drives the executions without hosting the workers, so a separate
        // kitchensink.Worker process can be killed and restarted mid-run (see chaos.sh).
        if (!"false".equals(System.getenv("KITCHENSINK_HOST"))) {
            Worker.host(conductor);
        }

        String stamp = String.valueOf(System.currentTimeMillis());
        // MCP server and A2A agent: stubs/ai_stub.py by default
        String ai = System.getenv().getOrDefault("KITCHENSINK_AI_URL", "http://localhost:8765");
        String mcp = ai + "/mcp";
        String agentCard = ai + "/.well-known/agent-card.json";
        KitchenSinkResult odd = runOrReport(conductor, new KitchenSinkInput(5, "1", "task_5", mcp, agentCard), "odd-" + stamp, true);
        if (odd != null) {
            check("odd: switch took case 1", "1".equals(odd.oddEven()));
            check("odd: charged", odd.charged());
        }

        KitchenSinkResult even = runOrReport(conductor, new KitchenSinkInput(5, "0", "task_5", mcp, agentCard), "even-" + stamp, false);
        if (even != null) {
            check("even: switch took case 0", "0".equals(even.oddEven()));
        }

        KitchenSinkResult failed = runOrReport(conductor, new KitchenSinkInput(11, "1", "task_5", mcp, agentCard), "fail-" + stamp, false);
        if (failed != null) {
            check("failure: charge over the limit is not charged", !failed.charged());
        }

        conductor.close();
        client.shutdown();
        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("KITCHENSINK PASSED: all checks passed for all three executions");
            System.exit(0);
        }
        System.out.println("KITCHENSINK FAILED: " + failures);
        System.exit(1);
    }

    /**
     * Runs one execution. With {@code KITCHENSINK_KEEP_GOING} (the chaos run, which injects task
     * failures) an execution that fails is reported and the next one runs; its checks are skipped.
     */
    private static KitchenSinkResult runOrReport(Conductor conductor, KitchenSinkInput input, String key, boolean export)
            throws Exception {
        if (System.getenv("KITCHENSINK_KEEP_GOING") == null) {
            return runOne(conductor, input, key, export);
        }
        try {
            return runOne(conductor, input, key, export);
        } catch (RuntimeException e) {
            System.out.println("ENDED " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private static KitchenSinkResult runOne(Conductor conductor, KitchenSinkInput input, String key, boolean export) throws Exception {
        System.out.println();
        System.out.println("=== kitchensink " + input);

        // Typed start with an idempotency key: a second start with the key returns the same run.
        WorkflowHandle<KitchenSinkResult> run =
                conductor.workflow(KitchenSinkWorkflow::kitchenSink).idempotencyKey("kitchensink-" + key).correlationId(key).priority(5).start(input);
        WorkflowHandle<KitchenSinkResult> again =
                conductor.workflow(KitchenSinkWorkflow::kitchenSink).idempotencyKey("kitchensink-" + key).start(input);
        System.out.println("started " + run.workflowId() + "  " + run.url());
        check("idempotent start returns the same execution", run.workflowId().equals(again.workflowId()));

        // Handlers by method reference: an update with a reply, a query, a signal.
        int noteCount = run.update(KitchenSinkWorkflow::addNote, "first note");
        String summary = run.query(KitchenSinkWorkflow::summary);
        run.signal(KitchenSinkWorkflow::cancel, new CancelRequest("demo"));
        System.out.println("update -> " + noteCount + " notes; query -> " + summary);
        check("update returns the handler's value", noteCount >= 1);
        check("query reads the variables", summary != null && summary.contains("notes"));

        // The outside world: a person answers the human task, a carrier posts the callback.
        run.completeTask("approval", new Approval("finance-bot", true), Duration.ofMinutes(10));
        System.out.println("answered the approval");
        run.completeTask("callback", new Callback("delivered"), Duration.ofMinutes(10));
        System.out.println("posted the callback");

        KitchenSinkResult result = run.result(Duration.ofMinutes(10));
        System.out.println("result: " + result);
        System.out.println("variables: " + run.variables());

        check("run id recorded once", result.runId() != null && result.runId().startsWith("ks-"));
        check("cancel signal observed by the code", result.cancelled());
        check("approval answered", "finance-bot".equals(result.approvedBy()));
        check("callback received", "delivered".equals(result.callbackPayload()));
        check("the polling loop finished", result.note() != null && !result.note().endsWith("(job did not finish)"));
        check("stage variable is done", "done".equals(run.variables().get("stage")));
        check("a loop over items labelled each, in order", List.of("LABEL[box-1]", "LABEL[box-2]", "LABEL[box-3]").equals(result.labels()));
        check("MCP server listed its tools", result.toolCount() == 2);
        check("MCP tool call returned its text", result.weather() != null && result.weather().contains("Seattle"));
        check("agent replied over A2A", result.agentReply() != null && result.agentReply().contains("reviewed"));
        check("start priority applied", Integer.valueOf(5).equals(field(conductor, run.workflowId(), "priority")));
        check("the SWITCH task recorded case " + input.oddEven() + " as taken",
                input.oddEven().equals(caseTaken(conductor, run.workflowId(), "switch_oddEven")));
        List<String> composed = new ArrayList<>(List.of(
                "1".equals(input.oddEven()) ? "odd-big" : "even-only", "while-odd", "while-even",
                "r1-first", "r1-right", "r2-later", "r2-right",
                "LABEL[x]", "LABEL[y]", "code-loop-2",
                "1".equals(input.oddEven()) ? "beside-odd-then" : "beside-even-then",
                "1".equals(input.oddEven()) ? "case-lane-odd" : "case-lane-even",
                "after-case", "deep-done", "deep-2",
                "o1-first", "o1-second", "o2-first", "o2-second"));
        check("composed constructs returned their results, in order: " + result.composed(), composed.equals(result.composed()));
        // every plain if and switch whose arms call the server is a SWITCH, named after what it decides on
        for (String sw : List.of("if_odd", "1".equals(input.oddEven()) ? "if_big" : "switch_lane", "if_pass", "if_pass_2",
                "if_first", "if_first_2", "switch_side", "if_lane", "if_startInner", "if_depth", "if_depth_2",
                "if_inner", "if_inner_2", "if_inner_3", "if_inner_4")) {
            check("composition: " + sw + " ran", caseTaken(conductor, run.workflowId(), sw) != null);
        }
        check("composition: the else of an if is its default case",
                "false".equals(caseTaken(conductor, run.workflowId(), "if_pass_2")));
        check("composition: an if in a Java loop is tagged with its loop",
                loopsOf(conductor, run.workflowId(), "if_pass").size() == 1);
        check("composition: an if in a fork in a Java loop is tagged with its loop",
                loopsOf(conductor, run.workflowId(), "if_first").size() == 1);
        check("composition: nested Java loops are both named",
                loopsOf(conductor, run.workflowId(), "if_inner").size() == 2);
        check("composition: an if outside every loop is not tagged", loopsOf(conductor, run.workflowId(), "if_odd").isEmpty());
        check("composition: a join waits for a lane started inside a switch case",
                startOf(conductor, run.workflowId(), "after_outer_join") >= endOf(conductor, run.workflowId(), "slow_step"));
        long httpAttempts = attempts(conductor, run.workflowId(), "get_es_1");
        check("the HTTP call was retried three times before its failure reached the code (" + httpAttempts + " attempts)", httpAttempts == 4);

        if (export && System.getenv("KITCHENSINK_EXPORT") != null) {
            Map<String, Object> execution = conductor.client().execute(
                    ConductorClientRequest.builder()
                            .method(ConductorClientRequest.Method.GET)
                            .path("/workflow/" + run.workflowId())
                            .addQueryParam("includeTasks", true)
                            .build(),
                    new TypeReference<Map<String, Object>>() {}).getData();
            Path out = Path.of(System.getenv("KITCHENSINK_EXPORT"));
            Files.createDirectories(out.getParent());
            Files.writeString(out, new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(execution));
            System.out.println("exported execution to " + out);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> taskNamed(Conductor conductor, String workflowId, String ref) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return ((List<Map<String, Object>>) wf.get("tasks")).stream()
                .filter(t -> ref.equals(t.get("referenceTaskName"))).findFirst().orElse(Map.of());
    }

    @SuppressWarnings("unchecked")
    private static List<String> loopsOf(Conductor conductor, String workflowId, String ref) {
        Map<String, Object> entry = (Map<String, Object>) taskNamed(conductor, workflowId, ref).get("workflowTask");
        Object loops = ((Map<String, Object>) entry.get("metadata")).get("loops");
        return loops == null ? List.of() : (List<String>) loops;
    }

    private static long startOf(Conductor conductor, String workflowId, String ref) {
        return ((Number) taskNamed(conductor, workflowId, ref).getOrDefault("scheduledTime", -1)).longValue();
    }

    private static long endOf(Conductor conductor, String workflowId, String ref) {
        return ((Number) taskNamed(conductor, workflowId, ref).getOrDefault("endTime", Long.MAX_VALUE)).longValue();
    }

    private static Object field(Conductor conductor, String workflowId, String name) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", false).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return wf.get(name);
    }

    @SuppressWarnings("unchecked")
    private static long attempts(Conductor conductor, String workflowId, String ref) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        return ((List<Map<String, Object>>) wf.get("tasks")).stream().filter(t -> ref.equals(t.get("referenceTaskName"))).count();
    }

    /** The case a SWITCH task took ({@code selectedCase}), or null when it has not run. */
    @SuppressWarnings("unchecked")
    private static String caseTaken(Conductor conductor, String workflowId, String ref) {
        Map<String, Object> wf = conductor.client().execute(
                ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET).path("/workflow/" + workflowId)
                        .addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        for (Map<String, Object> t : (List<Map<String, Object>>) wf.get("tasks")) {
            if (ref.equals(t.get("referenceTaskName")) && t.get("outputData") instanceof Map<?, ?> out
                    && out.get("selectedCase") != null) {
                return String.valueOf(out.get("selectedCase"));
            }
        }
        return null;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what);
        if (!ok) {
            failures.add(what);
        }
    }
}
