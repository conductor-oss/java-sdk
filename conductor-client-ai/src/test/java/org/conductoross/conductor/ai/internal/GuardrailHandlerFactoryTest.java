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
package org.conductoross.conductor.ai.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.model.GuardrailDef;
import org.conductoross.conductor.ai.model.GuardrailResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Server-free contract tests for the shared local guardrail worker handler. */
class GuardrailHandlerFactoryTest {

    private static GuardrailDef guard(String name, OnFail onFail, Function<String, GuardrailResult> function) {
        return GuardrailDef.builder().name(name).onFail(onFail).func(function).build();
    }

    @Test
    void passes_when_every_guardrail_passes() {
        Map<String, Object> out = output(GuardrailHandlerFactory.create(List.of(
                guard("first", OnFail.RAISE, value -> GuardrailResult.pass()))).apply(Map.of()));
        assertTrue((Boolean) out.get("passed"));
        assertEquals("pass", out.get("on_fail"));
    }

    @Test
    void returns_first_failure_in_declaration_order() {
        AtomicInteger secondCalls = new AtomicInteger();
        Map<String, Object> out = output(GuardrailHandlerFactory.create(List.of(
                guard("first", OnFail.RAISE, value -> GuardrailResult.fail("first failure")),
                guard("second", OnFail.RAISE, value -> {
                    secondCalls.incrementAndGet();
                    return GuardrailResult.fail("second failure");
                }))).apply(Map.of()));
        assertFalse((Boolean) out.get("passed"));
        assertEquals("first", out.get("guardrail_name"));
        assertEquals(0, secondCalls.get());
    }

    @Test
    void preserves_retry_fix_and_human_semantics() {
        Map<String, Object> retry = output(GuardrailHandlerFactory.create(List.of(
                guard("retry", OnFail.RETRY, value -> GuardrailResult.fail("no")))).apply(Map.of("content", "x", "iteration", 0)));
        assertEquals("retry", retry.get("on_fail"));
        assertTrue((Boolean) retry.get("should_continue"));

        Map<String, Object> escalated = output(GuardrailHandlerFactory.create(List.of(
                GuardrailDef.builder().name("retry").onFail(OnFail.RETRY).maxRetries(1)
                        .func(value -> GuardrailResult.fail("no")).build())).apply(Map.of("iteration", 1)));
        assertEquals("raise", escalated.get("on_fail"));

        Map<String, Object> fix = output(GuardrailHandlerFactory.create(List.of(
                guard("fix", OnFail.FIX, value -> GuardrailResult.fix("clean")))).apply(Map.of()));
        assertEquals("fix", fix.get("on_fail"));
        assertEquals("clean", fix.get("fixed_output"));

        Map<String, Object> human = output(GuardrailHandlerFactory.create(List.of(
                guard("human", OnFail.HUMAN, value -> GuardrailResult.fail("review")))).apply(Map.of()));
        assertEquals("human", human.get("on_fail"));
    }

    @Test
    void thrown_or_null_guardrail_result_fails_the_task() {
        assertThrows(RuntimeException.class, () -> GuardrailHandlerFactory.create(List.of(
                guard("throws", OnFail.RAISE, value -> { throw new IllegalStateException("boom"); }))).apply(Map.of()));
        assertThrows(IllegalStateException.class, () -> GuardrailHandlerFactory.create(List.of(
                guard("null", OnFail.RAISE, value -> null))).apply(Map.of()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> output(Object value) {
        return (Map<String, Object>) value;
    }
}
