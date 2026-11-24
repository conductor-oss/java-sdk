/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;

/**
 * Singleton ObjectMapper factory with consistent configuration.
 */
public class JsonMapper {

    // Jackson 3 mappers are immutable: all configuration goes through the builder. The jdk8 and
    // java.time datatypes are built into core now, so no module registration is needed. Dates are
    // written as ISO-8601 strings (WRITE_DATES_AS_TIMESTAMPS disabled -- the Jackson 3 default,
    // kept explicit to preserve the behavior this mapper had on Jackson 2).
    private static final ObjectMapper INSTANCE =
            tools.jackson.databind.json.JsonMapper.builder()
                    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .changeDefaultPropertyInclusion(
                            value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
                    .build();

    private JsonMapper() {}

    /** Get the shared ObjectMapper instance. */
    public static ObjectMapper get() {
        return INSTANCE;
    }

    /**
     * Serialize an object to a JSON string.
     *
     * @param obj the object to serialize
     * @return JSON string
     */
    public static String toJson(Object obj) {
        try {
            return INSTANCE.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Deserialize a JSON string to the given type.
     *
     * @param json the JSON string
     * @param cls  the target class
     * @param <T>  the target type
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Class<T> cls) {
        try {
            return INSTANCE.readValue(json, cls);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
