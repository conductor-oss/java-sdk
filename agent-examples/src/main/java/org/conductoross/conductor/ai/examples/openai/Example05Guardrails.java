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
package org.conductoross.conductor.ai.examples.openai;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.annotations.Tool;
import org.conductoross.conductor.ai.examples.Settings;
import org.conductoross.conductor.ai.frameworks.OpenAIAgent;
import org.conductoross.conductor.ai.model.AgentResult;

/**
 * Example OpenAi 05 — Guardrails
 *
 * <p>Java port of <code>sdk/python/examples/openai/05_guardrails.py</code>.
 *
 * <p>Demonstrates: a banking assistant that uses function tools to look up
 * balances and transfer funds. The Python original wraps the agent with
 * input + output guardrails (PII regex on input, forbidden-phrase scan on
 * output).
 *
 * <p>Python parity gap: the current {@link OpenAIAgent} builder does not
 * expose {@code input_guardrails} / {@code output_guardrails}. The generic
 * {@code Agent.Builder} has a {@code .guardrails(...)} hook but it is not
 * surfaced on the OpenAIAgent factory. We port the tool surface and agent
 * shape faithfully; the guardrail wrappers are described here for parity
 * but not wired:
 * <ul>
 *   <li>Input guardrail: reject messages containing an SSN regex
 *       ({@code \b\d{3}-\d{2}-\d{4}\b}) or a credit-card regex
 *       ({@code \b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b}).</li>
 *   <li>Output guardrail: reject responses mentioning any of
 *       "internal system", "database password", "api key", "secret token".</li>
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL=http://localhost:8080/api</li>
 *   <li>CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini</li>
 * </ul>
 */
public class Example05Guardrails {

    static class BankingTools {

        @Tool(name = "get_account_balance", description = "Look up the balance of a bank account.")
        public String getAccountBalance(String account_id) {
            switch (account_id) {
                case "ACC-100": return "$5,230.00";
                case "ACC-200": return "$12,750.50";
                case "ACC-300": return "$890.25";
                default: return "Account " + account_id + " not found";
            }
        }

        @Tool(name = "transfer_funds", description = "Transfer funds between accounts.")
        public String transferFunds(String from_account, String to_account, double amount) {
            return String.format("Transferred $%.2f from %s to %s.", amount, from_account, to_account);
        }
    }

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Agent agent = OpenAIAgent.builder()
                .name("banking_assistant")
                .instructions(
                        "You are a secure banking assistant. Help users check account balances "
                                + "and transfer funds. Never reveal internal system details.")
                .model(Settings.LLM_MODEL)
                .tools(new BankingTools())
                .build();

        // This should pass guardrails (no PII, no forbidden phrases in response).
        AgentResult result = runtime.run(agent, "What's the balance on account ACC-100?");
        result.printResult();

        runtime.shutdown();
    }
}
