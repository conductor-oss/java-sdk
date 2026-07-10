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
package org.conductoross.conductor.ai.plans;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpTest {

    @Test
    void rejectsNeitherArgsNorGenerate() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class, () -> Op.builder("write_file").build());
        assertTrue(e.getMessage().contains("exactly one of args or generate"), "message was: " + e.getMessage());
    }

    @Test
    void rejectsBothArgsAndGenerate() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Op.builder("write_file")
                .args(Map.of("path", "x"))
                .generate(Generate.builder()
                        .instructions("i")
                        .outputSchema("{\"x\":1}")
                        .build())
                .build());
        assertTrue(e.getMessage().contains("exactly one of args or generate"), "message was: " + e.getMessage());
    }

    @Test
    void acceptsArgsOnly() {
        Op op = Op.builder("write_file").args(Map.of("path", "x")).build();
        Map<String, Object> j = op.toJson();
        assertEquals("write_file", j.get("tool"));
        assertNotNull(j.get("args"));
    }

    @Test
    void acceptsGenerateOnly() {
        Op op = Op.builder("write_file")
                .generate(Generate.builder()
                        .instructions("i")
                        .outputSchema("{\"x\":1}")
                        .build())
                .build();
        Map<String, Object> j = op.toJson();
        assertEquals("write_file", j.get("tool"));
        assertNotNull(j.get("generate"));
    }
}
