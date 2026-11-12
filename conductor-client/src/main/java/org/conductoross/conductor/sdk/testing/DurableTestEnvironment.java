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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Functions.Fn1;
import org.conductoross.conductor.sdk.internal.MethodRefs;
import org.conductoross.conductor.sdk.internal.WorkflowSpec;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

/**
 * Tests for native workflows without a server or Docker. {@link #inMemory()} runs the workflow
 * code in process: tasks call the registered task objects, typically Mockito mocks of the task
 * classes; timers are skipped; HTTP, human, external and LLM steps answer from stubs given here.
 *
 * <pre>{@code
 * try (DurableTestEnvironment env = DurableTestEnvironment.inMemory()) {
 *     env.workflows(OrderWorkflow.class);
 *     env.tasks(mock(PaymentTasks.class));
 *     env.http("fraud").respond("{\"flagged\": false}");
 *     OrderResult r = env.start(OrderWorkflow::process, order).result();
 * }
 * }</pre>
 */
public final class DurableTestEnvironment implements AutoCloseable {

    final Map<String, Answer> answers = new HashMap<>();
    private final Map<Class<?>, Supplier<?>> factories = new LinkedHashMap<>();
    private final List<Object> taskObjects = new ArrayList<>();

    private DurableTestEnvironment() {}

    public static DurableTestEnvironment inMemory() {
        return new DurableTestEnvironment();
    }

    /**
     * Workflow classes, created as the host creates them: a constructor taking task classes gets
     * stubs whose calls go to the task objects registered with {@link #tasks}, or a no-argument
     * constructor. A constructor that also takes other parameters needs {@link #workflow(Class,
     * Supplier)}. Classes used as children must be listed too.
     */
    public DurableTestEnvironment workflows(Class<?>... types) {
        for (Class<?> t : types) {
            factories.putIfAbsent(t, null);
        }
        return this;
    }

    /** A workflow class created by {@code factory}; {@code Conductor.stub(Tasks.class)} gives it task stubs. */
    public <T> DurableTestEnvironment workflow(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
        return this;
    }

    /** Objects with {@code @WorkerTask} methods; mocks of task classes work. */
    public DurableTestEnvironment tasks(Object... objects) {
        taskObjects.addAll(List.of(objects));
        return this;
    }

    public HttpStub http(String ref) {
        return new HttpStub(ref);
    }

    public HumanStub human(String ref) {
        return new HumanStub(ref);
    }

    public ExternalStub external(String ref) {
        return new ExternalStub(ref);
    }

    public LlmStub llm(String ref) {
        return new LlmStub(ref);
    }

    /** Answers for an MCP step: a list of {@code McpTool} for listTools, an {@code McpResult} for a call. */
    public OutputStub mcp(String ref) {
        return new OutputStub("mcp:" + ref);
    }

    /** Answers for an agent step: an {@code AgentResult}. */
    public OutputStub agent(String ref) {
        return new OutputStub("agent:" + ref);
    }

    public final class OutputStub {
        private final String key;

        OutputStub(String key) {
            this.key = key;
        }

        public DurableTestEnvironment respond(Object result) {
            answer(key, result, null, false);
            return DurableTestEnvironment.this;
        }

        public DurableTestEnvironment fail(String reason) {
            answer(key, null, reason, false);
            return DurableTestEnvironment.this;
        }
    }

    public <T, A, R> TestRun<R> start(Fn1<T, A, R> workflow, A input) {
        Method method = MethodRefs.resolve(workflow);
        return new TestRun<>(this, method, input);
    }

    @Override
    public void close() {}

    // ------------------------------------------------------------------ internals

    Object newInstance(WorkflowSpec spec) {
        Supplier<?> factory = factories.get(spec.type);
        return factory != null ? factory.get() : spec.newInstance();
    }

    Object runChild(Method childMethod, Object input) {
        WorkflowSpec spec = DirectEngine.spec(childMethod);
        DirectEngine engine = new DirectEngine(this, "child-" + UUID.randomUUID());
        return DirectEngine.runWorkflow(spec, newInstance(spec), DirectEngine.plainInput(childMethod, input), engine);
    }

    Object taskObjectFor(Class<?> type) {
        for (Object o : taskObjects) {
            if (type.isInstance(o)) {
                return o;
            }
        }
        throw new IllegalStateException("no task object of type " + type.getSimpleName() + "; register one with env.tasks(...)");
    }

    Map.Entry<Object, Method> taskByName(String name) {
        for (Object o : taskObjects) {
            for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    WorkerTask wt = m.getAnnotation(WorkerTask.class);
                    if (wt != null && wt.value().equals(name)) {
                        return Map.entry(o, m);
                    }
                }
            }
        }
        throw new IllegalStateException("no @WorkerTask named " + name + " among the registered task objects");
    }

    static final class Answer {
        Object value;
        String failure;
        boolean timeout;
    }

    private void answer(String key, Object value, String failure, boolean timeout) {
        Answer a = new Answer();
        a.value = value;
        a.failure = failure;
        a.timeout = timeout;
        answers.put(key, a);
    }

    public final class HttpStub {
        private final String ref;

        HttpStub(String ref) {
            this.ref = ref;
        }

        /** The response body, as JSON text. */
        public DurableTestEnvironment respond(String body) {
            answer("http:" + ref, body, null, false);
            return DurableTestEnvironment.this;
        }

        public DurableTestEnvironment fail(String reason) {
            answer("http:" + ref, null, reason, false);
            return DurableTestEnvironment.this;
        }
    }

    public final class HumanStub {
        private final String ref;

        HumanStub(String ref) {
            this.ref = ref;
        }

        public DurableTestEnvironment answer(Object form) {
            DurableTestEnvironment.this.answer("human:" + ref, form, null, false);
            return DurableTestEnvironment.this;
        }

        public DurableTestEnvironment timeout() {
            DurableTestEnvironment.this.answer("human:" + ref, null, null, true);
            return DurableTestEnvironment.this;
        }
    }

    public final class ExternalStub {
        private final String ref;

        ExternalStub(String ref) {
            this.ref = ref;
        }

        public DurableTestEnvironment complete(Object body) {
            answer("external:" + ref, body, null, false);
            return DurableTestEnvironment.this;
        }

        public DurableTestEnvironment timeout() {
            answer("external:" + ref, null, null, true);
            return DurableTestEnvironment.this;
        }
    }

    public final class LlmStub {
        private final String ref;

        LlmStub(String ref) {
            this.ref = ref;
        }

        public DurableTestEnvironment respond(String text) {
            answer("llm:" + ref, text, null, false);
            return DurableTestEnvironment.this;
        }

        public DurableTestEnvironment fail(String reason) {
            answer("llm:" + ref, null, reason, false);
            return DurableTestEnvironment.this;
        }
    }
}
