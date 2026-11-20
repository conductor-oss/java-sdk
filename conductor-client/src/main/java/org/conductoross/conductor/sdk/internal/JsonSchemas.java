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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkflowInstanceIdInputParam;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

/**
 * JSON Schemas of what a task or workflow takes and gives, read from its Java signature by
 * reflection, shaped as the SDK actually sends it: a task's input has one property per parameter,
 * named as its calls name it; an output that is an object spreads into its properties, and any
 * other value is {@code result}. Types are read as Jackson, which serializes the values, sees
 * them. They document the data: nothing enforces them.
 */
public final class JsonSchemas {

    private static final String DRAFT = "http://json-schema.org/draft-07/schema#";

    private JsonSchemas() {}

    /** A task's input, or null when its parameter names are not known. */
    public static Map<String, Object> taskInput(Method method) {
        Map<String, Object> properties = new LinkedHashMap<>();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(WorkflowInstanceIdInputParam.class)) {
                continue; // filled from the task, not from the input
            }
            String name = nameOf(params[i]);
            if (name == null) {
                return null;
            }
            properties.put(name, of(params[i].getParameterizedType()));
        }
        return document(object(properties));
    }

    /** A task's output, or null when nothing can be said of it (no value, or a raw result). */
    public static Map<String, Object> taskOutput(Method method) {
        Type type = method.getGenericReturnType();
        Class<?> raw = method.getReturnType();
        if (raw == void.class || raw == Void.class || TaskResult.class.isAssignableFrom(raw) || raw == Object.class) {
            return null;
        }
        OutputParam named = method.getAnnotatedReturnType().getAnnotation(OutputParam.class);
        if (named != null) {
            return document(object(Map.of(named.value(), of(type))));
        }
        return document(spread(type));
    }

    /** A workflow's input, as the SDK starts it: a single object's properties, else one per parameter. */
    public static Map<String, Object> workflowInput(Method method) {
        if (method.getParameterCount() == 1 && !Json.isWrapped(method.getGenericParameterTypes()[0])) {
            return document(of(method.getGenericParameterTypes()[0]));
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Parameter p : method.getParameters()) {
            String name = nameOf(p);
            if (name == null) {
                return null;
            }
            properties.put(name, of(p.getParameterizedType()));
        }
        return document(object(properties));
    }

    /** A workflow's output: what its code returns, as the engine records it. */
    public static Map<String, Object> workflowOutput(Method method) {
        Class<?> raw = method.getReturnType();
        if (raw == void.class || raw == Void.class || raw == Object.class) {
            return null;
        }
        return document(spread(method.getGenericReturnType()));
    }

    /** The schema of one value of this type. */
    public static Map<String, Object> of(Type type) {
        return of(Json.MAPPER.getTypeFactory().constructType(type), new HashSet<>());
    }

    // ------------------------------------------------------------------ shapes

    /** An object spreads into its properties; any other value is {@code result}. */
    private static Map<String, Object> spread(Type type) {
        if (Json.isWrapped(type)) {
            return object(Map.of("result", of(type)));
        }
        return of(type);
    }

    private static Map<String, Object> of(JavaType type, Set<JavaType> visiting) {
        Class<?> raw = type.getRawClass();
        if (type.isReferenceType()) { // Optional<T> and the like: the value, when there is one
            return of(type.getReferencedType(), visiting);
        }
        if (raw == String.class || CharSequence.class.isAssignableFrom(raw) || raw == char.class
                || raw == Character.class) {
            return Map.of("type", "string");
        }
        if (raw == java.util.UUID.class) {
            return Map.of("type", "string", "format", "uuid");
        }
        if (raw == boolean.class || raw == Boolean.class) {
            return Map.of("type", "boolean");
        }
        if (raw == byte.class || raw == short.class || raw == int.class || raw == long.class || raw == Byte.class
                || raw == Short.class || raw == Integer.class || raw == Long.class
                || raw == java.math.BigInteger.class) {
            return Map.of("type", "integer");
        }
        if (raw.isPrimitive() || Number.class.isAssignableFrom(raw)) {
            return Map.of("type", "number");
        }
        if (raw.isEnum()) {
            return Map.of("type", "string", "enum", enumValues(raw));
        }
        if (raw == java.time.LocalDate.class) {
            return Map.of("type", "string", "format", "date");
        }
        if (raw == java.time.LocalTime.class || raw == java.time.OffsetTime.class) {
            return Map.of("type", "string", "format", "time");
        }
        if (java.time.temporal.Temporal.class.isAssignableFrom(raw) || raw == java.util.Date.class) {
            return Map.of("type", "string", "format", "date-time");
        }
        if (raw == java.time.Duration.class) {
            return Map.of("type", "string", "format", "duration");
        }
        if (type.isArrayType() || type.isCollectionLikeType()) {
            return Map.of("type", "array", "items", of(type.getContentType(), visiting));
        }
        if (type.isMapLikeType()) {
            return Map.of("type", "object", "additionalProperties", of(type.getContentType(), visiting));
        }
        if (raw == Object.class || raw.isInterface() || java.lang.reflect.Modifier.isAbstract(raw.getModifiers())) {
            return Map.of(); // any value
        }
        if (!visiting.add(type)) {
            return Map.of("type", "object"); // a type that holds itself: not unrolled again
        }
        try {
            Map<String, Object> properties = new LinkedHashMap<>();
            List<BeanPropertyDefinition> props = Json.MAPPER.getSerializationConfig().introspect(type).findProperties();
            for (BeanPropertyDefinition p : props) {
                if (p.couldSerialize()) {
                    properties.put(p.getName(), of(p.getPrimaryType(), visiting));
                }
            }
            return object(properties);
        } finally {
            visiting.remove(type);
        }
    }

    private static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    private static Map<String, Object> document(Map<String, Object> schema) {
        if (schema == null) {
            return null;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("$schema", DRAFT);
        doc.putAll(schema);
        return doc;
    }

    private static List<String> enumValues(Class<?> type) {
        return java.util.Arrays.stream(type.getEnumConstants())
                .map(c -> Json.MAPPER.convertValue(c, String.class))
                .toList();
    }

    /** As {@link MethodRefs#parameterNames}: {@code @InputParam}, else the compiled name; null if neither. */
    private static String nameOf(Parameter p) {
        com.netflix.conductor.sdk.workflow.task.InputParam ip =
                p.getAnnotation(com.netflix.conductor.sdk.workflow.task.InputParam.class);
        if (ip != null) {
            return ip.value();
        }
        return p.isNamePresent() ? p.getName() : null;
    }
}
