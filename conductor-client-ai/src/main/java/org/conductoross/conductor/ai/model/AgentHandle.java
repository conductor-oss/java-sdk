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
import java.util.Set;

import org.conductoross.conductor.ai.enums.AgentStatus;
import org.conductoross.conductor.ai.enums.EventType;
import org.conductoross.conductor.ai.exceptions.WorkerStallError;
import org.conductoross.conductor.ai.internal.ServerLivenessMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;
import io.orkes.conductor.client.model.agent.RespondBody;

/**
 * A handle to a running agent workflow.
 *
 * <p>Returned by {@link org.conductoross.conductor.ai.AgentRuntime#start(org.conductoross.conductor.ai.Agent, String)}.
 * Allows checking status, interacting with human-in-the-loop pauses, and controlling
 * execution — from any process, even after restarts.
 */
public class AgentHandle {
    private static final Logger logger = LoggerFactory.getLogger(AgentHandle.class);

    private static final long DEFAULT_POLL_INTERVAL_MS = 2000;
    private static final long DEFAULT_TIMEOUT_MS = 600_000; // 10 minutes

    private final String executionId;
    private final AgentClient agentClient;
    private final WorkflowClient workflowClient;
    /** Liveness watch for stateful runs (spec R11); {@code null} when not monitored. */
    private final ServerLivenessMonitor livenessMonitor;

    public AgentHandle(String executionId, AgentClient agentClient, WorkflowClient workflowClient) {
        this(executionId, agentClient, workflowClient, null);
    }

    /**
     * Internal — used by {@code AgentRuntime.startAsync} to attach a liveness
     * monitor to stateful runs. {@code livenessMonitor} may be {@code null}.
     */
    public AgentHandle(
            String executionId,
            AgentClient agentClient,
            WorkflowClient workflowClient,
            ServerLivenessMonitor livenessMonitor) {
        this.executionId = executionId;
        this.agentClient = agentClient;
        this.workflowClient = workflowClient;
        this.livenessMonitor = livenessMonitor;
    }

    public String getExecutionId() {
        return executionId;
    }

    /**
     * Poll the server until the agent completes and return the final result.
     *
     * @return the agent result
     * @throws RuntimeException if the agent fails or times out
     */
    public AgentResult waitForResult() {
        return waitForResult(DEFAULT_TIMEOUT_MS, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * Poll the server until the agent completes with explicit timeout.
     *
     * @param timeoutMs       maximum wait time in milliseconds
     * @param pollIntervalMs  polling interval in milliseconds
     * @return the agent result
     */
    // Consecutive poll errors before we escalate from DEBUG→WARN→ERROR logging.
    private static final int POLL_ERROR_WARN_AT = 3;

    private static final int POLL_ERROR_FAIL_AT = 10;

    @SuppressWarnings("unchecked")
    public AgentResult waitForResult(long timeoutMs, long pollIntervalMs) {
        try {
            return pollForResult(timeoutMs, pollIntervalMs);
        } finally {
            // Every exit (terminal result, stall, timeout, poll give-up,
            // interrupt) ends this wait — the monitor has nothing left to watch.
            if (livenessMonitor != null) {
                livenessMonitor.close();
            }
        }
    }

    private AgentResult pollForResult(long timeoutMs, long pollIntervalMs) {
        long startTime = System.currentTimeMillis();
        int consecutiveErrors = 0;
        Exception lastError = null;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Surface a worker stall immediately instead of burning the full
            // timeout on an execution nothing is polling (spec R11).
            String stalledTask = livenessMonitor != null ? livenessMonitor.stalledTask() : null;
            if (stalledTask != null) {
                throw new WorkerStallError(stalledTask, executionId);
            }
            try {
                AgentStatusResponse status = agentClient.getAgentStatus(executionId);
                consecutiveErrors = 0; // reset on success
                lastError = null;
                String workflowStatus = status.getStatus();

                if (workflowStatus == null) {
                    logger.debug("Waiting for agent {} — status unknown", executionId);
                } else if (isTerminalStatus(workflowStatus)) {
                    return buildResult(status, workflowStatus);
                } else {
                    logger.debug("Waiting for agent {} — status: {}", executionId, workflowStatus);
                }

                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for agent result", e);
            } catch (Exception e) {
                lastError = e;
                consecutiveErrors++;
                if (consecutiveErrors >= POLL_ERROR_FAIL_AT) {
                    // Too many consecutive failures — the server is unhealthy. Surface the error
                    // rather than silently timing out, which hides the root cause for 10 minutes.
                    throw new RuntimeException(
                            "Giving up polling agent " + executionId + " after " + consecutiveErrors
                                    + " consecutive errors (last: " + e.getMessage() + ")",
                            e);
                } else if (consecutiveErrors >= POLL_ERROR_WARN_AT) {
                    logger.warn(
                            "Repeated errors polling agent {} ({} consecutive): {}",
                            executionId,
                            consecutiveErrors,
                            e.getMessage());
                } else {
                    logger.debug("Error polling agent status (attempt {}): {}", consecutiveErrors, e.getMessage());
                }
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for agent result", ie);
                }
            }
        }

        String lastErrorMsg = lastError != null ? " (last poll error: " + lastError.getMessage() + ")" : "";
        throw new RuntimeException("Agent timed out after " + timeoutMs + "ms: " + executionId + lastErrorMsg);
    }

