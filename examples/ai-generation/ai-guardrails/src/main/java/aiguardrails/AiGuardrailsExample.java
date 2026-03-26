package aiguardrails;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aiguardrails.workers.InputCheckWorker;
import aiguardrails.workers.ContentFilterWorker;
import aiguardrails.workers.GenerateWorker;
import aiguardrails.workers.OutputCheckWorker;
import aiguardrails.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 803: AI Guardrails — Input Check, Content Filter, Generate, Output Check, Deliver
 */
public class AiGuardrailsExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 803: AI Guardrails — Input Check, Content Filter, Generate, Output Check, Deliver ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("grl_input_check", "grl_content_filter", "grl_generate", "grl_output_check", "grl_deliver"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new InputCheckWorker(), new ContentFilterWorker(), new GenerateWorker(), new OutputCheckWorker(), new DeliverWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("grl_ai_guardrails", 1, Map.of("userPrompt", "Explain what machine learning is", "userId", "USR-803", "modelId", "gpt-4"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("outputSafe: %s%n", workflow.getOutput().get("outputSafe"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
