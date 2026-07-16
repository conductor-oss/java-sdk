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
package org.conductoross.conductor.ai.examples;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.enums.Position;
import org.conductoross.conductor.ai.internal.ToolRegistry;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.model.GuardrailDef;
import org.conductoross.conductor.ai.model.GuardrailResult;

/**
 * Example 37 — Fix Guardrail (auto-correct output instead of retrying)
 *
 * <p>Demonstrates {@code OnFail.FIX}: when the guardrail fails, it provides
 * a corrected version of the output via {@code GuardrailResult.fixedOutput()}.
 * The workflow uses the fixed output directly without calling the LLM again.
 *
 * <p>Comparison of on_fail modes:
 * <ul>
 *   <li>{@code RETRY} — send feedback to LLM and regenerate (best for style issues)</li>
 *   <li>{@code FIX}   — replace output with corrected version (deterministic fixes)</li>
 *   <li>{@code RAISE} — terminate the workflow with an error</li>
 * </ul>
 */
public class Example37FixGuardrail {

    static class ContactTools {
        @Tool(name = "get_contact_info", description = "Look up contact information for a person")
        public Map<String, Object> getContactInfo(String name) {
            Map<String, Map<String, Object>> contacts = Map.of(
                "alice", Map.of(
                    "name", "Alice Johnson",
                    "email", "alice@example.com",
                    "phone", "(555) 123-4567",
                    "department", "Engineering"
                ),
                "bob", Map.of(
                    "name", "Bob Smith",
                    "email", "bob@example.com",
                    "phone", "555-987-6543",
                    "department", "Marketing"
                )
            );
            String key = name.toLowerCase().split("\\s+")[0];
            return contacts.getOrDefault(key, Map.of("error", "No contact found for '" + name + "'"));
        }
    }

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(?:\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        // ── Fix guardrail: redact phone numbers ────────────────────────────
        // Instead of asking the LLM to retry, auto-redact and return the fix.

        GuardrailDef phoneRedactor = GuardrailDef.builder()
            .name("phone_redactor")
            .position(Position.OUTPUT)
            .onFail(OnFail.FIX)
            .func(content -> {
                Matcher m = PHONE_PATTERN.matcher(content);
                if (m.find()) {
                    String redacted = m.replaceAll("[PHONE REDACTED]");
                    return GuardrailResult.fix(redacted);
                }
                return GuardrailResult.pass();
            })
            .build();

        Agent agent = Agent.builder()
            .name("directory_agent")
            .model(Settings.LLM_MODEL)
            .tools(ToolRegistry.fromInstance(new ContactTools()))
            .instructions(
                "You are a company directory assistant. When asked about employees, "
                + "look up their contact info and share everything you find.")
            .guardrails(List.of(phoneRedactor))
            .build();

        System.out.println("=== Scenario 1: Contact with phone number (guardrail triggers) ===");
        AgentResult r1 = runtime.run(agent, "What's Alice Johnson's contact information?");
        r1.printResult();

        System.out.println("\n=== Scenario 2: Department only (guardrail does not trigger) ===");
        AgentResult r2 = runtime.run(agent, "What department does Alice work in? Just the department name.");
        r2.printResult();

        runtime.shutdown();
    }
}
