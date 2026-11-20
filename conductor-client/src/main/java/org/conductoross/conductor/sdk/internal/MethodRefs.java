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

import java.io.Serializable;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a serializable method reference to the {@link Method} it names. The JVM gives every
 * serializable lambda a {@code writeReplace} method returning a {@link SerializedLambda}, which
 * records the implementation class, method name and descriptor. Reflection only; no bytecode is
 * read or generated.
 */
public final class MethodRefs {

    private static final Map<Class<?>, Method> CACHE = new ConcurrentHashMap<>();

    private MethodRefs() {}

    public static Method resolve(Serializable ref) {
        return CACHE.computeIfAbsent(ref.getClass(), c -> doResolve(ref));
    }

    /** The compiled method behind a serializable lambda or method reference. */
    public record Implementation(String owner, String name, String descriptor) {}

    public static Implementation implementation(Serializable ref) {
        SerializedLambda lambda = serialized(ref);
        return new Implementation(lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature());
    }

    /** The method a bytecode method handle names, or null when it cannot be loaded. */
    static Method method(ClassLoader loader, org.objectweb.asm.Handle handle) {
        try {
            Class<?> owner = Class.forName(handle.getOwner().replace('/', '.'), false, loader);
            MethodType type = MethodType.fromMethodDescriptorString(handle.getDesc(), loader);
            for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                try {
                    return c.getDeclaredMethod(handle.getName(), type.parameterArray());
                } catch (NoSuchMethodException ignored) {
                    // keep looking up the hierarchy
                }
            }
            return owner.getMethod(handle.getName(), type.parameterArray());
        } catch (ReflectiveOperationException | TypeNotPresentException e) {
            return null;
        }
    }

    private static SerializedLambda serialized(Serializable ref) {
        try {
            Method writeReplace = ref.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(ref);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("not a serializable method reference: " + ref, e);
        }
    }

    private static Method doResolve(Serializable ref) {
        SerializedLambda lambda = serialized(ref);
        String name = lambda.getImplMethodName();
        if (name.startsWith("lambda$")) {
            throw new IllegalArgumentException(
                    "expected a method reference such as Tasks::method, got a lambda");
        }
        try {
            ClassLoader loader = ref.getClass().getClassLoader();
            Class<?> owner = Class.forName(lambda.getImplClass().replace('/', '.'), false, loader);
            MethodType type = MethodType.fromMethodDescriptorString(lambda.getImplMethodSignature(), loader);
            for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name, type.parameterArray());
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {
                    // keep looking up the hierarchy
                }
            }
            Method m = owner.getMethod(name, type.parameterArray());
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("cannot resolve method reference " + name, e);
        }
    }

    /** Parameter names, which need {@code -parameters} at compile time. */
    public static String[] parameterNames(Method method) {
        Parameter[] params = method.getParameters();
        String[] names = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            com.netflix.conductor.sdk.workflow.task.InputParam ip =
                    params[i].getAnnotation(com.netflix.conductor.sdk.workflow.task.InputParam.class);
            if (ip != null) {
                names[i] = ip.value();
            } else if (params[i].isNamePresent()) {
                names[i] = params[i].getName();
            } else {
                throw new IllegalStateException(
                        "parameter names of "
                                + method.getDeclaringClass().getSimpleName()
                                + "."
                                + method.getName()
                                + " are not available: compile with -parameters, or annotate each"
                                + " parameter with @InputParam(\"name\")");
            }
        }
        return names;
    }

    /** The property a boolean accessor reads: {@code done()}, {@code isDone()} and {@code getDone()} all read {@code done}. */
    public static String propertyName(Method accessor) {
        String n = accessor.getName();
        if (n.startsWith("is") && n.length() > 2 && Character.isUpperCase(n.charAt(2))) {
            return Character.toLowerCase(n.charAt(2)) + n.substring(3);
        }
        if (n.startsWith("get") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
            return Character.toLowerCase(n.charAt(3)) + n.substring(4);
        }
        return n;
    }
}
