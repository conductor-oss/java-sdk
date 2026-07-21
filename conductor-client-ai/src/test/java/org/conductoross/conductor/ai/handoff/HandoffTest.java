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
package org.conductoross.conductor.ai.handoff;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for handoff routing rules. */
class HandoffTest {

    @Test
    void textMentionCapturesTriggerAndTarget() {
        OnTextMention h = OnTextMention.of("reverse", "text_agent");
        assertEquals("reverse", h.getText());
        assertEquals("text_agent", h.getTarget());
    }

    @Test
    void toolResultWithContains() {
        OnToolResult h = OnToolResult.of("calc", "math_agent", "42");
        assertEquals("calc", h.getToolName());
        assertEquals("math_agent", h.getTarget());
        assertEquals("42", h.getResultContains());
    }

    @Test
    void toolResultTwoArg() {
        OnToolResult h = OnToolResult.of("calc", "math_agent");
        assertEquals("calc", h.getToolName());
        assertEquals("math_agent", h.getTarget());
    }

    @Test
    void onConditionPredicateEvaluates() {
        OnCondition h = new OnCondition("router", m -> "go".equals(m.get("k")));
        assertEquals("router", h.getTarget());
        assertTrue(h.getCondition().apply(Map.of("k", "go")));
        assertFalse(h.getCondition().apply(Map.of("k", "stop")));
    }
}
