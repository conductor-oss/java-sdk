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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.conductoross.conductor.ai.model.GuardrailDef;
import org.conductoross.conductor.ai.model.GuardrailResult;

/** Creates the common worker handler used by agent- and tool-scoped local guardrails. */
public final class GuardrailHandlerFactory {
    private GuardrailHandlerFactory() {}

    /**
     * Creates a handler which evaluates local guardrails in declaration order and returns the
     * first failure. Exceptions from user functions deliberately propagate to the worker runner,
     * where they are reported as failed tasks.
     */
    public static Function<Map<String, Object>, Object> create(List<GuardrailDef> guardrails) {
        List<GuardrailDef> localGuardrails = List.copyOf(guardrails);
        return inputData -> {
            Object rawContent = inputData.get("content");
            String content = rawContent != null ? rawContent.toString() : "";
            int iteration = inputData.get("iteration") instanceof Number
                    ? ((Number) inputData.get("iteration")).intValue()
                    : 0;
            for (GuardrailDef guardrail : localGuardrails) {
                GuardrailResult result = guardrail.getFunc().apply(content);
                if (result == null) {
                    throw new IllegalStateException("Guardrail '" + guardrail.getName() + "' returned null");
                }
                if (!result.isPassed()) {
                    String onFail = guardrail.getOnFail().toJsonValue();
                    String fixedOutput = result.getFixedOutput();
                    if ("retry".equals(onFail) && iteration >= guardrail.getMaxRetries()) onFail = "raise";
                    if ("fix".equals(onFail) && fixedOutput == null) onFail = "raise";
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("passed", false);
                    out.put("message", result.getMessage() != null ? result.getMessage() : "");
                    out.put("on_fail", onFail);
                    out.put("fixed_output", fixedOutput);
                    out.put("guardrail_name", guardrail.getName());
                    out.put("should_continue", "retry".equals(onFail));
                    return out;
                }
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("passed", true);
            out.put("message", "");
            out.put("on_fail", "pass");
            out.put("fixed_output", null);
            out.put("guardrail_name", "");
            out.put("should_continue", false);
            return out;
        };
    }
}
