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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.Functions.Proc0;
import org.conductoross.conductor.sdk.Functions.Proc1;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.WorkflowFailedException;
import org.conductoross.conductor.sdk.internal.Json;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.Replayer;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

/**
 * An in-memory execution. Signals sent before {@link #result()} are delivered before the workflow
 * code starts; {@link #result()} runs the code to the end.
 */
public final class TestRun<R> {

    private final DurableTestEnvironment env;
    private final Method method;
    private final Object input;
    private final List<Object[]> signals = new ArrayList<>();
    private DirectEngine engine;
    private Object instance;
    private R result;
    private RuntimeException failure;

    TestRun(DurableTestEnvironment env, Method method, Object input) {
        this.env = env;
        this.method = method;
        this.input = input;
    }

    public <T, A> TestRun<R> signal(Proc1<T, A> handler, A argument) {
        signals.add(new Object[] {MethodRefs.resolve(handler), argument});
        return this;
    }

    public <T> TestRun<R> signal(Proc0<T> handler) {
        signals.add(new Object[] {MethodRefs.resolve(handler), null});
        return this;
    }

    /** Applies an update to the instance before the run (or after it, on the finished instance). */
    @SuppressWarnings("unchecked")
    public <T, A, V> V update(Fn1<T, A, V> handler, A argument) {
        Method m = MethodRefs.resolve(handler);
        if (m.getAnnotation(Update.class) == null) {
            throw new IllegalArgumentException(m.getName() + " is not an @Update handler");
        }
        return (V) Replayer.invokeHandler(instance(), m, Json.plain(argument));
    }

    /** Runs the workflow to the end and returns its result. */
    @SuppressWarnings("unchecked")
    public R result() {
        if (engine == null) {
            WorkflowSpec spec = DirectEngine.spec(method);
            engine = new DirectEngine(env, "test-" + UUID.randomUUID());
            Object target = instance();
            for (Object[] s : signals) {
                Method handler = (Method) s[0];
                if (handler.getAnnotation(Signal.class) == null) {
                    throw new IllegalArgumentException(handler.getName() + " is not a @Signal handler");
                }
                Replayer.invokeHandler(target, handler, Json.plain(s[1]));
            }
            try {
                Object value = DirectEngine.runWorkflow(spec, target, DirectEngine.plainInput(method, input), engine);
                result = (R) Json.decodeOutput(Json.encodeOutput(value), method.getGenericReturnType());
            } catch (TaskFailure | WorkflowFailedException e) {
                failure = new WorkflowFailedException(e.getMessage());
            } catch (RuntimeException e) {
                failure = e;
            }
        }
        if (failure != null) {
            throw failure;
        }
        return result;
    }

    /** Whether a server step with this reference name ran (or was attempted). */
    public boolean ran(String ref) {
        result();
        return engine.ran.contains(ref);
    }

    /** Reference names that ran with this base: {@code base}, {@code base_2}, and so on. */
    public List<String> tasks(String base) {
        result();
        return engine.ran.stream().filter(r -> r.equals(base) || r.matches(java.util.regex.Pattern.quote(base) + "_\\d+")).toList();
    }

    /** All reference names, in the order they ran. */
    public List<String> ranTasks() {
        result();
        return List.copyOf(engine.ran);
    }

    private Object instance() {
        if (instance == null) {
            instance = env.newInstance(DirectEngine.spec(method));
        }
        return instance;
    }

    Map<String, Object> variables() {
        return DirectEngine.spec(method).captureVariables(instance());
    }
}
