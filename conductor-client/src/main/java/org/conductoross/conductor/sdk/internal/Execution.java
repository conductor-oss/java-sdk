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
    /** The engine's native state: bootstrap and handler markers, replies, seen message ids. */
    public final Map<String, Object> nativeState;
    public final List<TaskView> tasks = new ArrayList<>();
    /** Every definition entry at any depth, by reference name. */
    public final Map<String, Map<String, Object>> entries = new java.util.LinkedHashMap<>();
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
        return b instanceof Map ? new Marker((Map<String, Object>) b) : null;
    }

    /** The replies to updates and queries, by message id. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> replies() {
        Object r = nativeState.get("replies");
        return r instanceof Map ? (Map<String, Object>) r : Map.of();
    }

    /** The ids of recently handled messages. */
    public List<String> seen() {
        List<String> ids = new ArrayList<>();
        if (nativeState.get("seen") instanceof List<?> l) {
            l.forEach(x -> ids.add(String.valueOf(x)));
        }
        return ids;
    }

    /**
     * The continuations the server has applied, in the order it applied them (their {@code step}):
     * the recording a replay follows. The handler lane is not part of it. For a task that was
     * retried, the latest attempt's marker counts.
     */
    public List<Marker> segments() {
        List<Marker> segments = new ArrayList<>();
        Marker bootstrap = bootstrap();
        if (bootstrap != null && bootstrap.applied()) {
            segments.add(bootstrap);
        }
        for (TaskView t : latest.values()) {
            if (t.marker != null && t.marker.applied() && !"handlers".equals(t.marker.lane)) {
                segments.add(t.marker);
            }
        }
        segments.sort(Comparator.comparingLong((Marker m) -> m.step).thenComparingInt(m -> m.n));
        return segments;
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? new LinkedHashMap<>() : m;
    }

    /** A continuation marker: where the code paused, and what its completion recorded. */
    public static final class Marker {
        public final String lane;
        public final int n;
        public final String state;
        public final long step;
        public final boolean done;
        public final Map<String, Object> resumedWith;
        public final Map<String, Object> recorded;

        @SuppressWarnings("unchecked")
        Marker(Map<String, Object> m) {
            this.lane = String.valueOf(m.get("lane"));
            this.n = m.get("n") instanceof Number x ? x.intValue() : 0;
            this.state = String.valueOf(m.get("state"));
            this.step = m.get("step") instanceof Number x ? x.longValue() : 0L;
            this.done = Boolean.TRUE.equals(m.get("done"));
            this.resumedWith = orEmpty((Map<String, Object>) m.get("resumedWith"));
            this.recorded = orEmpty((Map<String, Object>) m.get("recorded"));
        }

        public boolean applied() {
            return "APPLIED".equals(state);
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
            this.marker = c instanceof Map ? new Marker((Map<String, Object>) c) : null;
        }

        public boolean terminal() {
            return TERMINAL.contains(status);
        }

        public boolean successful() {
            return SUCCESS.contains(status);
        }
    }
}
