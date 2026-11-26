/*
 * Copyright 2025 Conductor Authors.
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentConfig;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.enums.Strategy;
import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.*;

import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.Javascript;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.model.agent.CompileResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Suite 20: JEV decision routing — deterministic, compile/register only.
 *
 * <p>Covers the three decision usages without calling any model, so no decision or chat
 * credentials are needed:
 * <ul>
 *   <li>Decision router: a decision {@code ToolDef} as {@code router(...)} compiles into a
 *       branchless decision SWITCH feeding the ROUTER loop.</li>
 *   <li>Decision tool: the tool config reaches the compiled workflow.</li>
 *   <li>Decision SWITCH: a workflow with {@code evaluatorType=decision} registers and reads
 *       back unchanged.</li>
 * </ul>
 *
 * <p>Requires conductor-oss 3.33.0+ (decision support, conductor-oss PR #1664); skipped on
 * older servers.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Suite20DecisionRouting extends BaseTest {

    private static final String DECISION_MODEL = "jev-1.13";

    private static AgentRuntime runtime;

    @BeforeAll
    static void setup() {
        assumeDecisionCapableServer();
        runtime = new AgentRuntime(new AgentConfig(100, 1));
    }

    @AfterAll
    static void teardown() {
        if (runtime != null) runtime.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Skip unless the server version is at least 3.33.0, the first release with decision support. */
    static void assumeDecisionCapableServer() {
        String version;
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/api/version"))
                            .timeout(Duration.ofSeconds(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            version = response.body() == null ? "" : response.body().trim();
        } catch (Exception e) {
            assumeTrue(false, "Cannot read server version: " + e.getMessage());
            return;
        }
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)").matcher(version);
        assumeTrue(m.find(), "Unrecognized server version '" + version + "' — skipping decision suite");
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        assumeTrue(major > 3 || (major == 3 && minor >= 33),
                "Server " + version + " predates decision support (needs 3.33.0+) — skipping decision suite");
    }

    private static Map<String, Object> choiceQuestion(Map<String, String> choices) {
        return Map.of("type", "choice", "instructions", "Choose the right support team.", "choices", choices);
    }

    private static ToolDef decisionTool(String name, Map<String, Object> questions) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("model", DECISION_MODEL);
        config.put("questions", questions);
        return ToolDef.builder().name(name).description("Decision selector.").toolType("decision").config(config)
                .build();
    }

    private static Agent decisionRouter(String name, Map<String, String> choices) {
        Agent billing = Agent.builder().name("e2e_java_decision_billing").model(MODEL)
                .instructions("Help with billing.").build();
        Agent technical = Agent.builder().name("e2e_java_decision_technical").model(MODEL)
                .instructions("Help with product issues.").build();
        return Agent.builder().name(name).strategy(Strategy.ROUTER)
                .router(decisionTool(name + "_selector", Map.of("agent", choiceQuestion(choices))))
                .agents(billing, technical).maxTurns(1).synthesize(false).build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    /**
     * A decision router compiles into a decision SWITCH whose empty branches are the child agents.
     *
     * COUNTERFACTUAL: if the parent were serialized as external (no model), the server would refuse
     * to compile it; if the router were serialized as a nested agent, an LLM task would appear instead.
     */
    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void test_decision_router_compiles_to_decision_switch() {
        Agent agent = decisionRouter("e2e_java_decision_router", Map.of(
                "e2e_java_decision_billing", "Payments and invoices",
                "e2e_java_decision_technical", "Product errors"));

        CompileResponse plan = runtime.plan(agent);
        Map<String, Object> agentDef = getAgentDef(plan);
        assertEquals(Boolean.FALSE, agentDef.get("external"),
                "decision router parent must not be external. COUNTERFACTUAL: Agent.isExternal() must exclude "
                        + "ROUTER agents with a decision router.");
        Map<String, Object> router = (Map<String, Object>) agentDef.get("router");
        assertNotNull(router, "agentDef.router is missing");
        assertEquals("decision", router.get("kind"), "agentDef.router.kind should be 'decision': " + router);

        List<Map<String, Object>> decisionSwitches = allTasksFlat(plan.getWorkflowDef()).stream()
                .filter(t -> "SWITCH".equals(t.get("type")) && "decision".equals(t.get("evaluatorType")))
                .toList();
        assertEquals(1, decisionSwitches.size(),
                "expected exactly one decision SWITCH in the compiled router, got " + decisionSwitches.size());
        Map<String, Object> selector = decisionSwitches.get(0);

        assertEquals("agent", selector.get("expression"), "expression should name the agent choice question");
        Map<String, Object> inputs = (Map<String, Object>) selector.get("inputParameters");
        assertEquals(DECISION_MODEL, inputs.get("model"));
        assertNotNull(inputs.get("questions"), "questions must be forwarded to the decision SWITCH");
        assertEquals("${workflow.variables.conversation}", inputs.get("state"),
                "state must be wired to the conversation");

        Map<String, List<?>> cases = (Map<String, List<?>>) selector.get("decisionCases");
        assertEquals(Set.of("e2e_java_decision_billing", "e2e_java_decision_technical"), cases.keySet(),
                "decision cases must be the child agent names");
        cases.forEach((name, tasks) -> assertTrue(tasks.isEmpty(),
                "selector branch '" + name + "' should be empty; the ROUTER SWITCH runs the agent"));
    }

    /**
     * The server rejects a decision router whose choices do not match the child agent names.
     *
     * COUNTERFACTUAL: if this compiled, JEV could select a name with no agent behind it.
     */
    @Test
    @Order(2)
    void test_decision_router_rejects_mismatched_choices() {
        Agent agent = decisionRouter("e2e_java_decision_router_mismatch", Map.of(
                "billing", "Payments and invoices",
                "technical", "Product errors"));

        AgentAPIException ex = assertThrows(AgentAPIException.class, () -> runtime.plan(agent),
                "server must refuse a decision router whose choices are not the child agent names");
        assertTrue(ex.getMessage().contains("choices are the configured agent names"),
                "unexpected rejection message: " + ex.getMessage());
    }

    /**
     * A decision tool's config reaches the compiled workflow so the server can build the SWITCH
     * when the LLM calls the tool.
     *
     * COUNTERFACTUAL: if toolType or config were dropped, the tool would compile as a worker task.
     */
    @Test
    @Order(3)
    @SuppressWarnings("unchecked")
    void test_decision_tool_config_reaches_compiled_workflow() {
        ToolDef classify = decisionTool("e2e_java_decision_classify", Map.of("department",
                choiceQuestion(Map.of("billing", "Payments and invoices", "technical", "Product errors"))));
        Agent agent = Agent.builder().name("e2e_java_decision_tool_agent").model(MODEL)
                .instructions("Call e2e_java_decision_classify before answering.").tools(classify).build();

        CompileResponse plan = runtime.plan(agent);
        Map<String, Object> agentDef = getAgentDef(plan);
        Map<String, Object> tool = ((List<Map<String, Object>>) agentDef.get("tools")).get(0);
        assertEquals("decision", tool.get("toolType"));

        assertEquals(DECISION_MODEL, ((Map<String, Object>) tool.get("config")).get("model"));

        // The server builds the decision SWITCH at tool-call time from the config embedded in the
        // enrich script, so the compiled workflow must carry it there.
        Map<String, Object> enrich = allTasksFlat(plan.getWorkflowDef()).stream()
                .filter(t -> "INLINE".equals(t.get("type"))
                        && String.valueOf(t.get("taskReferenceName")).endsWith("_enrich_tools"))
                .findFirst()
                .orElseGet(() -> fail("compiled workflow has no *_enrich_tools INLINE task"));
        String script = String.valueOf(((Map<String, Object>) enrich.get("inputParameters")).get("expression"));
        assertTrue(script.contains("e2e_java_decision_classify") && script.contains(DECISION_MODEL),
                "enrich script must embed the decision tool config. COUNTERFACTUAL: without it the tool call "
                        + "cannot be mapped to a decision SWITCH.");
    }

    /**
     * A decision SWITCH registers on the server and reads back with its evaluator, expression,
     * inputs, and branches intact, and converts back into an SDK Switch unchanged.
     *
     * COUNTERFACTUAL: if Switch rewrote the evaluator to value-param, the server would store
     * value-param and never call the decision model.
     */
    @Test
    @Order(4)
    void test_decision_switch_registers_and_round_trips() {
        WorkflowExecutor.initTaskImplementations();
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(null);
        workflow.setName("e2e_java_decision_switch");
        workflow.setVersion(1);
        workflow.setOwnerEmail("e2e@example.com");
        Switch decision = new Switch("route_request", "route")
                .evaluatorType(Switch.DECISION_NAME)
                .input("model", DECISION_MODEL)
                .input("state", "${workflow.input.request}")
                .input("questions", Map.of("route",
                        choiceQuestion(Map.of("billing", "Payments", "technical", "Errors"))))
                .switchCase("billing", new Javascript("handle_billing", "'billing'"))
                .switchCase("technical", new Javascript("handle_technical", "'technical'"));
        workflow.add(decision);

        ApiClient client = new ApiClient(SERVER_URL);
        MetadataClient metadata = new MetadataClient(client);
        try {
            metadata.updateWorkflowDefs(List.of(workflow.toWorkflowDef()));

            WorkflowDef stored = metadata.getWorkflowDef("e2e_java_decision_switch", 1);
            WorkflowTask task = stored.getTasks().get(0);
            assertEquals("SWITCH", task.getType());
            assertEquals("decision", task.getEvaluatorType(),
                    "server stored evaluatorType " + task.getEvaluatorType()
                            + ". COUNTERFACTUAL: Switch.evaluatorType(...) must not be rewritten to value-param.");
            assertEquals("route", task.getExpression());
            assertEquals(DECISION_MODEL, task.getInputParameters().get("model"));
            assertFalse(task.getInputParameters().containsKey("switchCaseValue"),
                    "decision SWITCH must not carry the value-param switchCaseValue input");
            assertEquals(Set.of("billing", "technical"), task.getDecisionCases().keySet());

            WorkflowTask roundTripped =
                    ConductorWorkflow.fromWorkflowDef(stored).toWorkflowDef().getTasks().get(0);
            assertEquals(task.getEvaluatorType(), roundTripped.getEvaluatorType());
            assertEquals(task.getExpression(), roundTripped.getExpression());
            assertEquals(task.getInputParameters(), roundTripped.getInputParameters());
            assertEquals(task.getDecisionCases().keySet(), roundTripped.getDecisionCases().keySet());
        } finally {
            try {
                metadata.unregisterWorkflowDef("e2e_java_decision_switch", 1);
            } catch (Exception ignored) {
                // Best-effort cleanup; never mask the test result.
            }
            client.shutdown();
        }
    }
}
