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
package org.conductoross.conductor.ai.schedule;

import java.util.*;
import java.util.function.Supplier;

import org.conductoross.conductor.ai.model.AgentHandle;
import org.conductoross.conductor.ai.model.AgentResult;

import com.netflix.conductor.client.exception.ConductorClientException;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.exceptions.AgentAPIException;
import io.orkes.conductor.client.http.OrkesSchedulerClient;
import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.WorkflowSchedule;

/**
 * Lifecycle API for cron-based agent schedules. Obtained via {@code runtime.schedules()}.
 *
 * <p>This class contributes only agent-scoped naming (prefix/unprefix),
 * {@link Schedule}/{@link ScheduleInfo} DTO mapping, declarative
 * {@link #reconcile}, and {@code runNow} sugar (via the typed {@link WorkflowClient}).
 *
 * <p>Operations are keyed by the <strong>wire name</strong> (prefixed with
 * {@code agent-}) returned by {@link #list(String)}. Use {@link Schedule} to
 * construct the user-facing short name; the SDK prefixes it at deploy time.
 */
public class Schedules {

    private final SchedulerClient schedulerClient;
    /** Shared native Conductor client for starting workflows (runNow). */
    private final WorkflowClient workflowClient;

    public Schedules(ConductorClient conductorClient) {
        this.schedulerClient = new OrkesSchedulerClient(conductorClient);
        this.workflowClient = new WorkflowClient(conductorClient);
    }

    /** Test seam: inject a {@link WorkflowClient} so {@code runNow}/{@code runNowAndWait} can be unit-tested. */
    Schedules(ConductorClient conductorClient, WorkflowClient workflowClient) {
        this.schedulerClient = new OrkesSchedulerClient(conductorClient);
        this.workflowClient = workflowClient;
    }

    // ── CRUD ────────────────────────────────────────────────────────────

    public void save(Schedule schedule, String agentName) {
        SaveScheduleRequest request = toSaveRequest(schedule, agentName);
        translate(() -> {
            schedulerClient.saveSchedule(request);
            return null;
        });
    }

    public ScheduleInfo get(String wireName) {
        WorkflowSchedule ws = translate(() -> schedulerClient.getSchedule(wireName));
        return fromWorkflowSchedule(ws, null);
    }

    public List<ScheduleInfo> list(String agentName) {
        List<WorkflowSchedule> resp = translate(() -> schedulerClient.getAllSchedules(agentName));
        if (resp == null) return new ArrayList<>();
        return resp.stream()
                .filter(Objects::nonNull)
                .map(item -> fromWorkflowSchedule(item, agentName))
                .toList();
    }

    public void pause(String wireName) {
        pause(wireName, null);
    }

    public void pause(String wireName, String reason) {
        translate(() -> {
            schedulerClient.pauseSchedule(wireName, reason);
            return null;
        });
    }

    public void resume(String wireName) {
        translate(() -> {
            schedulerClient.resumeSchedule(wireName);
            return null;
        });
    }

    public void delete(String wireName) {
        translate(() -> {
            schedulerClient.deleteSchedule(wireName);
            return null;
        });
    }

    /**
     * Start the scheduled agent's workflow immediately via the official Conductor
     * {@link WorkflowClient#startWorkflow} (returns the new workflowId).
     */
    public String runNow(ScheduleInfo info) {
        StartWorkflowRequest req = new StartWorkflowRequest();
        req.setName(info.getAgent());
        if (info.getInput() != null) req.setInput(info.getInput());
        return workflowClient.startWorkflow(req);
    }

    /** Default timeout (ms) for {@link #runNowAndWait}, mirroring Python's 600s default. */
    private static final long DEFAULT_WAIT_TIMEOUT_MS = 600_000L;
    /** Default poll interval (ms) for {@link #runNowAndWait}, mirroring Python's 1s default. */
    private static final long DEFAULT_POLL_INTERVAL_MS = 1_000L;

