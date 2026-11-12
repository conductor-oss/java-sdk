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
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.conductoross.conductor.sdk.Functions.Fn0;
import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.Functions.Proc0;
import org.conductoross.conductor.sdk.Functions.Proc1;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

/**
 * A running or finished execution, seen from the client. Handlers are addressed by method
 * reference, so renaming a handler is a compile error at every call site.
 */
public final class WorkflowHandle<R> {

    private static final Set<String> TERMINAL = Set.of("COMPLETED", "FAILED", "TERMINATED", "TIMED_OUT");

    private final Conductor conductor;
    private final String workflowId;
    private final Type resultType;

    WorkflowHandle(Conductor conductor, String workflowId, Type resultType) {
        this.conductor = conductor;
        this.workflowId = workflowId;
        this.resultType = resultType;
    }

    public String workflowId() {
        return workflowId;
    }

    /** The execution in the Conductor UI. */
    public String url() {
        String base = conductor.client().getBasePath().replaceAll("/api/?$", "");
        return base + "/execution/" + workflowId;
    }

    public <T, A> void signal(Proc1<T, A> handler, A argument) {
        send("signal", name(handler, Signal.class), argument);
    }

    public <T> void signal(Proc0<T> handler) {
        send("signal", name(handler, Signal.class), null);
    }

    /** Sends an update and waits for the handler's return value. */
    @SuppressWarnings("unchecked")
    public <T, A, V> V update(Fn1<T, A, V> handler, A argument) {
        Method m = MethodRefs.resolve(handler);
        String id = send("update", name(handler, Update.class), argument);
        return (V) Json.convert(awaitReply(id), m.getGenericReturnType());
    }

    /**
     * Runs a query against the execution's current state and returns its value. The query runs
     * here, in the client, on an instance of the workflow class restored from the execution's
     * {@code @Variable} values, which is the state a worker would restore to run it. So a query
     * writes nothing, cannot change the execution, works on a completed execution, and does not
     * wait for the workflow's handler lane. It sees every signal and update the execution has
     * applied; one that was sent but not yet applied is not visible yet.
     */
    @SuppressWarnings("unchecked")
    public <T, V> V query(Fn0<T, V> handler) {
        Method m = MethodRefs.resolve(handler);
        name(handler, Query.class); // checks the annotation
        WorkflowSpec spec = WorkflowSpec.of(workflowMethodOf(m.getDeclaringClass()));
        Object variables = execution(false).get("variables");
        Object instance = spec.newInstance();
        spec.restoreVariables(instance, variables instanceof Map<?, ?> v ? (Map<String, Object>) v : Map.of());
        Object value = Replayer.invokeHandler(instance, m, null);
        return (V) Json.convert(Json.plain(value), m.getGenericReturnType());
    }

