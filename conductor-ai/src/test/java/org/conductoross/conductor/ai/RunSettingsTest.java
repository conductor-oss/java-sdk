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
package org.conductoross.conductor.ai;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec R8 (T12/T13): per-run overrides map to the exact {@code agentConfig}
 * wire keys the Python SDK uses, with a non-null gate so zero values apply.
 */
class RunSettingsTest {

    @Test
    void fullOverridesMapToWireKeys() {
        Map<String, Object> overrides = new RunSettings()
                .model("openai/gpt-4o")
                .temperature(0.7)
                .maxTokens(2048)
                .reasoningEffort("high")
                .thinkingBudgetTokens(512)
                .toConfigOverrides();

        assertEquals("openai/gpt-4o", overrides.get("model"));
        assertEquals(0.7, overrides.get("temperature"));
        assertEquals(2048, overrides.get("maxTokens"));
        assertEquals("high", overrides.get("reasoningEffort"));
        assertEquals(Map.of("enabled", true, "budgetTokens", 512), overrides.get("thinkingConfig"));
    }

    @Test
    void onlySetFieldsAppear() {
        Map<String, Object> overrides = new RunSettings().model("openai/gpt-4o-mini").toConfigOverrides();

        assertEquals(Map.of("model", "openai/gpt-4o-mini"), overrides);
    }

    @Test
    void zeroValuesAreHonored() {
        Map<String, Object> overrides = new RunSettings()
                .temperature(0.0)
                .maxTokens(0)
                .toConfigOverrides();

        assertEquals(0.0, overrides.get("temperature"), "the gate is != null, not truthiness");
        assertEquals(0, overrides.get("maxTokens"));
    }

    @Test
    void emptySettingsProduceNoOverrides() {
        assertTrue(new RunSettings().toConfigOverrides().isEmpty());
    }

    @Test
    void noTopPKnob() {
        // The spec deliberately omits topP from RunSettings — guard against
        // someone "helpfully" adding it back.
        boolean hasTopP = false;
        for (java.lang.reflect.Method m : RunSettings.class.getMethods()) {
            if (m.getName().toLowerCase().contains("topp")) {
                hasTopP = true;
                break;
            }
        }
        assertFalse(hasTopP, "RunSettings must not expose topP (spec R8)");
    }
}
