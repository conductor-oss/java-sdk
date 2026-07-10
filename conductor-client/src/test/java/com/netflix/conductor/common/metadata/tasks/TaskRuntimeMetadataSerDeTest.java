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
package com.netflix.conductor.common.metadata.tasks;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The server delivers host-resolved secret values on {@code Task.runtimeMetadata} (wire-only, never
 * persisted) when a worker's {@code TaskDef.runtimeMetadata} declares secret names (conductor-oss PR
 * #1255). This verifies the client model round-trips the field and omits it when empty.
 */
public class TaskRuntimeMetadataSerDeTest {

    private ObjectMapper mapper() {
        ObjectMapper m = new ObjectMapper();
        m.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        m.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        m.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return m;
    }

    @Test
    public void runtimeMetadata_roundTrips() throws Exception {
        ObjectMapper mapper = mapper();
        Task task = new Task();
        task.setRuntimeMetadata(Map.of("GITHUB_TOKEN", "ghp_secret", "GH_APP_ID", "42"));

        String json = mapper.writeValueAsString(task);
        assertTrue(json.contains("\"runtimeMetadata\""), "field must serialize under the wire key");

        Task back = mapper.readValue(json, Task.class);
        assertEquals("ghp_secret", back.getRuntimeMetadata().get("GITHUB_TOKEN"));
        assertEquals("42", back.getRuntimeMetadata().get("GH_APP_ID"));
    }

    @Test
    public void runtimeMetadata_omittedWhenEmpty() throws Exception {
        String json = mapper().writeValueAsString(new Task());
        assertFalse(json.contains("runtimeMetadata"), "empty/absent map must not appear on the wire");
    }
}
