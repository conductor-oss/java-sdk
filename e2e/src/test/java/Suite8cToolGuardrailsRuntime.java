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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentConfig;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.enums.AgentStatus;
import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.guardrail.Guardrail;
import org.conductoross.conductor.ai.guardrail.RegexGuardrail;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.GuardrailDef;
import org.conductoross.conductor.ai.model.GuardrailResult;
import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite 8c: Tool Guardrails — runtime behavior tests.
 *
 * <p>Every prior guardrail e2e test either drives an agent-level guardrail
 * (Suite8Guardrails) or only inspects the compiled workflow JSON for a
 * tool-level guardrail (Suite8bGuardrailsExtended) — nothing anywhere
 * actually executed a tool-level guardrail. That gap let a real bug ship:
 * before SDK commit {@code 55c74ddd}, the client never registered a worker
 * for tool-level custom guardrails, so the compiled guardrail task sat
 * unclaimed on the server and the check silently never ran.
 *
 * <p>COUNTERFACTUAL: each test below is designed to fail if the guardrail
 * silently does nothing, exactly the failure mode above.
 *
 * <p>Retry-escalation for tool guardrails (idea-15 gap G12 — the server
 * hardcodes the retry-loop iteration to {@code "1"}, so {@code onFail=RETRY}
 * can never escalate to {@code RAISE}) is intentionally NOT covered here —
 * that requires a server fix not yet released; see
 * {@code idea-15/implementation-plan-java-e2e.md} Stage E3.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Suite8cToolGuardrailsRuntime extends BaseTest {

    private static AgentRuntime runtime;
    private static final AtomicBoolean toolWasCalled = new AtomicBoolean(false);

    @BeforeAll
    static void setup() {
        runtime = new AgentRuntime(new AgentConfig(100, 1));
    }

    @AfterAll
    static void teardown() {
        if (runtime != null) runtime.close();
    }

    // ── Tool class ────────────────────────────────────────────────────────

    static class EchoTools {
        @Tool(name = "echo", description = "Echo the given input text back")
        public String echo(String input) {
            toolWasCalled.set(true);
            return input;
        }
    }

    private static ToolDef echoTool() {
        return ToolRegistry.fromInstance(new EchoTools()).get(0);
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    /**
     * Tool-level RAISE guardrail that always fails blocks the run.
     *
     * <p>The tool-guardrail gate runs pre-execution against the formatted tool
     * call (idea-15 gap G13) — so a RAISE guardrail correctly blocking the call
     * means {@code echo}'s function body never runs at all. That is the
     * guardrail working as intended, not a failure of this test.
     *
     * COUNTERFACTUAL (verified by disabling the fix and re-running): if the
     * tool-guardrail worker is never registered (the pre-{@code 55c74ddd}
     * bug), the compiled guardrail task sits SCHEDULED and unpolled on the
     * server — only {@code echo} shows up in the client's poll log, never
     * {@code echo_output_guardrail} — and the agent stays RUNNING forever
     * instead of reaching FAILED/TERMINATED (bounded only by
     * {@code AgentHandle}'s 10-minute default wait timeout, since a
     * stateless {@code run()} has no liveness monitor to fail fast).
     */
    @Test
    @Order(1)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void test_tool_raise_guardrail_blocks_run() {
        toolWasCalled.set(false); // reset before each run

        GuardrailDef alwaysBlock = Guardrail.of(
                        "e2e_tool_raise_guard", content -> GuardrailResult.fail("blocked for e2e test"))
                .onFail(OnFail.RAISE)
                .build();

        ToolDef guardedTool = echoTool().withGuardrails(List.of(alwaysBlock));

        Agent agent = Agent.builder()
                .name("e2e_java_tool_raise_guard_agent")
                .model(MODEL)
                .instructions("You MUST call the echo tool with input='hello'. Then report its result.")
                .tools(List.of(guardedTool))
                .maxTurns(3)
                .build();

        AgentResult result = runtime.run(agent, "Please echo 'hello'.");

        assertTrue(
                result.getStatus() == AgentStatus.FAILED || result.getStatus() == AgentStatus.TERMINATED,
                "Expected agent to FAIL or TERMINATE after a tool-level RAISE guardrail blocked the "
                        + "call. Got status: " + result.getStatus()
                        + ". COUNTERFACTUAL: if the tool-guardrail worker is never registered, the "
                        + "agent completes normally instead of failing.");
    }

    /**
     * Tool-level guardrail that always passes lets the run complete.
     *
     * COUNTERFACTUAL: proves the worker exercised by test 1 is actually being
     * polled and does not block unconditionally — if it always raised
     * regardless of the guardrail function's result, this test would also
     * see a FAILED/TERMINATED status.
     */
    @Test
    @Order(2)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void test_tool_pass_guardrail_lets_run_complete() {
        toolWasCalled.set(false);

        GuardrailDef alwaysPass =
                Guardrail.of("e2e_tool_pass_guard", content -> GuardrailResult.pass())
                        .onFail(OnFail.RAISE)
                        .build();

        ToolDef guardedTool = echoTool().withGuardrails(List.of(alwaysPass));

        Agent agent = Agent.builder()
                .name("e2e_java_tool_pass_guard_agent")
                .model(MODEL)
                .instructions("You MUST call the echo tool with input='hello'. Then report its result.")
                .tools(List.of(guardedTool))
                .maxTurns(3)
                .build();

        AgentResult result = runtime.run(agent, "Please echo 'hello'.");

        assertTrue(toolWasCalled.get(), "The echo tool function body never ran.");

        assertEquals(
                AgentStatus.COMPLETED,
                result.getStatus(),
                "Agent did not complete with an always-passing tool guardrail attached. Status: "
                        + result.getStatus() + ". Error: " + result.getError());
    }

    /**
     * Agent-level FIX guardrail must substitute its corrected output into the
     * final result on a COMPLETED run, not fail the run.
     *
     * <p>Deliberately NOT gated on {@code ServerCapabilities}, unlike the
     * tests below — this one already passes against today's server (verified
     * live). It exists to guard against a regression the naive form of the
     * G12/G1 server fix would introduce: upstream commit {@code cc025ed0}
     * adds an {@code escalate()} helper to the custom-guardrail normalizer
     * that coerces {@code fix → raise} unconditionally, without checking
     * whether a {@code fixed_output} is actually present — unlike the SDK's
     * own worker handler, which only coerces when {@code fixedOutput == null}.
     * Applied as-is, that upstream commit would break exactly the case this
     * test exercises. See idea-15/g12-fix-tool-guardrail-retry-escalation.md
     * for the corrected form (add {@code && fixedOutput == null} to the
     * {@code fix} branch of {@code escalate()} before adopting it).
     */
    @Test
    @Order(3)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void test_agent_fix_guardrail_substitutes_output() {
        String marker = "SECRET_MARKER_VALUE";
        GuardrailDef redactMarker = Guardrail.of("e2e_agent_fix_guard", content -> content.contains(marker)
                        ? GuardrailResult.fix(content.replace(marker, "[REDACTED]"))
                        : GuardrailResult.pass())
                .onFail(OnFail.FIX)
                .build();

        Agent agent = Agent.builder()
                .name("e2e_java_agent_fix_guard_agent")
                .model(MODEL)
                .instructions("Output only this exact text and nothing else: " + marker)
                .guardrails(List.of(redactMarker))
                .maxTurns(3)
                .build();

        AgentResult result = runtime.run(agent, "Go.");

        assertEquals(
                AgentStatus.COMPLETED,
                result.getStatus(),
                "Agent did not complete with a FIX guardrail attached. Status: " + result.getStatus()
                        + ". Error: " + result.getError());

        String output = String.valueOf(result.getOutput());
        assertTrue(
                output.contains("[REDACTED]") && !output.contains(marker),
                "Expected the FIX guardrail's substituted output in the final result. Got output: "
                        + output + ". If a future server-side change discards a valid "
                        + "fixed_output (see the class-level doc on this test), the marker "
                        + "survives uncensored or the run fails instead of completing with the "
                        + "substitution.");
    }

    // ── Gated tests: require the server-side retry-escalation fix (G12) ────
    //
    // These are skipped — visibly, not @Disabled — against any server that
    // predates the fix proposed in idea-15/g12-fix-tool-guardrail-retry-escalation.md.
    // Force them open locally against an already-fixed build with
    // GUARDRAIL_ESCALATION_FIXED=true. See ServerCapabilities.
    //
    // Both verified live against today's real (unfixed) server with the gate
    // forced open: both fail fast with "Got status: COMPLETED" — the exact
    // G12 symptom — confirming they correctly detect the bug once activated.

    /**
     * Tool-level RETRY guardrail that always fails must escalate to a FAILED/
     * TERMINATED run once {@code maxRetries} is exceeded, not retry forever.
     *
     * COUNTERFACTUAL (idea-15 gap G12): {@code GuardrailCompiler
     * .compileToolGuardrailTasks} hardcodes the retry-loop iteration counter
     * to the literal {@code "1"} instead of the live turn number, so a
     * tool-level {@code onFail=RETRY} guardrail can never see
     * {@code iteration >= maxRetries} — it retries every turn until
     * {@code maxTurns}, and the run COMPLETES instead of failing.
     */
    @Test
    @Order(4)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void test_tool_retry_guardrail_escalates_to_failure() {
        GuardrailDef alwaysFailRetry = Guardrail.of(
                        "e2e_tool_retry_guard", content -> GuardrailResult.fail("blocked for e2e retry test"))
                .onFail(OnFail.RETRY)
                .maxRetries(2)
                .build();

        ToolDef guardedTool = echoTool().withGuardrails(List.of(alwaysFailRetry));

        Agent agent = Agent.builder()
                .name("e2e_java_tool_retry_guard_agent")
                .model(MODEL)
                .instructions("You MUST call the echo tool with input='hello'. Then report its result.")
                .tools(List.of(guardedTool))
                .maxTurns(6)
                .build();

        AgentResult result = runtime.run(agent, "Please echo 'hello'.");

        assertTrue(
                result.getStatus() == AgentStatus.FAILED || result.getStatus() == AgentStatus.TERMINATED,
                "Expected agent to escalate to FAIL/TERMINATE once the tool guardrail's maxRetries=2 "
                        + "was exceeded. Got status: " + result.getStatus()
                        + ". COUNTERFACTUAL (idea-15 gap G12): if the server still hardcodes the "
                        + "tool-guardrail iteration to \"1\", the run retries until maxTurns and "
                        + "completes instead.");
    }

    /**
     * Regex tool guardrail with {@code onFail=RETRY} must also escalate after
     * {@code maxRetries} — same bug as above (G12), exercised through the
     * server-side regex script path with no SDK worker involved at all, to
     * confirm the fix (a compiler-level iteration ref) covers every
     * guardrail type, not just custom function guardrails.
     */
    @Test
    @Order(5)
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void test_regex_tool_guardrail_escalates_to_failure() {
        GuardrailDef blockMarker = RegexGuardrail.builder()
                .name("e2e_tool_regex_retry_guard")
                .patterns("BLOCKME")
                .onFail(OnFail.RETRY)
                .maxRetries(2)
                .build();

        ToolDef guardedTool = echoTool().withGuardrails(List.of(blockMarker));

        Agent agent = Agent.builder()
                .name("e2e_java_tool_regex_retry_agent")
                .model(MODEL)
                .instructions("You MUST call the echo tool with input='BLOCKME'. Then report its result.")
                .tools(List.of(guardedTool))
                .maxTurns(6)
                .build();

        AgentResult result = runtime.run(agent, "Please echo 'BLOCKME'.");

        assertTrue(
                result.getStatus() == AgentStatus.FAILED || result.getStatus() == AgentStatus.TERMINATED,
                "Expected agent to escalate to FAIL/TERMINATE once the regex tool guardrail's "
                        + "maxRetries=2 was exceeded. Got status: " + result.getStatus()
                        + ". COUNTERFACTUAL (idea-15 gap G12, script path): same hardcoded-iteration "
                        + "bug reached via the server-side regex script instead of an SDK worker.");
    }
}