    /**
     * Fetch the schedule by its wire {@code name} and start its agent's workflow
     * immediately with the schedule's stored input. Returns the new workflowId.
     *
     * <p>Name-keyed parity with the Python/TS {@code run_now(name)}.
     */
    public String runNow(String name) {
        return runNow(get(name));
    }

    /**
     * Fetch the schedule by its wire {@code name} and start its agent's workflow.
     *
     * <p>When {@code wait} is {@code false} (default behaviour) returns the
     * workflowId immediately. When {@code wait} is {@code true} this blocks until
     * the workflow reaches a terminal state and returns an {@link AgentResult}
     * built from the completed workflow (parity with Python's
     * {@code run_now(name, wait=True)} and the C#/TS SDKs, which return an
     * {@link AgentResult} from the wait variant).
     *
     * @return a {@link String} workflowId when {@code wait=false}, or an
     *     {@link AgentResult} when {@code wait=true}
     */
    public Object runNow(String name, boolean wait) {
        if (!wait) {
            return runNow(name);
        }
        return runNowAndWait(name);
    }

    /**
     * Fetch the schedule by its wire {@code name}, start it, then poll until the
     * triggered workflow reaches a terminal state and return it as an
     * {@link AgentResult}.
     *
     * @throws ScheduleException.Timeout if the workflow has not finished within the timeout
     */
    public AgentResult runNowAndWait(String name) {
        return runNowAndWait(name, DEFAULT_WAIT_TIMEOUT_MS, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * Fetch the schedule by its wire {@code name}, start it, then poll until the
     * triggered workflow reaches a terminal state and return it as an
     * {@link AgentResult}.
     *
     * <p>The completed {@link Workflow} is converted via the SDK's shared
     * workflow → {@link AgentResult} extraction ({@link AgentHandle#fromWorkflow})
     * — the same logic the {@code AgentHandle.waitForResult} path uses — so the
     * output, status, error, token usage, and tool calls match a direct run.
     *
     * @param name           the schedule's wire name
     * @param timeoutMs      maximum time to wait, in milliseconds
     * @param pollIntervalMs delay between status polls, in milliseconds
     * @throws ScheduleException.Timeout if the workflow has not finished within {@code timeoutMs}
     */
    public AgentResult runNowAndWait(String name, long timeoutMs, long pollIntervalMs) {
        String executionId = runNow(name);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            Workflow wf = workflowClient.getWorkflow(executionId, true);
            if (isTerminal(wf)) {
                return AgentHandle.fromWorkflow(wf);
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new ScheduleException.Timeout("runNow('" + name + "') did not finish within " + timeoutMs + "ms");
            }
            if (pollIntervalMs > 0) {
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ScheduleException.Timeout("runNow('" + name + "') was interrupted while waiting");
                }
            }
        }
    }

    /** {@code true} if the workflow has reached a terminal state (completed/failed/terminated/timed-out). */
    static boolean isTerminal(Workflow wf) {
        Workflow.WorkflowStatus status = wf != null ? wf.getStatus() : null;
        return status != null && status.isTerminal();
    }

    public List<Long> previewNext(String cron, int n) {
        List<Long> resp = translate(() -> schedulerClient.getNextFewSchedules(cron, null, null, n));
        return resp != null ? resp : new ArrayList<>();
    }

    // ── Declarative reconcile ───────────────────────────────────────────

    /**
     * Apply declarative scheduling semantics:
     * <ul>
     *   <li>{@code null} → no-op</li>
     *   <li>empty list → purge all schedules whose workflow == agent</li>
     *   <li>non-empty list → upsert listed, delete any other schedule for this agent</li>
     * </ul>
     */
    public void reconcile(String agentName, List<Schedule> desired) {
        if (desired == null) return;
        checkUniqueNames(desired);

        Map<String, String> existingWireByShort = new LinkedHashMap<>();
        for (ScheduleInfo info : list(agentName)) {
            existingWireByShort.put(info.getShortName(), info.getName());
        }
        Set<String> desiredShort = new HashSet<>();
        for (Schedule s : desired) desiredShort.add(s.getName());

        for (Map.Entry<String, String> entry : existingWireByShort.entrySet()) {
            if (!desiredShort.contains(entry.getKey())) {
                delete(entry.getValue());
            }
        }
        for (Schedule s : desired) {
            save(s, agentName);
        }
    }

    // ── Internals ───────────────────────────────────────────────────────

    static String prefix(String agentName, String shortName) {
        return agentName + "-" + shortName;
    }

    static String unprefix(String agentName, String wireName) {
        String p = agentName + "-";
        return wireName.startsWith(p) ? wireName.substring(p.length()) : wireName;
    }

    static void checkUniqueNames(List<Schedule> schedules) {
        Set<String> seen = new HashSet<>();
        for (Schedule s : schedules) {
            if (!seen.add(s.getName())) {
                throw new ScheduleException.NameConflict(
                        "Duplicate schedule name '" + s.getName() + "' — names must be unique per agent");
            }
        }
    }

    static SaveScheduleRequest toSaveRequest(Schedule s, String agentName) {
        StartWorkflowRequest startWorkflowRequest = new StartWorkflowRequest()
                .withName(agentName)
                .withInput(s.getInput() == null ? Map.of() : new LinkedHashMap<>(s.getInput()));
        return new SaveScheduleRequest()
                .name(prefix(agentName, s.getName()))
                .cronExpression(s.getCron())
                .zoneId(s.getTimezone())
                .runCatchupScheduleInstances(s.isCatchup())
                .paused(s.isPaused())
                .scheduleStartTime(s.getStartAt())
                .scheduleEndTime(s.getEndAt())
                .startWorkflowRequest(startWorkflowRequest)
                .description(s.getDescription());
    }

    static ScheduleInfo fromWorkflowSchedule(WorkflowSchedule ws, String agentHint) {
        StartWorkflowRequest swr = ws.getStartWorkflowRequest();
        String wireName = ws.getName() != null ? ws.getName() : "";
        String swrName = swr != null && swr.getName() != null ? swr.getName() : "";
        String agent = agentHint != null ? agentHint : (swrName.isEmpty() ? "" : swrName);

        return new ScheduleInfo(
                wireName,
                unprefix(agent, wireName),
                swrName,
                ws.getCronExpression() != null ? ws.getCronExpression() : "",
                ws.getZoneId() != null ? ws.getZoneId() : "UTC",
                swr != null && swr.getInput() != null ? swr.getInput() : new HashMap<>(),
                Boolean.TRUE.equals(ws.isPaused()),
                ws.getPausedReason(),
                Boolean.TRUE.equals(ws.isRunCatchupScheduleInstances()),
                ws.getScheduleStartTime(),
                ws.getScheduleEndTime(),
                ws.getDescription(),
                ws.getNextRunTime(),
                ws.getCreateTime(),
                ws.getUpdatedTime(),
                ws.getCreatedBy(),
                ws.getUpdatedBy());
    }

    /** Run a {@link SchedulerClient} call, translating transport errors to the scheduler's typed exceptions. */
    private static <T> T translate(Supplier<T> call) {
        try {
            return call.get();
        } catch (ConductorClientException e) {
            throw mapException(e);
        }
    }

    /** Map Conductor's exception to the scheduler's typed exceptions (preserves the contract). */
    private static RuntimeException mapException(ConductorClientException e) {
        int status = e.getStatus();
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (status == 404) return new ScheduleException.NotFound(msg);
        if (status == 400 && msg.toLowerCase().contains("cron")) {
            return new ScheduleException.InvalidCron(msg);
        }
        return new AgentAPIException(status, msg);
    }
}
