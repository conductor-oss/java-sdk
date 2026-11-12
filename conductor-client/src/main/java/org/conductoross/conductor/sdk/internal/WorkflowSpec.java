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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.conductoross.conductor.sdk.Query;
import org.conductoross.conductor.sdk.Signal;
import org.conductoross.conductor.sdk.Update;
import org.conductoross.conductor.sdk.Variable;
import org.conductoross.conductor.sdk.Workflow;

/** Everything the SDK reads from a class with a {@link Workflow} method. */
public final class WorkflowSpec {

    public final Class<?> type;
    public final Method method;
    public final String name;
    public final int version;
    public final String ownerEmail;
    public final String description;
    public final long timeoutSeconds;
    public final String failureWorkflow;
    public final int failureWorkflowVersion;
    public final List<org.conductoross.conductor.sdk.Schedule> schedules = new ArrayList<>();
    public final Map<String, Field> variables = new LinkedHashMap<>();
    public final Map<String, Method> signals = new LinkedHashMap<>();
    public final Map<String, Method> updates = new LinkedHashMap<>();
    public final Map<String, Method> queries = new LinkedHashMap<>();
    private Supplier<Object> factory;

    private WorkflowSpec(Method method) {
        Workflow wf = method.getAnnotation(Workflow.class);
        this.type = method.getDeclaringClass();
        this.method = method;
        method.setAccessible(true);
        this.name = wf.name().isBlank() ? snake(method.getName()) : wf.name();
        this.version = wf.version();
        this.ownerEmail = wf.ownerEmail();
        this.description = wf.description();
        this.timeoutSeconds = wf.timeout().isBlank() ? 0 : Duration.parse(wf.timeout()).toSeconds();
        this.failureWorkflow = wf.failureWorkflow();
        this.failureWorkflowVersion = wf.failureWorkflowVersion();
        schedules.addAll(List.of(method.getAnnotationsByType(org.conductoross.conductor.sdk.Schedule.class)));
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Variable v = f.getAnnotation(Variable.class);
                if (v != null) {
                    if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                        throw new IllegalStateException(
                                "@Variable field " + f.getName() + " must be neither static nor final");
                    }
                    f.setAccessible(true);
                    variables.put(v.value().isBlank() ? f.getName() : v.value(), f);
                }
            }
        }
        for (Method m : type.getMethods()) {
            Signal s = m.getAnnotation(Signal.class);
            Update u = m.getAnnotation(Update.class);
            Query q = m.getAnnotation(Query.class);
            if (s != null) {
                signals.put(s.value().isBlank() ? m.getName() : s.value(), m);
            }
            if (u != null) {
                updates.put(u.value().isBlank() ? m.getName() : u.value(), m);
            }
            if (q != null) {
                queries.put(q.value().isBlank() ? m.getName() : q.value(), m);
            }
        }
        this.factory = defaultFactory(type);
    }

    public static WorkflowSpec of(Method method) {
        if (method.getAnnotation(Workflow.class) == null) {
            throw new IllegalArgumentException(method + " is not annotated with @Workflow");
        }
        return new WorkflowSpec(method);
    }

    /** All @Workflow methods of a class. */
    public static List<WorkflowSpec> of(Class<?> type) {
        List<WorkflowSpec> specs = new ArrayList<>();
        for (Method m : type.getMethods()) {
            if (m.getAnnotation(Workflow.class) != null) {
                specs.add(new WorkflowSpec(m));
            }
        }
        if (specs.isEmpty()) {
            throw new IllegalArgumentException(type.getName() + " has no @Workflow method");
        }
        return specs;
    }

    public void factory(Supplier<Object> factory) {
        this.factory = factory;
    }

    public Object newInstance() {
        Object instance = factory.get();
        TaskStubs.check(instance);
        return instance;
    }

    public String codeTaskName() {
        return name + ".v" + version + ".code";
    }

    public boolean hasHandlers() {
        return !signals.isEmpty() || !updates.isEmpty() || !queries.isEmpty();
    }

    public Method handler(String kind, String handlerName) {
        Map<String, Method> handlers =
                switch (kind) {
                    case "signal" -> signals;
                    case "update" -> updates;
                    case "query" -> queries;
                    default -> Map.of();
                };
        Method m = handlers.get(handlerName);
        if (m == null) {
            throw new IllegalArgumentException("no @" + kind + " handler named " + handlerName + " on " + type.getSimpleName());
        }
        return m;
    }

    // ------------------------------------------------------------------ input and variables

    /** Workflow input from method arguments: a single object parameter is the input itself. */
    public static Map<String, Object> encodeInput(Method method, Object[] args) {
        if (method.getParameterCount() == 1 && !Json.isWrapped(method.getGenericParameterTypes()[0])) {
            return Json.map(args[0]);
        }
        String[] names = MethodRefs.parameterNames(method);
        Map<String, Object> input = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            input.put(names[i], Json.plain(args[i]));
        }
        return input;
    }

    public static Object[] decodeInput(Method method, Map<String, Object> input) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }
        if (method.getParameterCount() == 1 && !Json.isWrapped(method.getGenericParameterTypes()[0])) {
            return new Object[] {Json.convert(input == null ? Map.of() : input, method.getGenericParameterTypes()[0])};
        }
        String[] names = MethodRefs.parameterNames(method);
        Object[] args = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            Object v = input == null ? null : input.get(names[i]);
            args[i] = v == null && method.getParameterTypes()[i].isPrimitive()
                    ? Json.convert(0, method.getGenericParameterTypes()[i])
                    : Json.convert(v, method.getGenericParameterTypes()[i]);
        }
        return args;
    }

    public Map<String, Object> captureVariables(Object instance) {
        Map<String, Object> vars = new LinkedHashMap<>();
        variables.forEach(
                (n, f) -> {
                    try {
                        vars.put(n, Json.plain(f.get(instance)));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                });
        return vars;
    }

    /** Only the variables this class declares, from a map of workflow variables. */
    public Map<String, Object> captureVariablesOf(Map<String, Object> values) {
        Map<String, Object> vars = new LinkedHashMap<>();
        for (String n : variables.keySet()) {
            if (values.containsKey(n)) {
                vars.put(n, values.get(n));
            }
        }
        return vars;
    }

    public void restoreVariables(Object instance, Map<String, Object> values) {
        if (values == null) {
            return;
        }
        variables.forEach(
                (n, f) -> {
                    if (values.containsKey(n)) {
                        try {
                            Object v = values.get(n);
                            f.set(instance, v == null && f.getType().isPrimitive() ? f.get(instance) : Json.convert(v, f.getGenericType()));
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });
    }

    /** Its no-argument constructor, or its only constructor with a task stub for each task class. */
    private static Supplier<Object> defaultFactory(Class<?> type) {
        return () -> TaskStubs.newWorkflow(type, TaskStubs::unsupplied);
    }

    static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
