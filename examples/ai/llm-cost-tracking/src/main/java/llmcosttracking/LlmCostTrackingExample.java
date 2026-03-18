package llmcosttracking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import llmcosttracking.workers.AggregateCostsWorker;
import llmcosttracking.workers.CallClaudeWorker;
import llmcosttracking.workers.CallGeminiWorker;
import llmcosttracking.workers.CallGpt4Worker;

import java.util.List;
import java.util.Map;

/**
 * LLM Cost Tracking — Sequential chain of LLM call workers
 * with cost aggregation:
 * ct_call_gpt4 -> ct_call_claude -> ct_call_gemini -> ct_aggregate_costs
 *
 * Run:
 *   java -jar target/llm-cost-tracking-1.0.0.jar
 */
public class LlmCostTrackingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== LLM Cost Tracking: Multi-Model Cost Aggregation ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ct_call_gpt4", "ct_call_claude", "ct_call_gemini", "ct_aggregate_costs"));
        System.out.println("  Registered: ct_call_gpt4, ct_call_claude, ct_call_gemini, ct_aggregate_costs\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'llm_cost_tracking_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CallGpt4Worker(),
                new CallClaudeWorker(),
                new CallGeminiWorker(),
                new AggregateCostsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("llm_cost_tracking_workflow", 1, Map.of(
                "prompt", "Analyze the quarterly revenue trends and provide insights."
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