    /** Approve a pending tool call that requires human approval. */
    public void approve() {
        agentClient.respond(executionId, RespondBody.approve());
    }

    /**
     * Approve with a human-readable comment.
     *
     * @param comment optional comment sent alongside the approval
     */
    public void approve(String comment) {
        agentClient.respond(executionId, RespondBody.approve(comment));
    }

    /**
     * Reject a pending tool call with an optional reason.
     *
     * @param reason rejection reason
     */
    public void reject(String reason) {
        agentClient.respond(executionId, RespondBody.reject(reason));
    }

    /**
     * Send an arbitrary structured response to a waiting workflow.
     *
     * <p>Use this for MANUAL agent selection:
     * <pre>{@code handle.respond(Map.of("selected", "writer")); }</pre>
     *
     * @param data the response payload
     */
    public void respond(Map<String, Object> data) {
        agentClient.respond(executionId, RespondBody.of(data));
    }

    /**
     * Send a message to a waiting agent (multi-turn conversation).
     *
     * <p>Delivered as {@code {"message": ...}} via
     * {@code POST /api/agent/{id}/respond} — the same wire shape as
     * {@link org.conductoross.conductor.ai.model.AgentStream#send(String)} and
     * the Python SDK's {@code handle.send()}.
     *
     * @param message the message to send to the waiting execution
     */
    public void send(String message) {
        agentClient.respond(executionId, RespondBody.of(Map.of("message", message)));
    }

    /**
     * Gracefully stop the agent execution.
     *
     * <p>The agent loop exits after the current iteration completes; the
     * execution reaches {@code COMPLETED} status with the last LLM output
     * preserved. Deterministic — does not depend on the LLM following stop
     * instructions. For immediate termination ({@code TERMINATED} status) use
     * {@link #cancel(String)}.
     *
     * <p>Also best-effort signals the execution to unblock a blocking
     * message-wait; failures are swallowed (the agent may not be waiting).
     */
    public void stop() {
        agentClient.stopAgent(executionId);
        try {
            agentClient.signalAgent(executionId, "");
        } catch (Exception e) {
            logger.debug("Best-effort stop unblock failed for {}: {}", executionId, e.getMessage());
        }
    }

    /** Pause the agent workflow (standard Conductor pause). */
    public void pause() {
        workflowClient.pauseWorkflow(executionId);
    }

    /**
     * Resume a <b>paused</b> agent workflow (un-pause).
     *
     * <p>Not to be confused with
     * {@code AgentRuntime.resume(executionId, agent)}, which re-attaches this
     * process's workers to an existing execution.
     */
    public void resume() {
        workflowClient.resumeWorkflow(executionId);
    }