    private static Method workflowMethodOf(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Workflow.class)) {
                return method;
            }
        }
        throw new IllegalArgumentException(type.getName() + " has no @Workflow method");
    }

    /** Waits for the execution to finish and returns its result. */
    @SuppressWarnings("unchecked")
    public R result(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Map<String, Object> wf = execution(false);
            String status = (String) wf.get("status");
            if (TERMINAL.contains(status)) {
                if ("COMPLETED".equals(status)) {
                    return (R) Json.decodeOutput((Map<String, Object>) wf.get("output"), resultType);
                }
                throw new WorkflowFailedException(status + ": " + wf.get("reasonForIncompletion"));
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("execution " + workflowId + " still " + status + " after " + timeout);
            }
            sleep(250);
        }
    }

    public String status() {
        return (String) execution(false).get("status");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> variables() {
        Object v = execution(false).get("variables");
        return v == null ? Map.of() : (Map<String, Object>) v;
    }

    /**
     * Waits until a task with this reference name is pending, then completes it with the given
     * output, the way a person answering a human task or an external callback would.
     */
    @SuppressWarnings("unchecked")
    public void completeTask(String taskReferenceName, Object output, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Map<String, Object> wf = execution(true);
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) wf.get("tasks");
            boolean pending = tasks != null && tasks.stream().anyMatch(t -> taskReferenceName.equals(t.get("referenceTaskName"))
                    && ("IN_PROGRESS".equals(t.get("status")) || "SCHEDULED".equals(t.get("status"))));
            if (pending) {
                try {
                    Http.post(conductor.client(), "/tasks/" + workflowId + "/" + taskReferenceName + "/COMPLETED",
                            Json.encodeOutput(output), String.class);
                    return;
                } catch (RuntimeException e) {
                    // the server may be restarting; the loop re-reads and tries again
                }
            }
            if (TERMINAL.contains(wf.get("status"))) {
                throw new IllegalStateException("execution ended before task " + taskReferenceName + " was pending");
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("task " + taskReferenceName + " was not pending within " + timeout);
            }
            sleep(250);
        }
    }

    /**
     * Sends a message and returns its id. The id is chosen here and travels in the payload, so a
     * send retried after an ambiguous failure carries the same id: the handler lane applies each
     * id once, and replies are keyed by it.
     */
    private String send(String kind, String name, Object argument) {
        String id = java.util.UUID.randomUUID().toString();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("kind", kind);
        message.put("name", name);
        message.put("id", id);
        if (argument != null) {
            message.put("args", Json.plain(argument));
        }
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();
        while (true) {
            try {
                Http.post(conductor.client(), "/workflow/" + workflowId + "/messages", message, String.class);
                return id;
            } catch (RuntimeException e) {
                // a restarting server: retry. A message accepted just before a failure may be
                // queued twice; the handler lane applies its id once. A refusal (4xx, such as an
                // execution that is no longer running) is definite and is not retried.
                if (isRefusal(e) || System.currentTimeMillis() > deadline) {
                    throw e;
                }
                sleep(1000);
            }
        }
    }

    private static boolean isRefusal(RuntimeException e) {
        return e instanceof io.orkes.conductor.client.http.ApiException api
                && api.getStatusCode() >= 400 && api.getStatusCode() < 500;
    }

    @SuppressWarnings("unchecked")
    private Object awaitReply(String messageId) {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> execution = execution(false);
            Object state = execution.get("nativeState");
            Object replies = state instanceof Map<?, ?> v ? v.get("replies") : null;
            if (replies instanceof Map<?, ?> m && m.containsKey(messageId)) {
                Map<String, Object> reply = (Map<String, Object>) m.get(messageId);
                if (reply.containsKey("error")) {
                    throw new IllegalStateException("handler failed: " + reply.get("error"));
                }
                return reply.get("value");
            }
            Object status = execution.get("status");
            if (status != null && !"RUNNING".equals(status) && !"PAUSED".equals(status)) {
                // the handler lane is gone: no reply will ever come
                throw new IllegalStateException("workflow " + workflowId + " ended with status " + status
                        + " before replying to message " + messageId);
            }
            sleep(200);
        }
        throw new IllegalStateException("no reply to message " + messageId + " within 3 minutes; is the workflow's host running?");
    }

    /**
     * Reads the execution, riding out a server that is restarting: connection failures and
     * server errors are retried for up to two minutes.
     */
    private Map<String, Object> execution(boolean includeTasks) {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();
        while (true) {
            try {
                return Http.getMap(conductor.client(), "/workflow/" + workflowId, Map.of("includeTasks", String.valueOf(includeTasks)));
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                sleep(1000);
            }
        }
    }

    private static String name(java.io.Serializable handler, Class<? extends java.lang.annotation.Annotation> kind) {
        Method m = MethodRefs.resolve(handler);
        java.lang.annotation.Annotation a = m.getAnnotation(kind);
        if (a == null) {
            throw new IllegalArgumentException(m.getName() + " is not annotated with @" + kind.getSimpleName());
        }
        String value = a instanceof Signal s ? s.value() : a instanceof Update u ? u.value() : ((Query) a).value();
        return value.isBlank() ? m.getName() : value;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
