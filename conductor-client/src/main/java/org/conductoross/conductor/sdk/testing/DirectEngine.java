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
package org.conductoross.conductor.sdk.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Backoff;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Functions;
import org.conductoross.conductor.sdk.Promise;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

/**
 * Runs workflow code in process, answering every server call at once: tasks call the registered
 * task objects (typically Mockito mocks), timers are skipped, forks run their lanes one after the
 * other, child workflows run inline, and HTTP, human, external and LLM steps answer from the test's
 * stubs. Reference names are derived exactly as against a server, so the graph is assertable.
 */
final class DirectEngine implements Engine {

    private final DurableTestEnvironment env;
    private final String workflowId;
    private final Set<String> used = new HashSet<>();
    final List<String> ran = new ArrayList<>();
    private Group pendingGroup;
    private int blockingCalls;
    private boolean capture;

    DirectEngine(DurableTestEnvironment env, String workflowId) {
        this.env = env;
        this.workflowId = workflowId;
    }

    private static final class Group {
        final String forkName;
        final List<DirectPromise<?>> members = new ArrayList<>();
        boolean blocked;

        Group(String forkName) {
            this.forkName = forkName;
        }
    }

    private static final class Capture extends RuntimeException {
        final Command command;

        Capture(Command command) {
            super(null, null, false, false);
            this.command = command;
        }
    }

    private String alloc(String base) {
        String ref = base;
        for (int n = 2; used.contains(ref); n++) {
            ref = base + "_" + n;
        }
        used.add(ref);
        return ref;
    }


    private void settleGroup() {
        if (pendingGroup != null) {
            Group g = pendingGroup;
            pendingGroup = null;
            closeGroup(g);
        }
    }

    private void closeGroup(Group g) {
        if (g.blocked) {
            String fork = alloc(g.forkName != null ? g.forkName : "fork");
            ran.add(fork);
            ran.add(alloc(fork + "_join"));
        }
    }

    @Override
    public String workflowId() {
        return workflowId;
    }

    @Override
    public Object call(Command cmd) {
        if (capture) {
            throw new Capture(cmd);
        }
        settleGroup();
        // numbered like Replayer.call: x, x_2, x_3 by how many came before
        String ref = alloc(cmd.explicitRef != null ? cmd.explicitRef : cmd.refBase);
        return execute(cmd, ref);
    }

    private Object execute(Command cmd, String ref) {
        ran.add(ref);
        if (cmd.blocking()) {
            blockingCalls++;
        }
        return switch (cmd.kind) {
            case SIMPLE -> decode(cmd, invokeTask(cmd, ref));
            case SUB_WORKFLOW -> decode(cmd, Json.encodeOutput(env.runChild(cmd.childMethod, cmd.childInput)));
            case START_WORKFLOW, EVENT -> null;
            case WAIT -> {
                if (!cmd.emptyIsEmpty) {
                    yield null;
                }
                DurableTestEnvironment.Answer a = answer("external:", cmd, ref);
                yield a == null || a.timeout ? Optional.empty() : Optional.ofNullable(Json.convert(a.value, cmd.resultType));
            }
            case HUMAN -> {
                DurableTestEnvironment.Answer a = answer("human:", cmd, ref);
                yield a == null || a.timeout ? Optional.empty() : Optional.ofNullable(Json.convert(a.value, cmd.resultType));
            }
            case HTTP -> {
                DurableTestEnvironment.Answer a = answer("http:", cmd, ref);
                if (a == null || a.failure != null) {
                    throw new TaskFailure(ref, "FAILED", a == null ? "no http stub for " + ref : a.failure);
                }
                yield Json.convert(parse(String.valueOf(a.value)), cmd.resultType);
            }
            case MCP_LIST, MCP_CALL, AGENT -> {
                String kind = cmd.kind == Command.Kind.AGENT ? "agent:" : "mcp:";
                DurableTestEnvironment.Answer a = answer(kind, cmd, ref);
                if (a == null || a.failure != null) {
                    throw new TaskFailure(ref, "FAILED", a == null ? "no stub for " + ref : a.failure);
                }
                if (cmd.outputKey != null) {
                    yield Json.convert(a.value, cmd.resultType);
                }
                yield decode(cmd, Json.encodeOutput(a.value));
            }
            case LLM -> {
                DurableTestEnvironment.Answer a = answer("llm:", cmd, ref);
                if (a == null || a.failure != null) {
                    throw new TaskFailure(ref, "FAILED", a == null ? "no llm stub for " + ref : a.failure);
                }
                yield decode(cmd, Map.of("result", a.value));
            }
        };
    }

