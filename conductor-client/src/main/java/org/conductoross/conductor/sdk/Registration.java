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
package org.conductoross.conductor.sdk;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.internal.WorkflowSpec;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/** Registers skeleton workflow definitions and task definitions from annotations. */
final class Registration {

    /** The owner of task definitions this SDK created; any other owner's are left alone. */
    private static final String NATIVE_OWNER = "native@conductor.local";

    private Registration() {}

    static Map<String, Method> workerMethods(Class<?> type) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                WorkerTask wt = m.getAnnotation(WorkerTask.class);
                if (wt != null && !methods.containsKey(wt.value())) {
                    m.setAccessible(true);
                    methods.put(wt.value(), m);
                }
            }
        }
        return methods;
    }

    static void register(ConductorClient client, Collection<WorkflowSpec> specs, List<Object> taskObjects,
            long continuationTimeoutSeconds) {
        register(client, specs, taskObjects, continuationTimeoutSeconds, true);
    }

    static void register(ConductorClient client, Collection<WorkflowSpec> specs, List<Object> taskObjects,
            long continuationTimeoutSeconds, boolean discover) {
        List<Map<String, Object>> taskDefs = new ArrayList<>();
        for (Object o : taskObjects) {
            for (Map.Entry<String, Method> e : workerMethods(o.getClass()).entrySet()) {
                WorkerTask wt = e.getValue().getAnnotation(WorkerTask.class);
                Map<String, Object> def = new LinkedHashMap<>();
                def.put("name", e.getKey());
                // Conductor's default of three retries, with exponential backoff from one second
                def.put("retryCount", wt.retryCount() >= 0 ? wt.retryCount() : 3);
                def.put("retryLogic", "EXPONENTIAL_BACKOFF");
                def.put("retryDelaySeconds", wt.retryDelaySeconds() >= 0 ? wt.retryDelaySeconds() : 1);
                def.put("timeoutSeconds", 0);
                def.put("responseTimeoutSeconds", wt.responseTimeoutSeconds() > 0 ? wt.responseTimeoutSeconds() : 120);
                // a worker that dies holding a task: the response timeout reschedules it
                def.put("timeoutPolicy", "RETRY");
                def.put("ownerEmail", NATIVE_OWNER);
                taskDefs.add(def);
            }
        }
        if (!taskDefs.isEmpty()) {
            // a task another team already defined keeps its definition: its retries and
            // timeouts are theirs, and this process just becomes one more worker of it
            Map<String, Object> owners = new LinkedHashMap<>();
            for (Map<String, Object> existing : Http.getList(client, "/metadata/taskdefs")) {
                owners.put(String.valueOf(existing.get("name")), existing.get("ownerEmail"));
            }
            taskDefs.removeIf(d -> owners.containsKey(d.get("name")) && !NATIVE_OWNER.equals(owners.get(d.get("name"))));
        }
        List<Map<String, Object>> workflowDefs = new ArrayList<>();
        for (WorkflowSpec spec : specs) {
            workflowDefs.add(skeleton(spec, continuationTimeoutSeconds, discover));
        }
        if (!taskDefs.isEmpty()) {
            Http.post(client, "/metadata/taskdefs", taskDefs, String.class);
        }
        if (!workflowDefs.isEmpty()) {
            Http.put(client, "/metadata/workflow", workflowDefs);
        }
        registerSchedules(client, specs);
    }

    /** Creates or updates a scheduler entry for each {@code @Schedule} on a workflow method. */
    @SuppressWarnings("unchecked")
    static void registerSchedules(ConductorClient client, Collection<WorkflowSpec> specs) {
        for (WorkflowSpec spec : specs) {
            int n = 0;
            for (org.conductoross.conductor.sdk.Schedule s : spec.schedules) {
                n++;
                String name = !s.name().isBlank() ? s.name() : spec.name + "_schedule" + (n > 1 ? "_" + n : "");
                Map<String, Object> start = new LinkedHashMap<>();
                start.put("name", spec.name);
                start.put("version", spec.version);
                try {
                    start.put("input", org.conductoross.conductor.sdk.internal.Json.MAPPER.readValue(s.input(), Map.class));
                } catch (Exception e) {
                    throw new IllegalArgumentException("@Schedule input of " + spec.name + " is not a JSON object: " + s.input(), e);
                }
                Map<String, Object> schedule = new LinkedHashMap<>();
                schedule.put("name", name);
                schedule.put("cronExpression", s.cron());
                schedule.put("zoneId", s.zone());
                schedule.put("paused", s.paused());
                schedule.put("description", s.description().isBlank() ? "Schedule of native workflow " + spec.name : s.description());
                schedule.put("startWorkflowRequest", start);
                try {
                    Http.post(client, "/scheduler/schedules", schedule, Map.class);
                } catch (RuntimeException e) {
                    org.slf4j.LoggerFactory.getLogger(Registration.class)
                            .warn("Could not register schedule {} (is conductor.scheduler.enabled=true?): {}", name, e.toString());
                }
            }
        }
    }

    /**
     * The registered definition: no tasks, and the native metadata the engine needs to run the
     * first continuation of the workflow method (and the handler lane, when the class has
     * handlers). The execution's own copy grows from here, one fragment per continuation.
     */
    static Map<String, Object> skeleton(WorkflowSpec spec, long continuationTimeoutSeconds) {
        return skeleton(spec, continuationTimeoutSeconds, true);
    }

    static Map<String, Object> skeleton(WorkflowSpec spec, long continuationTimeoutSeconds, boolean discover) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("name", spec.name);
        def.put("version", spec.version);
        def.put("description", spec.description.isBlank() ? "Native workflow " + spec.type.getSimpleName() + "." + spec.method.getName() : spec.description);
        def.put("schemaVersion", 2);
        def.put("ownerEmail", spec.ownerEmail);
        if (spec.timeoutSeconds > 0) {
            def.put("timeoutSeconds", spec.timeoutSeconds);
            def.put("timeoutPolicy", "TIME_OUT_WF");
        }
        if (!spec.failureWorkflow.isBlank()) {
            def.put("failureWorkflow", spec.failureWorkflow);
            if (spec.failureWorkflowVersion > 0) {
                def.put("failureWorkflowVersion", spec.failureWorkflowVersion);
            }
        }
        Map<String, Object> nativeMeta = new LinkedHashMap<>();
        nativeMeta.put("sdk", "java");
        nativeMeta.put("class", spec.type.getName());
        nativeMeta.put("method", spec.method.getName());
        nativeMeta.put("codeType", spec.codeTaskName());
        // how long the server waits for a continuation it handed to a worker before handing it
        // to another (the lease)
        nativeMeta.put("continuationTimeoutSeconds", continuationTimeoutSeconds);
        nativeMeta.put("handlers", spec.hasHandlers());
        nativeMeta.put("signals", new ArrayList<>(spec.signals.keySet()));
        nativeMeta.put("updates", new ArrayList<>(spec.updates.keySet()));
        nativeMeta.put("queries", new ArrayList<>(spec.queries.keySet()));
        // The graph discovered from the code, for the definition view and inventory. An
        // execution's own copy of the definition starts empty and grows with what the code calls.
        List<Map<String, Object>> discovered;
        try {
            discovered = discover ? org.conductoross.conductor.sdk.internal.Replayer.discover(spec) : List.of();
            nativeMeta.put("discovered", discover);
        } catch (RuntimeException e) {
            discovered = List.of();
            nativeMeta.put("discovered", false);
            nativeMeta.put("discoveryError", e.getMessage());
            org.slf4j.LoggerFactory.getLogger(Registration.class)
                    .warn("Could not discover the graph of workflow {}: {}", spec.name, e.getMessage());
        }
        inlineTaskDefinitions(discovered);
        def.put("metadata", Map.of("native", nativeMeta));
        def.put("inputParameters", inputParameters(spec));
        def.put("tasks", discovered);
        return def;
    }

    /** The workflow method's parameter names (a single record's components), for the definition. */
    static List<String> inputParameters(WorkflowSpec spec) {
        java.lang.reflect.Method m = spec.method;
        if (m.getParameterCount() == 0) {
            return List.of();
        }
        if (m.getParameterCount() == 1 && !org.conductoross.conductor.sdk.internal.Json.isWrapped(m.getGenericParameterTypes()[0])) {
            Class<?> type = m.getParameterTypes()[0];
            if (type.isRecord()) {
                List<String> names = new ArrayList<>();
                for (var c : type.getRecordComponents()) {
                    names.add(c.getName());
                }
                return names;
            }
            return List.of();
        }
        return List.of(org.conductoross.conductor.sdk.internal.MethodRefs.parameterNames(m));
    }

    /** Discovered SIMPLE tasks carry an inline definition, so registration validates without one. */
    @SuppressWarnings("unchecked")
    private static void inlineTaskDefinitions(List<Map<String, Object>> entries) {
        for (Map<String, Object> e : entries) {
            if ("SIMPLE".equals(e.get("type")) && !e.containsKey("taskDefinition")) {
                Map<String, Object> td = new LinkedHashMap<>();
                td.put("name", e.get("name"));
                td.put("retryCount", 3);
                td.put("timeoutSeconds", 0);
                td.put("responseTimeoutSeconds", 120);
                td.put("timeoutPolicy", "RETRY");
                td.put("ownerEmail", "native@conductor.local");
                e.put("taskDefinition", td);
            }
            if (e.get("forkTasks") instanceof List<?> branches) {
                for (Object b : branches) {
                    inlineTaskDefinitions((List<Map<String, Object>>) b);
                }
            }
            if (e.get("decisionCases") instanceof Map<?, ?> dc) {
                for (Object v : dc.values()) {
                    inlineTaskDefinitions((List<Map<String, Object>>) v);
                }
            }
            if (e.get("defaultCase") instanceof List<?> d) {
                inlineTaskDefinitions((List<Map<String, Object>>) d);
            }
            if (e.get("loopOver") instanceof List<?> l) {
                inlineTaskDefinitions((List<Map<String, Object>>) l);
            }
        }
    }
}
