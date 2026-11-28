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
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
                // values JSON writes as a string or a number, not as an object
                || Temporal.class.isAssignableFrom(raw)
                || TemporalAmount.class.isAssignableFrom(raw)
                || Date.class.isAssignableFrom(raw)
                || UUID.class == raw;
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
        // an object spreads into the output; any other value (a scalar, a list, or a type JSON
        // writes as one value, such as a UUID) is the result
        Object plain = plain(value);
        if (!isWrapped(value.getClass()) && plain instanceof Map<?, ?> m) {
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        out.put("result", plain);
        return out;
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

    /**
     * A value as it is recorded for a re-run to read back: its JSON, and the type to read that
     * JSON as. The type is the value's class, with the element types of a list, set or map taken
     * from its elements, since a re-run has only the JSON: a {@code List<Order>} is recorded as a
     * list of {@code Order}, not as the list class that happened to hold it.
     */
    public static Map<String, Object> record(Object value) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("value", plain(value));
        if (value != null) {
            record.put("type", typeOf(value).toCanonical());
        }
        return record;
    }

    /** Reads back a value recorded with {@link #record}; classes are found through {@code loader}. */
    public static Object recorded(Map<?, ?> record, ClassLoader loader) {
        Object value = record.get("value");
        Object type = record.get("type");
        if (value == null || type == null) {
            return value;
        }
        JavaType javaType;
        try {
            javaType = MAPPER.getTypeFactory().withClassLoader(loader).constructFromCanonical(type.toString());
        } catch (IllegalArgumentException e) {
            return value; // a class this build no longer has: the plain JSON value
        }
        return MAPPER.convertValue(value, javaType);
    }

    /** The type to read a value's JSON back as. */
    private static JavaType typeOf(Object value) {
        TypeFactory types = MAPPER.getTypeFactory();
        if (value instanceof Map<?, ?> m) {
            return types.constructMapType(LinkedHashMap.class, common(m.keySet()), common(m.values()));
        }
        if (value instanceof Collection<?> c) {
            @SuppressWarnings("rawtypes") // what TypeFactory.constructCollectionType takes
            Class<? extends Collection> holder = value instanceof SortedSet ? TreeSet.class
                    : value instanceof Set ? LinkedHashSet.class
                    : ArrayList.class;
            return types.constructCollectionType(holder, common(c));
        }
        // an enum constant with a body is its own class: the enum is what reads it back
        return types.constructType(value instanceof Enum<?> e ? e.getDeclaringClass() : value.getClass());
    }

    /** The one type all of these values are read back as; any JSON value when they differ. */
    private static JavaType common(Collection<?> values) {
        JavaType common = null;
        for (Object v : values) {
            if (v == null) {
                continue;
            }
            JavaType type = typeOf(v);
            if (common != null && !common.equals(type)) {
                return MAPPER.getTypeFactory().constructType(Object.class);
            }
            common = type;
        }
        return common == null ? MAPPER.getTypeFactory().constructType(Object.class) : common;
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
}