    /**
     * Cancel the agent workflow immediately ({@code TERMINATED} status).
     *
     * @param reason the termination reason recorded on the workflow
     */
    public void cancel(String reason) {
        workflowClient.terminateWorkflow(executionId, reason);
    }

    /**
     * Fetch the current status snapshot of the agent execution
     * ({@code GET /api/agent/{id}/status}).
     */
    public AgentStatusResponse getStatus() {
        return agentClient.getAgentStatus(executionId);
    }

    /**
     * Check whether the workflow is currently paused waiting for human input.
     *
     * @return true if the server reports isWaiting == true
     */
    public boolean isWaiting() {
        try {
            AgentStatusResponse status = agentClient.getAgentStatus(executionId);
            return status.isWaiting();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Poll until the workflow is waiting for human input or reaches a terminal state.
     *
     * @param timeoutMs maximum wait time in milliseconds
     * @return true if the workflow is now waiting, false if it completed/failed first
     */
    public boolean waitUntilWaiting(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                AgentStatusResponse status = agentClient.getAgentStatus(executionId);
                if (status.isWaiting()) return true;
                if (status.getStatus() != null && isTerminalStatus(status.getStatus())) return false;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status)
                || "FAILED".equals(status)
                || "TERMINATED".equals(status)
                || "TIMED_OUT".equals(status);
    }

    @SuppressWarnings("unchecked")
    private AgentResult buildResult(AgentStatusResponse statusResponse, String workflowStatus) {
        Object output = statusResponse.getOutput();

        AgentStatus status;
        try {
            status = AgentStatus.valueOf(workflowStatus);
        } catch (IllegalArgumentException e) {
            status = AgentStatus.FAILED;
        }

        String error = null;
        if (status != AgentStatus.COMPLETED) {
            error = statusResponse.getReasonForIncompletion();
        }

        // Normalize output to a map
        if (output == null) {
            output = java.util.Collections.singletonMap("result", (Object) null);
        } else if (!(output instanceof Map)) {
            output = java.util.Collections.singletonMap("result", output);
        }

        // Token usage, tool calls and events: the server aggregates none of them
        // on the workflow status response, but every LLM_CHAT_COMPLETE task
        // carries tokenUsed/promptTokens/completionTokens in its outputData and
        // every tool task in the workflow is one LLM tool call. Walk the
        // workflow tasks once and aggregate all three.
        // WorkflowClient is the standard Conductor client for /api/workflow/* —
        // no need to go through AgentClient for this standard endpoint.
        TaskExtract extract = new TaskExtract();
        try {
            extract = extractFromTasks(workflowClient.getWorkflow(executionId, true));
        } catch (Exception e) {
            logger.debug("Could not extract tokens/toolCalls for {}: {}", executionId, e.getMessage());
        }

        return toResult(extract, output, executionId, status, error);
    }

    /**
     * Assemble the {@link AgentResult} both non-streaming paths return: what the
     * task walk found, closed with the terminal event.
     */
    private static AgentResult toResult(
            TaskExtract extract, Object output, String executionId, AgentStatus status, String error) {
        extract.events.add(terminalEvent(executionId, status, output, error));
        return new AgentResult(
                output, executionId, status, extract.toolCalls, extract.events, extract.tokenUsage, error);
    }

    /** Bundles the token usage, tool calls and events walked out of a workflow's tasks. */
    private static final class TaskExtract {
        TokenUsage tokenUsage;
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        List<AgentEvent> events = new ArrayList<>();
    }

    /**
     * Conductor task types the server compiles agent tools to — the values of the
     * server's {@code ToolCompiler.TYPE_MAP}, plus {@code GENERATE_PDF}, which
     * that map leaves to the upper-cased tool type.
     *
     * <p>A tool task is never recognised by its reference name. The server seeds
     * that from the provider's own tool-call id, so only OpenAI's happens to
     * start {@code call_} — an Anthropic-backed agent records {@code toolu_}, and
     * the next provider picks its own format again.
     */
    private static final Set<String> TOOL_TASK_TYPES = Set.of(
            "SIMPLE",
            "HTTP",
            "CALL_MCP_TOOL",
            "SUB_WORKFLOW",
            "HUMAN",
            "GENERATE_IMAGE",
            "GENERATE_AUDIO",
            "GENERATE_VIDEO",
            "GENERATE_PDF",
            "LLM_INDEX_TEXT",
            "LLM_SEARCH_INDEX",
            "PULL_WORKFLOW_MESSAGES");

    /** The one input key the server sets on every tool kind it dispatches. */
    private static final String TOOL_NAME_KEY = "_agent_tool_name";

    /** The tool name a server-compiled tool task carries, predating {@link #TOOL_NAME_KEY}. */
    private static final String TOOL_METHOD_KEY = "method";

    /**
     * Walk a workflow's tasks once and aggregate token usage (from
     * {@code LLM_CHAT_COMPLETE} tasks) plus the tool calls and their
     * {@code tool_call}/{@code tool_result} events (from tool tasks). Shared by
     * both {@link #buildResult} and {@link #fromWorkflow(Workflow)} so the
     * extraction lives in one place.
     */
    private static TaskExtract extractFromTasks(Workflow workflow) {
        TaskExtract out = new TaskExtract();
        List<Task> tasks = workflow != null && workflow.getTasks() != null ? workflow.getTasks() : List.of();
        String executionId = workflow != null && workflow.getWorkflowId() != null ? workflow.getWorkflowId() : "";
        int promptT = 0, completionT = 0, totalT = 0;
        boolean sawTokens = false;
        for (Task task : tasks) {
            Map<String, Object> outputData = task.getOutputData();

            // LLM task — aggregate tokens
            if ("LLM_CHAT_COMPLETE".equals(task.getTaskType()) && outputData != null) {
                promptT += toInt(outputData.get("promptTokens"));
                completionT += toInt(outputData.get("completionTokens"));
                totalT += toInt(outputData.get("tokenUsed"));
                sawTokens = true;
                continue;
            }

            if (!isToolTask(task)) continue;
            // A tool task that has neither finished nor produced anything is a
            // call the agent has not made yet — a HUMAN tool still waiting on
            // its assignee, say. Reporting it would claim a call that has not
            // happened.
            boolean produced = outputData != null && !outputData.isEmpty();
            if (!produced && (task.getStatus() == null || !task.getStatus().isTerminal())) continue;

            String name = resolveToolName(task);
            Map<String, Object> args = toolArgs(task.getInputData());
            // A tool whose output isn't wrapped in "result" — HTTP, MCP — reports
            // the whole output map, as the server's own event listener does. Keyed
            // on the key being there, so a tool that answers null keeps its null.
            Object result = null;
            if (produced) {
                result = outputData.containsKey("result") ? outputData.get("result") : outputData;
            }

            Map<String, Object> tc = new LinkedHashMap<>();
            tc.put("name", name);
            if (args != null) tc.put("args", args);
            tc.put("result", result);
            out.toolCalls.add(tc);

            out.events.add(
                    new AgentEvent(EventType.TOOL_CALL, null, name, args, null, null, executionId, null, null));
            out.events.add(
                    new AgentEvent(EventType.TOOL_RESULT, null, name, null, result, null, executionId, null, null));
        }
        if (sawTokens) {
            out.tokenUsage = new TokenUsage(promptT, completionT, totalT);
        }
        return out;
    }

    /**
     * Whether a task is one of the agent's tool invocations, as opposed to the
     * LLM call, a control-flow task, a guardrail or the approval gate.
     */
    private static boolean isToolTask(Task task) {
        // Framework passthrough wrappers restate a tool task that is already
        // in the list on its own.
        String refName = task.getReferenceTaskName();
        if (refName != null && refName.startsWith("_fw_")) return false;

        Map<String, Object> inputData = task.getInputData();
        if (inputData != null && inputData.get(TOOL_NAME_KEY) != null) return true;

        if (!TOOL_TASK_TYPES.contains(task.getTaskType())) return false;
        // Typed like a tool but untagged, so it only counts if it names one.
        // That keeps out the approval gate's HUMAN task and guardrail workers,
        // which share their task types with real tools.
        return inputData != null && inputData.get(TOOL_METHOD_KEY) != null;
    }

    /**
     * The tool's own name, which the server puts in the task's input. Never the
     * task type: Conductor overwrites an executed SIMPLE task's type with the
     * task's own name, so that reads correctly for a worker tool and reports
     * every other kind under its system task type — an HTTP tool as
     * {@code "HTTP"}, an MCP tool as {@code "CALL_MCP_TOOL"}.
     */
    private static String resolveToolName(Task task) {
        Map<String, Object> inputData = task.getInputData();
        if (inputData != null) {
            Object toolName = inputData.get(TOOL_NAME_KEY);
            if (toolName != null && !toolName.toString().isEmpty()) return toolName.toString();
            Object method = inputData.get(TOOL_METHOD_KEY);
            if (method != null && !method.toString().isEmpty()) return method.toString();
        }
        // Last resort, and only reachable when the server set one of the keys
        // above to an empty string: getTaskDefName() itself falls back to the
        // task type, so it is right for a worker tool and no better than the
        // old behaviour for anything else.
        String taskDefName = task.getTaskDefName();
        return taskDefName != null && !taskDefName.isEmpty() ? taskDefName : null;
    }

    /** A tool task's input with the server's internal runtime keys stripped. */
    private static Map<String, Object> toolArgs(Map<String, Object> inputData) {
        if (inputData == null) return null;
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : inputData.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("_")
                    || TOOL_METHOD_KEY.equals(k)
                    || "evaluatorType".equals(k)
                    || "expression".equals(k)
                    || "ctx".equals(k)
                    || "workerTag".equals(k)
                    || "agentConfig".equals(k)) continue;
            cleaned.put(k, e.getValue());
        }
        return cleaned;
    }

    /**
     * The event the stream would have ended on. Both non-streaming paths append
     * one so that an events list is never empty for a run that happened —
     * without it, "no events were collected" and "no events occurred" are the
     * same empty list.
     */
    private static AgentEvent terminalEvent(String executionId, AgentStatus status, Object output, String error) {
        String id = executionId != null ? executionId : "";
        if (status == AgentStatus.COMPLETED) {
            return new AgentEvent(EventType.DONE, null, null, null, null, output, id, null, null);
        }
        return new AgentEvent(EventType.ERROR, error, null, null, null, output, id, null, null);
    }

    /**
     * Build an {@link AgentResult} from a terminal {@link Workflow}.
     *
     * <p>Shared workflow → {@link AgentResult} extraction used by callers that
     * already hold a completed {@link Workflow}. Maps the workflow status to an {@link AgentStatus},
     * normalizes the output map, surfaces {@code reasonForIncompletion} as the
     * error for non-completed runs, and reuses {@link #extractFromTasks} for the
     * token-usage, tool-call and event aggregation.
     *
     * @param workflow a finished (or at least populated) workflow; may be null
     * @return the equivalent {@link AgentResult}
     */
    public static AgentResult fromWorkflow(Workflow workflow) {
        String executionId = workflow != null ? workflow.getWorkflowId() : null;

        Workflow.WorkflowStatus wfStatus = workflow != null ? workflow.getStatus() : null;
        AgentStatus status;
        try {
            status = wfStatus != null ? AgentStatus.valueOf(wfStatus.name()) : AgentStatus.FAILED;
        } catch (IllegalArgumentException e) {
            status = AgentStatus.FAILED;
        }

        String error = null;
        if (status != AgentStatus.COMPLETED && workflow != null) {
            error = workflow.getReasonForIncompletion();
        }

        Object output = workflow != null ? workflow.getOutput() : null;
        if (output == null) {
            output = java.util.Collections.singletonMap("result", (Object) null);
        } else if (!(output instanceof Map)) {
            output = java.util.Collections.singletonMap("result", output);
        }

        return toResult(extractFromTasks(workflow), output, executionId, status, error);
    }

    private static int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "AgentHandle{executionId=" + executionId + "}";
    }
}