    /** The stub for this call: one given for its numbered name (x_2), else for the name in the code (x). */
    private DurableTestEnvironment.Answer answer(String kind, Command cmd, String ref) {
        DurableTestEnvironment.Answer a = env.answers.get(kind + ref);
        return a != null || cmd.explicitRef == null ? a : env.answers.get(kind + cmd.explicitRef);
    }

    private static Object parse(String json) {
        try {
            return Json.MAPPER.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private Object decode(Command cmd, Map<String, Object> output) {
        Object v = Json.decodeOutput(output, cmd.resultType);
        return cmd.optional ? Optional.ofNullable(v) : v;
    }

    private Map<String, Object> invokeTask(Command cmd, String ref) {
        Method method = cmd.method;
        Object target;
        Object[] args;
        if (method != null) {
            target = env.taskObjectFor(method.getDeclaringClass());
            args = cmd.args;
        } else {
            Map.Entry<Object, Method> found = env.taskByName(cmd.name);
            target = found.getKey();
            method = found.getValue();
            String[] names = MethodRefs.parameterNames(method);
            args = new Object[names.length];
            for (int i = 0; i < names.length; i++) {
                Object v = cmd.input.get(names[i]);
                args[i] = v == null && method.getParameterTypes()[i].isPrimitive()
                        ? Json.convert(0, method.getGenericParameterTypes()[i])
                        : Json.convert(v, method.getGenericParameterTypes()[i]);
            }
        }
        // Retried like the server retries it: the @WorkerTask retry count (default 3), except
        // after a NonRetryableException. Delays are skipped.
        com.netflix.conductor.sdk.workflow.task.WorkerTask wt =
                method.getAnnotation(com.netflix.conductor.sdk.workflow.task.WorkerTask.class);
        int retries = wt != null && wt.retryCount() >= 0 ? wt.retryCount() : 3;
        method.setAccessible(true);
        for (int attempt = 0; ; attempt++) {
            try {
                return Json.encodeOutput(method.invoke(target, args));
            } catch (InvocationTargetException e) {
                Throwable c = e.getCause();
                boolean terminal = c instanceof com.netflix.conductor.sdk.workflow.executor.task.NonRetryableException;
                if (terminal || attempt >= retries) {
                    throw new TaskFailure(
                            ref,
                            terminal ? "FAILED_WITH_TERMINAL_ERROR" : "FAILED",
                            c.getMessage() == null ? c.toString() : c.getMessage());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void fire(Command cmd) {
        call(cmd);
    }

    @Override
    public Object switchOn(String name, Object value, Map<String, Supplier<?>> cases, Supplier<?> otherwise) {
        settleGroup();
        ran.add(alloc(name));
        Object plain = Json.plain(value);
        String key = plain == null ? "null" : String.valueOf(plain);
        Supplier<?> body = cases.containsKey(key) ? cases.get(key) : otherwise;
        if (body == null) {
            throw new org.conductoross.conductor.sdk.WorkflowFailedException("switch " + name + " has no case for " + key);
        }
        return body.get();
    }

    @Override
    public Object once(String name, Supplier<?> supplier) {
        settleGroup();
        ran.add(alloc(name));
        return supplier.get();
    }

    @Override
    public Instant now() {
        settleGroup();
        ran.add(alloc("now"));
        return Instant.now();
    }

    @Override
    public double random() {
        settleGroup();
        ran.add(alloc("random"));
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public <T> Promise<T> async(Supplier<T> body) {
        if (pendingGroup == null) {
            pendingGroup = new Group(null);
        }
        return run(pendingGroup, body);
    }

    private <T> DirectPromise<T> run(Group g, Supplier<T> body) {
        int before = blockingCalls;
        Group saved = pendingGroup;
        pendingGroup = null;
        DirectPromise<T> p = new DirectPromise<>();
        try {
            p.value = body.get();
        } catch (RuntimeException e) {
            p.error = e;
        }
        pendingGroup = saved;
        if (blockingCalls > before) {
            g.blocked = true;
        }
        g.members.add(p);
        return p;
    }

    @Override
    public void join(List<? extends Promise<?>> promises) {
        settleGroup();
        for (Promise<?> p : promises) {
            p.get();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, R> List<R> fanOut(String name, List<I> items, Function<I, R> body) {
        settleGroup();
        Group g = new Group(name);
        for (I item : items) {
            run(g, () -> body.apply(item));
        }
        closeGroup(g);
        List<R> results = new ArrayList<>();
        for (DirectPromise<?> p : g.members) {
            results.add((R) p.get());
        }
        return results;
    }

    @Override
    public boolean awaitCondition(BooleanSupplier condition, Duration timeout) {
        settleGroup();
        if (condition.getAsBoolean()) {
            return true;
        }
        ran.add(alloc("condition"));
        blockingCalls++;
        return condition.getAsBoolean();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object repeatUntil(Supplier<?> body, Functions.Pred<?> condition, Backoff backoff) {
        settleGroup();
        Command check;
        capture = true;
        try {
            body.get();
            throw new IllegalArgumentException("the repeatUntil body must make exactly one task call");
        } catch (Capture c) {
            check = c.command;
        } finally {
            capture = false;
        }
        MethodRefs.resolve(condition);
        String loopRef = alloc("poll");
        String checkRef = loopRef + "_check";
        used.add(loopRef + "_backoff");
        used.add(loopRef + "_wait");
        used.add(checkRef);
        ran.add(loopRef);
        Object result = null;
        for (int i = 0; i < backoff.attempts(); i++) {
            try {
                result = execute(check, checkRef);
            } catch (TaskFailure e) {
                result = null;
                continue;
            }
            if (result != null && ((Functions.Pred) condition).test(result)) {
                break;
            }
        }
        return result;
    }

    @Override
    public List<Object> forEach(String name, List<?> items, Command task, String itemParameter) {
        settleGroup();
        List<Object> results = new ArrayList<>();
        if (items.isEmpty()) {
            return results;
        }
        String loopRef = alloc(name);
        String taskRef = loopRef + "_task";
        used.add(taskRef);
        ran.add(loopRef);
        blockingCalls++;
        for (Object item : items) {
            task.input.put(itemParameter, Json.plain(item));
            task.args = new Object[] {item};
            results.add(execute(task, taskRef));
        }
        return results;
    }

    private static final class DirectPromise<T> implements Promise<T> {
        T value;
        RuntimeException error;

        @Override
        public T get() {
            if (error != null) {
                throw error;
            }
            return value;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    static Object runWorkflow(WorkflowSpec spec, Object instance, Map<String, Object> input, DirectEngine engine) {
        Engine previous = Engine.CURRENT.get();
        Engine.CURRENT.set(engine);
        try {
            return spec.method.invoke(instance, WorkflowSpec.decodeInput(spec.method, input));
        } catch (InvocationTargetException e) {
            Throwable c = e.getCause();
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(c);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } finally {
            Engine.CURRENT.set(previous);
        }
    }

    static Map<String, Object> plainInput(Method method, Object input) {
        return input == null ? new LinkedHashMap<>() : WorkflowSpec.encodeInput(method, new Object[] {input});
    }

    static WorkflowSpec spec(Method method) {
        return Conductor.spec(method);
    }
}
