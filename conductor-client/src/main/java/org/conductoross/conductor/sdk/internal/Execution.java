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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A read-only view of an execution as the server returns it ({@code GET /workflow/{id}} with
 * tasks): its own grown definition, its tasks, its variables, and the continuation markers (the
 * bootstrap marker in {@code nativeState}, and one on each task the code paused on). Plain JSON
 * maps are used throughout because the definition carries fields (task metadata) that the client
 * model classes drop.
 */
public final class Execution {

    private static final Set<String> SUCCESS = Set.of("COMPLETED", "COMPLETED_WITH_ERRORS", "SKIPPED");
    private static final Set<String> TERMINAL =
            Set.of("COMPLETED", "COMPLETED_WITH_ERRORS", "SKIPPED", "FAILED", "FAILED_WITH_TERMINAL_ERROR", "TIMED_OUT", "CANCELED");

    /** Set on a failed task the server marked COMPLETED_WITH_ERRORS: how the task really ended. */
    private static final String FAILED_STATUS = "conductor.failedStatus";

    public final String workflowId;
    public final String status;
    public final Map<String, Object> input;
    public final Map<String, Object> output;
    public final Map<String, Object> variables;
    public final Map<String, Object> definition;
    /** The engine's native state: bootstrap and handler markers, and the handled messages. */
    public final Map<String, Object> nativeState;
    public final List<TaskView> tasks = new ArrayList<>();
    /** Every definition entry at any depth, by reference name. */
    public final Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
    /** Every value the code recorded ({@code once}, {@code now}, {@code random}), by name. */
    public final Map<String, Object> recorded = new HashMap<>();
    private final Map<String, TaskView> latest = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Execution(Map<String, Object> json) {
        this.workflowId = (String) json.get("workflowId");
        this.status = (String) json.get("status");
        this.input = orEmpty((Map<String, Object>) json.get("input"));
        this.output = orEmpty((Map<String, Object>) json.get("output"));
        this.variables = orEmpty((Map<String, Object>) json.get("variables"));
        this.definition = orEmpty((Map<String, Object>) json.get("workflowDefinition"));
        this.nativeState = orEmpty((Map<String, Object>) json.get("nativeState"));
        collect((List<Map<String, Object>>) definition.get("tasks"));
        List<Map<String, Object>> raw = (List<Map<String, Object>>) json.get("tasks");
        if (raw != null) {
            for (Map<String, Object> t : raw) {
                TaskView view = new TaskView(t);
                tasks.add(view);
                TaskView prev = latest.get(view.ref);
                if (prev == null || prev.seq < view.seq) {
                    latest.put(view.ref, view);
                }
            }
        }
        tasks.sort(Comparator.comparingInt(t -> t.seq));
        for (Marker m : segments()) {
            recorded.putAll(m.recorded);
        }
    }

    @SuppressWarnings("unchecked")
    private void collect(List<Map<String, Object>> list) {
        if (list == null) {
            return;
        }
        for (Map<String, Object> e : list) {
            entries.put((String) e.get("taskReferenceName"), e);
            List<List<Map<String, Object>>> forks = (List<List<Map<String, Object>>>) e.get("forkTasks");
            if (forks != null) {
                forks.forEach(this::collect);
            }
            Map<String, List<Map<String, Object>>> cases = (Map<String, List<Map<String, Object>>>) e.get("decisionCases");
            if (cases != null) {
                cases.values().forEach(this::collect);
            }
            collect((List<Map<String, Object>>) e.get("defaultCase"));
            collect((List<Map<String, Object>>) e.get("loopOver"));
        }
    }

    /** The latest attempt of the task with exactly this reference name. */
    public TaskView latest(String ref) {
        return latest.get(ref);
    }

    /**
     * A task as a poll handed it out, which a lane kept parked in memory waited for: its result is
     * what the lane reads when it continues.
     */
    public void arrived(TaskView task) {
        tasks.add(task);
        latest.put(task.ref, task);
    }

    /**
     * Drops the tasks and the recorded values, for an execution whose code stays parked in memory:
     * what its lanes read next arrives with their continuations ({@link #arrived}), and what they
     * already read is in their own state. The definition entries stay, for the names they took.
     */
    public void forgetHistory() {
        tasks.clear();
        latest.clear();
        recorded.clear();
    }

    /** The latest task whose reference name is {@code base} or {@code base__<iteration>}. */
    public TaskView latestByBase(String base) {
        TaskView best = null;
        for (TaskView t : tasks) {
            if (t.ref.equals(base) || t.ref.startsWith(base + "__")) {
                if (best == null || t.seq > best.seq) {
                    best = t;
                }
            }
        }
        return best;
    }

