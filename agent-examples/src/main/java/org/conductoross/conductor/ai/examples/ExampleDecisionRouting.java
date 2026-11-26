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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.Javascript;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.ApiClient;

/** Route a request through one synchronous decision SWITCH and the selected INLINE branch. */
public class ExampleDecisionRouting {
    public static ConductorWorkflow<Map<String, Object>> createWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("ai_decision_routing_java");
        workflow.setVersion(1);
        Switch decision = new Switch("route_request", "route")
                .evaluatorType(Switch.DECISION_NAME)
                .input("model", "jev-1.13")
                .input("state", "${workflow.input.request}")
                .input("questions", Map.of("route", Map.of(
                        "type", "choice",
                        "instructions", "Choose the team best suited to handle this request.",
                        "choices", Map.of(
                                "billing", "Payments, invoices, refunds, or subscriptions.",
                                "technical", "Errors, outages, or product troubleshooting."))));
        String provider = System.getenv("CONDUCTOR_DECISION_PROVIDER");
        if (provider != null) decision.input("provider", provider);
        Javascript billing = new Javascript("handle_billing",
                "({team: 'billing', nextAction: 'review_invoice', "
                        + "message: 'Check invoice line items and payment records.', request: $.request})")
                .input("request", "${workflow.input.request}");
        Javascript technical = new Javascript("handle_technical",
                "({team: 'technical', nextAction: 'collect_diagnostics', "
                        + "message: 'Collect error logs and steps to reproduce.', request: $.request})")
                .input("request", "${workflow.input.request}");
        decision.switchCase("billing", billing).switchCase("technical", technical);
        Javascript selected = new Javascript("selected_result", "$.billing || $.technical")
                .input("billing", "${handle_billing.output.result}")
                .input("technical", "${handle_technical.output.result}");
        workflow.add(decision);
        workflow.add(selected);
        workflow.setWorkflowOutput(Map.of(
                "selectedCase", "${route_request.output.selectedCase}",
                "answers", "${route_request.output.answers}",
                "usage", "${route_request.output.usage}",
                "cost", "${route_request.output.usage.cost}",
                "latencyMs", "${route_request.output.latencyMs}",
                "result", "${selected_result.output.result}"));
        return workflow;
    }

    public static void main(String[] args) throws Exception {
        String request = args.length == 0 ? "I was charged twice on my latest invoice." : args[0];
        ApiClient client = new ApiClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 100);
        try {
            ConductorWorkflow<Map<String, Object>> workflow = createWorkflow(executor);
            workflow.registerWorkflow(true);
            Workflow result = executor.executeWorkflow(workflow, Map.of("request", request))
                    .get(120, TimeUnit.SECONDS);
            System.out.println("Execution: " + result.getWorkflowId());
            if (result.getStatus() != Workflow.WorkflowStatus.COMPLETED) {
                throw new IllegalStateException(result.getStatus() + ": " + result.getReasonForIncompletion());
            }
            System.out.println(result.getOutput());
        } finally {
            executor.shutdown();
            client.shutdown();
        }
    }
}
