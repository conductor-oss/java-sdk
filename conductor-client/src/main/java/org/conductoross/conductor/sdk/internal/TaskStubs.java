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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.conductoross.conductor.sdk.Conductor;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Task classes as a workflow sees them. A workflow takes its task classes as constructor
 * parameters and gets a stub for each: a subclass whose {@code @WorkerTask} methods schedule the
 * task, as {@code Conductor.task(Tasks::method, ...)} does, instead of running the method. The
 * real objects run on the workers.
 */
public final class TaskStubs {

    private static final Map<Class<?>, Boolean> TASK_TYPES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> STUB_TYPES = new ConcurrentHashMap<>();

    /** Marks a stub. */
    public interface Stub {}

    /** Ends the name of each stub class. */
    public static final String STUB_SUFFIX = "$$ConductorStub";

    /** Whether a stack frame's class is a stub, which is never the workflow's own code. */
    public static boolean isStubClass(String className) {
        return className.endsWith(STUB_SUFFIX);
    }

    private TaskStubs() {}

    /** Whether a class has {@code @WorkerTask} methods. */
    public static boolean isTaskType(Class<?> type) {
        return TASK_TYPES.computeIfAbsent(type, t -> {
            for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(WorkerTask.class)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T stub(Class<T> type) {
        try {
            return (T) STUB_TYPES.computeIfAbsent(type, TaskStubs::stubType).newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create a task stub of " + type.getName(), e);
        }
    }

    /**
     * A new instance of a workflow class: through its no-argument constructor, or else its only
     * constructor, with a stub for each task-class parameter and {@code other} supplying the rest.
     * The instance is then checked: see {@link #check}.
     */
    public static <T> T newWorkflow(Class<T> type, Function<Parameter, Object> other) {
        Constructor<?> constructor = constructorOf(type);
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = isTaskType(parameters[i].getType()) ? stub(parameters[i].getType()) : other.apply(parameters[i]);
        }
        try {
            constructor.setAccessible(true);
            return type.cast(constructor.newInstance(args));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IllegalStateException("constructor of " + type.getName() + " failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create " + type.getName(), e);
        }
    }

    /** For a parameter nothing supplies: only task classes are supplied outside Spring. */
    public static Object unsupplied(Parameter parameter) {
        Class<?> owner = parameter.getDeclaringExecutable().getDeclaringClass();
        throw new IllegalStateException("the constructor of " + owner.getName() + " takes a "
                + parameter.getType().getSimpleName() + ", which is not a class with @WorkerTask methods:"
                + " register the workflow with host().workflow(" + owner.getSimpleName()
                + ".class, factory), using Conductor.stub(...) for its task classes");
    }

    /**
     * Fails when a field of a workflow instance holds a real task object: its calls would run the
     * method in the workflow's code, again on each replay, instead of scheduling the task.
     */
    public static void check(Object workflow) {
        for (Class<?> c = workflow.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || !isTaskType(f.getType())) {
                    continue;
                }
                Object value;
                try {
                    f.setAccessible(true);
                    value = f.get(workflow);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    continue;
                }
                if (value != null && !(value instanceof Stub)) {
                    throw new IllegalStateException("field " + f.getName() + " of " + c.getName() + " holds a "
                            + f.getType().getSimpleName() + " created by the workflow, so its task methods would run"
                            + " in the workflow on every replay: take it as a constructor parameter instead, and"
                            + " Conductor supplies one that schedules the tasks");
                }
            }
        }
    }

    private static Constructor<?> constructorOf(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            if (c.getParameterCount() == 0) {
                return c;
            }
        }
        if (constructors.length == 1) {
            return constructors[0];
        }
        throw new IllegalStateException(type.getName() + " has several constructors and none without arguments:"
                + " keep one, or register it with host().workflow(" + type.getSimpleName() + ".class, factory)");
    }

    private static Constructor<?> stubType(Class<?> type) {
        if (Modifier.isFinal(type.getModifiers())) {
            throw new IllegalStateException(type.getName() + " is final, so a workflow cannot call its tasks:"
                    + " make it non-final");
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(WorkerTask.class) && Modifier.isFinal(m.getModifiers())) {
                    throw new IllegalStateException(c.getSimpleName() + "." + m.getName() + " is final, so a workflow"
                            + " cannot call it: make it non-final");
                }
            }
        }
        try {
            Class<?> stub = new ByteBuddy()
                    .subclass(type)
                    .name(type.getName() + STUB_SUFFIX)
                    .implement(Stub.class)
                    .method(not(isDeclaredBy(Object.class)).and(not(isFinal())).and(not(isStatic())))
                    .intercept(InvocationHandlerAdapter.of(handler(type)))
                    .make()
                    .load(type.getClassLoader(),
                            ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(type, MethodHandles.lookup())))
                    .getLoaded();
            // a stub holds no state: create it without running the task class's constructor
            @SuppressWarnings("removal")
            Constructor<?> constructor = sun.reflect.ReflectionFactory.getReflectionFactory()
                    .newConstructorForSerialization(stub, Object.class.getDeclaredConstructor());
            return constructor;
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException("cannot create a task stub of " + type.getName(), e);
        }
    }

    private static InvocationHandler handler(Class<?> type) {
        Map<Method, Method> targets = new ConcurrentHashMap<>();
        return (proxy, method, args) -> {
            Method target = targets.computeIfAbsent(method, m -> declared(type, m));
            if (!target.isAnnotationPresent(WorkerTask.class)) {
                throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName()
                        + " is not a @WorkerTask method: a workflow calls only the tasks of " + type.getSimpleName());
            }
            return Engine.current().call(Conductor.taskCommand(target, args == null ? new Object[0] : args));
        };
    }

    /** The method as the task class declares it, with its annotations. */
    private static Method declared(Class<?> type, Method method) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
                // keep looking up the hierarchy
            }
        }
        throw new IllegalStateException("no " + method + " in " + type.getName() + Arrays.toString(method.getParameterTypes()));
    }
}
