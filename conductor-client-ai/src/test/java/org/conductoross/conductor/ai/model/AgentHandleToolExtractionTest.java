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
package org.conductoross.conductor.ai.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.conductoross.conductor.ai.enums.EventType;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tool-call and event extraction on the non-streaming path, exercised through
 * {@link AgentHandle#fromWorkflow(Workflow)} — the seam {@code waitForResult()}
 * shares with it.
 *
 * <p>The fixtures reproduce what Conductor actually stores: an executed SIMPLE
 * task carries the task's own name as its {@code taskType}, every other tool
 * kind carries its system task type, the server tags every kind's input with
 * {@code _agent_tool_name}, and the reference name is seeded from the LLM
 * provider's tool-call id.
 */
class AgentHandleToolExtractionTest {

    private static Task task(String taskType, String refName, Map<String, Object> input, Map<String, Object> output) {
        Task t = new Task();
        t.setTaskType(taskType);
        t.setReferenceTaskName(refName);
        t.setInputData(input);
        t.setOutputData(output);
        t.setStatus(Task.Status.COMPLETED);
        return t;
    }

    /**
     * A worker tool as the server compiles it: the LLM's arguments sit at the top
     * level beside the runtime keys, and Conductor has rewritten {@code taskType}
     * to the task's own name by the time the task is executed.
     */
    private static Task workerToolTask(String refName, String toolName, Map<String, Object> args) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("_agent_tool_name", toolName);
        input.put("_agent_state", Map.of());
        input.put("method", toolName);
        input.putAll(args);
        return task(toolName, refName, input, Map.of("result", toolName + "-output"));
    }

    /**
     * A tool the server compiles to a system task. The LLM's arguments have been
     * folded into whatever that task type takes — an HTTP tool's into
     * {@code http_request} — and only {@code _agent_tool_name} still names the
     * tool.
     */
    private static Task systemToolTask(
            String taskType, String refName, String toolName, Map<String, Object> compiledInput) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("_agent_tool_name", toolName);
        input.putAll(compiledInput);
        return task(taskType, refName, input, Map.of("result", toolName + "-output"));
    }

    private static Task llmTask(int prompt, int completion) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("promptTokens", prompt);
        out.put("completionTokens", completion);
        out.put("tokenUsed", prompt + completion);
        return task("LLM_CHAT_COMPLETE", "chat_0", Map.of(), out);
    }

    private static Workflow workflow(Task... tasks) {
        Workflow wf = new Workflow();
        wf.setWorkflowId("exec-1");
        wf.setStatus(Workflow.WorkflowStatus.COMPLETED);
        wf.setOutput(Map.of("result", "done"));
        wf.setTasks(new ArrayList<>(List.of(tasks)));
        return wf;
    }

    private static List<String> names(AgentResult result) {
        return result.getToolCalls().stream()
                .map(tc -> (String) tc.get("name"))
                .collect(Collectors.toList());
    }

    /**
     * Every tool kind is reported under the tool's own name, not the system task
     * type it compiles to. COUNTERFACTUAL (pre-fix): the names came from
     * {@code getTaskType()}, so this read
     * {@code [get_weather, HTTP, CALL_MCP_TOOL, SUB_WORKFLOW, HUMAN, GENERATE_IMAGE]}.
     */
    @Test
    void namesEveryToolKindAfterTheToolNotItsTaskType() {
        AgentResult result = AgentHandle.fromWorkflow(workflow(
                llmTask(10, 5),
                workerToolTask("call_a_0__1", "get_weather", Map.of("city", "SF")),
                systemToolTask(
                        "HTTP",
                        "call_b_0__1",
                        "fetch_page",
                        Map.of("http_request", Map.of("uri", "https://example.com", "method", "GET"))),
                systemToolTask("CALL_MCP_TOOL", "call_c_0__1", "search_docs", Map.of("toolInput", Map.of("q", "e"))),
                systemToolTask("SUB_WORKFLOW", "call_d_0__1", "billing_agent", Map.of("request", "invoice?")),
                systemToolTask("HUMAN", "call_e_0__1", "ask_manager", Map.of("prompt", "approve?")),
                systemToolTask("GENERATE_IMAGE", "call_f_0__1", "draw_chart", Map.of("prompt", "a bar chart"))));

        assertEquals(
                List.of("get_weather", "fetch_page", "search_docs", "billing_agent", "ask_manager", "draw_chart"),
                names(result));
    }

    /**
     * Detection does not key on the provider's tool-call id format. COUNTERFACTUAL
     * (pre-fix): selection was {@code refName.startsWith("call_")}, so an
     * Anthropic-backed run — whose ids start {@code toolu_} — reported no tool
     * calls at all.
     */
    @Test
    void detectsToolCallsWhateverTheProviderIdFormat() {
        AgentResult anthropic = AgentHandle.fromWorkflow(workflow(
                llmTask(10, 5), workerToolTask("toolu_01ABCdef_0__1", "get_weather", Map.of("city", "SF"))));
        AgentResult uuidFallback = AgentHandle.fromWorkflow(workflow(
                llmTask(10, 5),
                workerToolTask("3f2b1c9e-0d4a-4c7b-9f11-2a6d8e5b7c30_0__1", "get_weather", Map.of())));

        assertEquals(List.of("get_weather"), names(anthropic));
        assertEquals(List.of("get_weather"), names(uuidFallback));
    }

    /** Internal runtime keys the server injects are not reported as tool arguments. */
    @Test
    @SuppressWarnings("unchecked")
    void stripsInternalKeysFromArgs() {
        AgentResult result = AgentHandle.fromWorkflow(
                workflow(workerToolTask("call_a_0__1", "get_weather", Map.of("city", "SF"))));

        Map<String, Object> args = (Map<String, Object>) result.getToolCalls().get(0).get("args");
        assertEquals(Map.of("city", "SF"), args);
    }

    /**
     * A tool whose output isn't wrapped in {@code result} reports the whole output
     * map, and its args are the compiled request — the server folds an HTTP tool's
     * LLM arguments into {@code http_request}, so that is all the workflow record
     * keeps of them.
     */
    @Test
    @SuppressWarnings("unchecked")
    void reportsUnwrappedOutputForSystemTaskTools() {
        Map<String, Object> httpRequest = Map.of("uri", "https://example.com?q=evals", "method", "GET");
        Map<String, Object> httpOutput = Map.of("response", Map.of("body", "hello"), "statusCode", 200);
        Task http = task(
                "HTTP",
                "call_b_0__1",
                Map.of("_agent_tool_name", "fetch_page", "http_request", httpRequest),
                httpOutput);

        AgentResult result = AgentHandle.fromWorkflow(workflow(http));

        Map<String, Object> call = result.getToolCalls().get(0);
        assertEquals("fetch_page", call.get("name"));
        assertEquals(Map.of("http_request", httpRequest), (Map<String, Object>) call.get("args"));
        assertEquals(httpOutput, call.get("result"));
    }

    /**
     * The last rung of the name resolution. Defensive rather than observed: the
     * server tags every tool it dispatches, so this only fires if that tag ever
     * arrives blank.
     */
    @Test
    void fallsBackToTheTaskDefNameWhenTheServerTagIsBlank() {
        Task human = task(
                "HUMAN", "call_e_0__1", Map.of("_agent_tool_name", ""), Map.of("result", "approved"));
        human.setTaskDefName("ask_manager");

        AgentResult result = AgentHandle.fromWorkflow(workflow(human));

        assertEquals(List.of("ask_manager"), names(result));
    }

    /** A tool that answers {@code null} keeps its null rather than reporting its own input. */
    @Test
    void keepsANullResult() {
        Map<String, Object> output = new java.util.HashMap<>();
        output.put("result", null);
        Task worker = task("get_weather", "call_a_0__1", Map.of("_agent_tool_name", "get_weather"), output);

        AgentResult result = AgentHandle.fromWorkflow(workflow(worker));

        assertEquals(1, result.getToolCalls().size());
        assertNull(result.getToolCalls().get(0).get("result"));
    }

    /**
     * The LLM call, control flow, the approval gate and guardrail workers share
     * task types with real tools, and none of them is a tool call.
     */
    @Test
    void ignoresNonToolTasks() {
        Task approvalGate = task(
                "HUMAN",
                "weather_agent_approval_human",
                Map.of("__humanTaskDefinition", Map.of("displayName", "Approve")),
                Map.of("approved", true));
        Task guardrail = task("toxicity_guardrail", "weather_agent_guardrail_0", Map.of(), Map.of("passed", true));
        Task frameworkWrapper = task("SIMPLE", "_fw_task", Map.of("_agent_tool_name", "get_weather"), Map.of());

        AgentResult result = AgentHandle.fromWorkflow(workflow(
                llmTask(10, 5),
                task("SWITCH", "tool_switch", Map.of(), Map.of()),
                task("INLINE", "enrich_tools", Map.of(), Map.of("result", Map.of())),
                task("JOIN", "tool_join", Map.of(), Map.of()),
                approvalGate,
                guardrail,
                frameworkWrapper));

        assertTrue(result.getToolCalls().isEmpty(), "expected no tool calls, got " + names(result));
    }

    /** A tool task that has neither finished nor produced output is not yet a call. */
    @Test
    void ignoresAToolTaskStillInFlight() {
        Task pendingHuman = new Task();
        pendingHuman.setTaskType("HUMAN");
        pendingHuman.setReferenceTaskName("call_e_0__1");
        pendingHuman.setInputData(Map.of("_agent_tool_name", "ask_manager"));
        pendingHuman.setOutputData(Map.of());
        pendingHuman.setStatus(Task.Status.IN_PROGRESS);

        AgentResult result = AgentHandle.fromWorkflow(workflow(pendingHuman));

        assertTrue(result.getToolCalls().isEmpty());
    }

    /** Token usage still aggregates across LLM tasks. */
    @Test
    void aggregatesTokenUsage() {
        AgentResult result = AgentHandle.fromWorkflow(workflow(llmTask(10, 5), llmTask(7, 3)));

        assertNotNull(result.getTokenUsage());
        assertEquals(17, result.getTokenUsage().getPromptTokens());
        assertEquals(8, result.getTokenUsage().getCompletionTokens());
        assertEquals(25, result.getTokenUsage().getTotalTokens());
    }

    /**
     * COUNTERFACTUAL (pre-fix): {@code events} was hard-coded to {@code null} on
     * this path and normalized to an empty list, so a run that called three tools
     * was indistinguishable from one that did nothing.
     */
    @Test
    void populatesEventsOnTheNonStreamingPath() {
        AgentResult result = AgentHandle.fromWorkflow(workflow(
                llmTask(10, 5),
                workerToolTask("call_a_0__1", "get_weather", Map.of("city", "SF")),
                systemToolTask("HTTP", "call_b_0__1", "fetch_page", Map.of("http_request", Map.of("uri", "u")))));

        List<AgentEvent> events = result.getEvents();
        assertEquals(
                List.of(
                        EventType.TOOL_CALL,
                        EventType.TOOL_RESULT,
                        EventType.TOOL_CALL,
                        EventType.TOOL_RESULT,
                        EventType.DONE),
                events.stream().map(AgentEvent::getType).collect(Collectors.toList()));
        assertEquals("get_weather", events.get(0).getToolName());
        assertEquals(Map.of("city", "SF"), events.get(0).getArgs());
        assertEquals("get_weather-output", events.get(1).getResult());
        assertEquals("fetch_page", events.get(2).getToolName());
        assertEquals("exec-1", events.get(0).getExecutionId());
        assertEquals(Map.of("result", "done"), events.get(4).getOutput());
    }

    /** A run with no tools still records that it ran, rather than an empty list. */
    @Test
    void alwaysRecordsATerminalEvent() {
        AgentResult completed = AgentHandle.fromWorkflow(workflow(llmTask(10, 5)));
        assertEquals(1, completed.getEvents().size());
        assertEquals(EventType.DONE, completed.getEvents().get(0).getType());

        Workflow failed = workflow(llmTask(10, 5));
        failed.setStatus(Workflow.WorkflowStatus.FAILED);
        failed.setReasonForIncompletion("model unavailable");

        AgentResult result = AgentHandle.fromWorkflow(failed);
        AgentEvent last = result.getEvents().get(result.getEvents().size() - 1);
        assertEquals(EventType.ERROR, last.getType());
        assertEquals("model unavailable", last.getContent());
    }

    /** Both paths report the same call under the same name. */
    @Test
    void agreesWithTheStreamingPathOnToolNames() {
        AgentResult polled = AgentHandle.fromWorkflow(
                workflow(workerToolTask("call_a_0__1", "get_weather", Map.of("city", "SF"))));

        // What the server emits on the stream for the same call: the tool's own
        // name, and the task input minus the keys AgentEvent strips.
        AgentEvent streamed = AgentEvent.fromMap(Map.of(
                "type", "tool_call",
                "toolName", "get_weather",
                "args",
                        Map.of(
                                "city", "SF",
                                "method", "get_weather",
                                "_agent_tool_name", "get_weather",
                                "_agent_state", Map.of()),
                "executionId", "exec-1"));

        assertEquals(streamed.getToolName(), polled.getToolCalls().get(0).get("name"));
        assertEquals(streamed.getArgs(), polled.getToolCalls().get(0).get("args"));
    }
}
