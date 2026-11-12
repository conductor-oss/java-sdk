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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON conventions shared by workflow code, workers and the client. Task output follows the same
 * rule as annotated workers: a scalar, string, enum or collection is {@code {"result": value}}, a
 * map is the output itself, and any other object is converted to a map of its properties.
 */
public final class Json {

    public static final ObjectMapper MAPPER = create();

    private Json() {}

    private static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /** Converts a value to plain JSON structures: maps, lists, strings, numbers, booleans. */
    public static Object plain(Object value) {
        return value == null ? null : MAPPER.convertValue(value, Object.class);
    }

    public static <T> T convert(Object value, Type type) {
        if (value == null) {
            return null;
        }
        JavaType javaType = MAPPER.getTypeFactory().constructType(type);
        return MAPPER.convertValue(value, javaType);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        return MAPPER.convertValue(value, LinkedHashMap.class);
    }

    /** Whether values of this type travel as {@code {"result": value}}. */
    public static boolean isWrapped(Type type) {
        Class<?> raw = raw(type);
        return raw.isPrimitive()
                || CharSequence.class.isAssignableFrom(raw)
                || Number.class.isAssignableFrom(raw)
                || Boolean.class == raw
                || Character.class == raw
                || raw.isEnum()
                || Collection.class.isAssignableFrom(raw)
                || raw.isArray()
                || java.time.temporal.Temporal.class.isAssignableFrom(raw);
    }

    /** Encodes a task result or workflow result as an output map. */
    public static Map<String, Object> encodeOutput(Object value) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value == null) {
            return out;
        }
        if (value instanceof Map<?, ?> m) {
            m.forEach((k, v) -> out.put(String.valueOf(k), plain(v)));
            return out;
        }
        if (isWrapped(value.getClass())) {
            out.put("result", plain(value));
            return out;
        }
        return map(value);
    }

    /** Decodes an output map produced by {@link #encodeOutput} into the given type. */
    public static Object decodeOutput(Map<String, Object> output, Type type) {
        Class<?> raw = raw(type);
        if (raw == void.class || raw == Void.class) {
            return null;
        }
        if (output == null) {
            return null;
        }
        if (isWrapped(type)) {
            return convert(output.get("result"), type);
        }
        if (Object.class == raw) {
            return output.containsKey("result") && output.size() == 1 ? output.get("result") : output;
        }
        try {
            return convert(output, type);
        } catch (IllegalArgumentException e) {
            // a value that is not a JSON object (a UUID, say) is carried under "result"
            if (output.size() == 1 && output.containsKey("result")) {
                return convert(output.get("result"), type);
            }
            throw e;
        }
    }

    public static Class<?> raw(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType p) {
            return (Class<?>) p.getRawType();
        }
        return Object.class;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot serialize " + value, e);
        }
    }
}
