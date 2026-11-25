/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.common.config;

import java.io.InputStream;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.module.kotlin.KotlinModule;

public class ObjectMapperProvider {

    // Keep in sync with jacksonBom in versions.gradle: the jackson-annotations release that
    // the Jackson 3 engine was built against.
    private static final String MIN_JACKSON_ANNOTATIONS = "2.21";

    private static final ObjectMapper objectMapper = _getObjectMapper();

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Jackson 3 mappers are immutable, so every setting has to be applied to the builder before the
     * mapper is built. The jdk8 and java.time datatypes and the property-access optimisations that
     * used to need separate modules (jsr310, afterburner) are part of the core now, which is why
     * only the Kotlin module is registered here.
     *
     * <p>WRITE_DATES_AS_TIMESTAMPS is enabled to keep the numeric date encoding that Jackson 2
     * produced with JavaTimeModule. Jackson 3 defaults to ISO-8601 strings, which would change the
     * payload format seen by the server and stored task/workflow documents.
     *
     * <p>The enum, duration, getter-as-setter and final-field settings restore other Jackson 2
     * defaults, so worker inputs and outputs keep the shape they had with SDK 6.x.
     */
    private static ObjectMapper _getObjectMapper() {
        try {
            JsonMapper.Builder builder =
                    JsonMapper.builder()
                            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                            .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                            .disable(
                                    EnumFeature.READ_ENUMS_USING_TO_STRING,
                                    EnumFeature.WRITE_ENUMS_USING_TO_STRING)
                            .enable(
                                    MapperFeature.USE_GETTERS_AS_SETTERS,
                                    MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                            .changeDefaultPropertyInclusion(
                                    value ->
                                            JsonInclude.Value.construct(
                                                    JsonInclude.Include.NON_NULL,
                                                    JsonInclude.Include.ALWAYS))
                            .addModule(new KotlinModule.Builder().build());
            SimpleModule jackson2JsonNode = jackson2JsonNodeModule();
            if (jackson2JsonNode != null) {
                builder.addModule(jackson2JsonNode);
            }
            ObjectMapper mapper = builder.build();
            selfCheck(mapper);
            return mapper;
        } catch (RuntimeException | LinkageError e) {
            LinkageError missing = findLinkageError(e);
            if (missing == null) {
                throw e;
            }
            throw new IllegalStateException(incompatibleJacksonMessage(missing), e);
        }
    }

    /**
     * Some classes Jackson needs are only loaded the first time it inspects a bean, so convert
     * one here. An incompatible classpath then fails at startup, not on the first task.
     */
    private static void selfCheck(ObjectMapper mapper) {
        SelfCheck value = new SelfCheck();
        value.value = "ok";
        mapper.readValue(mapper.writeValueAsString(value), SelfCheck.class);
    }

    public static class SelfCheck {
        public String value;
    }

    /**
     * Workers often put a Jackson 2 JsonNode (for example a REST client response) into task output.
     * Jackson 3 does not know that type and would write its getters, so write the JSON it holds.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SimpleModule jackson2JsonNodeModule() {
        Class<?> nodeType;
        try {
            nodeType =
                    Class.forName(
                            "com.fasterxml.jackson.databind.JsonNode",
                            false,
                            ObjectMapperProvider.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
        SimpleModule module = new SimpleModule("conductor-jackson2-json-node");
        module.addSerializer((Class) nodeType, new Jackson2JsonNodeSerializer());
        return module;
    }

    private static class Jackson2JsonNodeSerializer extends ValueSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
            ctxt.writeValue(gen, JsonMapper.shared().readTree(value.toString()));
        }
    }

    private static LinkageError findLinkageError(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof LinkageError linkageError) {
                return linkageError;
            }
        }
        return null;
    }

    private static String incompatibleJacksonMessage(LinkageError e) {
        String missing = String.valueOf(e.getMessage());
        String hint;
        if (missing.startsWith("com/fasterxml/jackson/annotation/")) {
            hint = "jackson-annotations is older than this Jackson version needs.";
        } else if (missing.startsWith("kotlin/")) {
            hint = "kotlin-stdlib is older than this Jackson version needs.";
        } else {
            hint = "A Jackson dependency is older than this Jackson version needs.";
        }
        return "Conductor SDK could not start Jackson: missing class "
                + missing
                + ". "
                + hint
                + " Found jackson-databind "
                + mavenVersion("tools.jackson.core", "jackson-databind")
                + ", jackson-annotations "
                + mavenVersion("com.fasterxml.jackson.core", "jackson-annotations")
                + ", kotlin-stdlib "
                + kotlinVersion()
                + ". A dependency BOM most likely forced an older version; in a Spring Boot 3 app,"
                + " Boot's BOM manages jackson-annotations and Kotlin. This SDK needs"
                + " jackson-annotations "
                + MIN_JACKSON_ANNOTATIONS
                + " or later: use Spring Boot 3.5.13 or later, or set the jackson-bom.version"
                + " property to "
                + MIN_JACKSON_ANNOTATIONS
                + ".x.";
    }

    private static String mavenVersion(String group, String artifact) {
        String path = "META-INF/maven/" + group + "/" + artifact + "/pom.properties";
        try (InputStream in = ObjectMapperProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (Exception | LinkageError ignored) {
            return "unknown";
        }
    }

    private static String kotlinVersion() {
        try {
            String version = Class.forName("kotlin.Unit").getPackage().getImplementationVersion();
            return version == null ? "unknown" : version;
        } catch (Exception | LinkageError ignored) {
            return "not on classpath";
        }
    }
}
