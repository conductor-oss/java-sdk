package multimodelcompare;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multimodelcompare.workers.McCallClaudeWorker;
import multimodelcompare.workers.McCallGeminiWorker;
import multimodelcompare.workers.McCallGpt4Worker;
import multimodelcompare.workers.McCompareWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Model Compare — Conductor Workflow Example
 *
 * Demonstrates a FORK_JOIN pattern that runs 3 model calls in parallel,
 * joins the results, and then compares them to pick a winner.
 *
 * Run:
 *   java -jar target/multi-model-compare-1.0.0.jar
 */
public class MultiModelCompareExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Model Compare Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("mc_call_gpt4", "mc_call_claude", "mc_call_gemini", "mc_compare"));
        System.out.println("  Registered: mc_call_gpt4, mc_call_claude, mc_call_gemini, mc_compare\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_model_compare'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new McCallGpt4Worker(),
                new McCallClaudeWorker(),
                new McCallGeminiWorker(),
                new McCompareWorker()
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
        String workflowId = client.startWorkflow("multi_model_compare", 1,
                Map.of("prompt", "What is Conductor?"));
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
