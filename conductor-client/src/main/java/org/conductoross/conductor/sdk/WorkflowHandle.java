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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.conductoross.conductor.sdk.Functions.Fn0;
import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.Functions.Proc0;
import org.conductoross.conductor.sdk.Functions.Proc1;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.Workflow.WorkflowStatus;

/**
 * An execution, seen from the client: wait for its result, send it signals and updates, query its
 * state. Handlers are addressed by method reference, so renaming a handler is a compile error at
 * every call site.
 */
public final class WorkflowHandle<R> {

    // the Conductor this handle came from: its client, the factories of the workflow classes it
    // hosts (for query), and the one thread that waits for results, which close() stops
    private final Conductor conductor;
    private final WorkflowClient workflows;
    private final String workflowId;
    private final Type resultType;

    WorkflowHandle(Conductor conductor, String workflowId, Type resultType) {
        this.conductor = conductor;
        this.workflows = new WorkflowClient(conductor.client());
        this.workflowId = workflowId;
        this.resultType = resultType;
    }

    public String workflowId() {
        return workflowId;
    }

    /** The execution's status now. */
    public WorkflowStatus status() {
        return execution(false).getStatus();
    }

    /**
     * Waits for the execution to finish and returns its result. Throws {@link
     * WorkflowFailedException} when it ended without completing, and {@link
     * WorkflowTimeoutException} when it is still going after {@code timeout}; the execution keeps
     * running.
     */
    public R result(Duration timeout) {
        CompletableFuture<R> result = resultAsync();
        try {
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            result.cancel(false);
            throw new WorkflowTimeoutException(workflowId, status(), timeout);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException cause ? cause : new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            result.cancel(false);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * The result as a future, completed when the execution finishes: with the workflow method's
     * return value, or exceptionally with a {@link WorkflowFailedException}. For waiting on many
     * executions at once, or composing with other futures:
     *
     * <pre>{@code
     * CompletableFuture<Receipt> a = conductor.start(Order::process, first).resultAsync();
     * CompletableFuture<Receipt> b = conductor.start(Order::process, second).resultAsync();
     * CompletableFuture.allOf(a, b).orTimeout(2, TimeUnit.MINUTES).join();
     * }</pre>
     *
     * One thread per {@link Conductor} checks every pending future, and a server that is
     * restarting is waited out. Cancelling the future only stops waiting: the execution keeps
     * running. Closing the Conductor cancels the futures still waiting.
     */
    public CompletableFuture<R> resultAsync() {
        return conductor.whenFinished(() -> {
            Workflow execution;
            try {
                execution = workflows.getWorkflow(workflowId, false);
            } catch (RuntimeException e) {
                if (Http.isRefusal(e)) {
                    throw e; // no such execution, for one: it will not finish
                }
                return null; // a restarting server: check again
            }
            return execution.getStatus().isTerminal() ? execution : null;
        }, this::outcome);
    }

    /** The result of a finished execution, or the failure it ended with. */
    @SuppressWarnings("unchecked")
    private R outcome(Workflow execution) {
        if (execution.getStatus() == WorkflowStatus.COMPLETED) {
            return (R) Json.decodeOutput(execution.getOutput(), resultType);
        }
        throw new WorkflowFailedException(workflowId, execution.getStatus(), execution.getReasonForIncompletion());
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
     * applied; one that was sent but not yet applied is not visible yet. A class this {@code
     * Conductor} hosts with a factory ({@code workflows(Type.class, factory)}) is made by that
     * factory.
     */
    @SuppressWarnings("unchecked")
    public <T, V> V query(Fn0<T, V> handler) {
        Method m = MethodRefs.resolve(handler);
        name(handler, Query.class); // checks the annotation
        WorkflowSpec spec = conductor.hostedSpec(m.getDeclaringClass())
                .orElseGet(() -> WorkflowSpec.of(m.getDeclaringClass()).get(0));
        Map<String, Object> variables = execution(false).getVariables();
        Object instance = spec.newInstance();
        spec.restoreVariables(instance, variables == null ? Map.of() : variables);
        Object value = Replayer.invokeHandler(instance, m, null);
        return (V) Json.convert(Json.plain(value), m.getGenericReturnType());
    }

    /**
     * Waits until a task with this reference name is pending, then completes it with the given
     * output, the way a person answering a human task or an external callback would.
     */
    public void completeTask(String taskReferenceName, Object output, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            Workflow execution = execution(true);
            boolean pending = execution.getTasks().stream().anyMatch(t -> taskReferenceName.equals(t.getReferenceTaskName())
                    && (t.getStatus() == Task.Status.IN_PROGRESS || t.getStatus() == Task.Status.SCHEDULED));
            if (pending) {
                TaskClient tasks = new TaskClient(conductor.client());
                Http.retrying(() -> {
                    tasks.updateTask(workflowId, taskReferenceName, TaskResult.Status.COMPLETED, Json.encodeOutput(output));
                    return null;
                });
                return;
            }
            if (execution.getStatus().isTerminal()) {
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
        String id = UUID.randomUUID().toString();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("kind", kind);
        message.put("name", name);
        message.put("id", id);
        if (argument != null) {
            message.put("args", Json.plain(argument));
        }
        Http.retrying(() -> Http.post(conductor.client(), "/workflow/" + workflowId + "/messages", message, String.class));
        return id;
    }

    private Object awaitReply(String messageId) {
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
        while (System.currentTimeMillis() < deadline) {
            Workflow execution = execution(false);
            Map<String, Object> replies = execution.getNativeState() == null ? Map.of() : execution.getNativeState().getReplies();
            if (replies.get(messageId) instanceof Map<?, ?> reply) {
                if (reply.containsKey("error")) {
                    throw new IllegalStateException("handler failed: " + reply.get("error"));
                }
                return reply.get("value");
            }
            if (execution.getStatus().isTerminal()) {
                // the handler lane is gone: no reply will ever come
                throw new IllegalStateException("workflow " + workflowId + " ended with status " + execution.getStatus()
                        + " before replying to message " + messageId);
            }
            sleep(200);
        }
        throw new IllegalStateException("no reply to message " + messageId + " within 3 minutes; is the workflow's host running?");
    }

    private Workflow execution(boolean includeTasks) {
        return Http.retrying(() -> workflows.getWorkflow(workflowId, includeTasks));
    }

    private static String name(Serializable handler, Class<? extends Annotation> kind) {
        Method m = MethodRefs.resolve(handler);
        Annotation a = m.getAnnotation(kind);
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
