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
package org.conductoross.conductor.ai.examples.adk;

import java.util.Map;

import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.model.AgentResult;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.ThinkingConfig;

/**
 * Example Adk 30 — Thinking Config
 *
 * <p>Java port of <code>sdk/python/examples/adk/30_thinking_config.py</code>.
 *
 * <p>Demonstrates: ADK's extended thinking mode via the native
 * {@link ThinkingConfig} embedded in {@link GenerateContentConfig}.
 */
public class Example30ThinkingConfig {

    @Schema(description = "Evaluate a mathematical expression.")
    public static Map<String, Object> calculate(
            @Schema(name = "expression", description = "Math expression") String expression) {
        // Safe-only digits + basic operators evaluator.
        if (!expression.matches("[0-9+\\-*/().\\s]+")) {
            return Map.of("expression", expression, "error", "Invalid expression");
        }
        try {
            double result = evalExpr(expression.replaceAll("\\s+", ""), new int[]{0});
            return Map.of("expression", expression, "result", result);
        } catch (Exception e) {
            return Map.of("expression", expression, "error", e.getMessage());
        }
    }

    private static double evalExpr(String s, int[] pos) {
        double val = evalTerm(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '+' || s.charAt(pos[0]) == '-')) {
            char op = s.charAt(pos[0]++);
            val = op == '+' ? val + evalTerm(s, pos) : val - evalTerm(s, pos);
        }
        return val;
    }

    private static double evalTerm(String s, int[] pos) {
        double val = evalFactor(s, pos);
        while (pos[0] < s.length() && (s.charAt(pos[0]) == '*' || s.charAt(pos[0]) == '/')) {
            char op = s.charAt(pos[0]++);
            val = op == '*' ? val * evalFactor(s, pos) : val / evalFactor(s, pos);
        }
        return val;
    }

    private static double evalFactor(String s, int[] pos) {
        if (pos[0] < s.length() && s.charAt(pos[0]) == '(') {
            pos[0]++;
            double val = evalExpr(s, pos);
            pos[0]++; // ')'
            return val;
        }
        int start = pos[0];
        while (pos[0] < s.length() && (Character.isDigit(s.charAt(pos[0])) || s.charAt(pos[0]) == '.')) pos[0]++;
        return Double.parseDouble(s.substring(start, pos[0]));
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        LlmAgent thinker = LlmAgent.builder()
            .name("deep_thinker")
            .description("Analytical assistant with extended thinking enabled for step-by-step reasoning.")
            .model(Settings.LLM_MODEL)
            .instruction(
                "You are an analytical assistant. Think carefully through complex "
                + "problems step by step. Use the calculate tool for math.")
            .generateContentConfig(GenerateContentConfig.builder()
                .thinkingConfig(ThinkingConfig.builder()
                    .thinkingBudget(2048)
                    .build())
                .build())
            .tools(FunctionTool.create(Example30ThinkingConfig.class, "calculate"))
            .build();

        AgentResult result = runtime.run(thinker,
            "If a train travels 120 km in 2 hours, then speeds up by 50% for "
            + "the next 3 hours, what is the total distance traveled?");
        result.printResult();

        runtime.shutdown();
    }
}