    /** The bootstrap marker: the first continuation of the workflow method. */
    @SuppressWarnings("unchecked")
    public Marker bootstrap() {
        Object b = nativeState.get("bootstrap");
        if (!(b instanceof Map)) {
            return null;
        }
        return new Marker((Map<String, Object>) b, "main", 1); // always main #1
    }

    /** Every recently handled message, by the sender's id, with its reply when it has one. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> replies() {
        Object r = nativeState.get("replies");
        return r instanceof Map ? (Map<String, Object>) r : Map.of();
    }

    /**
     * The continuations the server has applied, in the order it applied them: by their
     * {@code endTime}, which the server keeps increasing within a run; the recording a replay
     * follows. The handler lane is not part of it. For a task that was retried, the latest
     * attempt's marker counts.
     */
    public List<Marker> segments() {
        List<Marker> segments = new ArrayList<>();
        Marker bootstrap = bootstrap();
        if (bootstrap != null && bootstrap.applied()) {
            segments.add(bootstrap);
        }
        for (TaskView t : latest.values()) {
            if (t.marker != null && t.marker.applied()) {
                segments.add(t.marker);
            }
        }
        segments.sort(Comparator.comparingLong((Marker m) -> m.endTime));
        return segments;
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? new LinkedHashMap<>() : m;
    }

    /**
     * A continuation marker: which continuation it is (the lane of the code and its number, which
     * the entry the code paused on names), and what its completion recorded.
     */
    public static final class Marker {
        public final String lane;
        public final int n;
        public final boolean leased;
        /** When it applied; 0 until then. */
        public final long endTime;
        public final boolean done;
        public final Map<String, Object> resumedWith;
        public final Map<String, Object> recorded;

        @SuppressWarnings("unchecked")
        Marker(Map<String, Object> m, String lane, int n) {
            this.lane = lane;
            this.n = n;
            this.leased = Boolean.TRUE.equals(m.get("leased"));
            this.endTime = m.get("endTime") instanceof Number x ? x.longValue() : 0L;
            this.done = Boolean.TRUE.equals(m.get("done"));
            this.resumedWith = orEmpty((Map<String, Object>) m.get("resumedWith"));
            this.recorded = orEmpty((Map<String, Object>) m.get("recorded"));
        }

        public boolean applied() {
            return endTime > 0;
        }

        /** As the server would say: applied, leased, or not run yet. */
        public String state() {
            return applied() ? "applied" : leased ? "leased" : "not run";
        }
    }

    /** One task of the execution. */
    public static final class TaskView {
        public final String taskId;
        public final String ref;
        public final String taskType;
        public final String status;
        public final int seq;
        public final int iteration;
        public final Map<String, Object> input;
        public final Map<String, Object> output;
        public final String reason;
        public final String definitionType;
        /** The continuation marker, when the code paused on this task; else null. */
        public final Marker marker;

        @SuppressWarnings("unchecked")
        TaskView(Map<String, Object> t) {
            this.taskId = (String) t.get("taskId");
            this.ref = (String) t.get("referenceTaskName");
            this.taskType = (String) t.get("taskType");
            this.seq = t.get("seq") instanceof Number n ? n.intValue() : 0;
            this.iteration = t.get("iteration") instanceof Number n ? n.intValue() : 0;
            this.input = orEmpty((Map<String, Object>) t.get("inputData"));
            this.output = orEmpty((Map<String, Object>) t.get("outputData"));
            this.status = this.output.get(FAILED_STATUS) instanceof String failed
                    ? failed
                    : (String) t.get("status");
            this.reason = (String) t.get("reasonForIncompletion");
            Map<String, Object> wt = (Map<String, Object>) t.get("workflowTask");
            this.definitionType = wt == null ? null : (String) wt.get("type");
            Object c = t.get("continuation");
            Map<String, Object> meta = wt == null ? null : (Map<String, Object>) wt.get("metadata");
            this.marker = c instanceof Map && meta != null
                    ? new Marker(
                            (Map<String, Object>) c,
                            String.valueOf(meta.get("lane")),
                            meta.get("continuation") instanceof Number x ? x.intValue() : 0)
                    : null;
        }

        /** A task from the fields a poll hands out. */
        public static TaskView of(String taskId, String ref, String taskType, String status,
                                  Map<String, Object> output, String reason) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("taskId", taskId);
            t.put("referenceTaskName", ref);
            t.put("taskType", taskType);
            t.put("status", status);
            t.put("outputData", output);
            t.put("reasonForIncompletion", reason);
            return new TaskView(t);
        }

        public boolean terminal() {
            return TERMINAL.contains(status);
        }

        public boolean successful() {
            return SUCCESS.contains(status);
        }
    }
}
