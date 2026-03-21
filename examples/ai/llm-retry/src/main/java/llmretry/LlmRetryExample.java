package llmretry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import llmretry.workers.RetryLlmCallWorker;
import llmretry.workers.RetryReportWorker;

import java.util.List;
import java.util.Map;

/**
 * LLM with Retry -- Handle Rate Limits and Timeouts
 *
 * Registers a task with retryCount=3 and EXPONENTIAL_BACKOFF policy.
 * The worker fails the first 2 attempts (performing HTTP 429 rate
 * limits), then succeeds on the 3rd. Conductor handles retries
 * automatically.
 *
 * Run:
 *   java -jar target/llm-retry-1.0.0.jar
 */
public class LlmRetryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== LLM with Retry: Rate Limits & Timeouts ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("retry_llm_call", "retry_report"));
        System.out.println("  Registered: retry_llm_call, retry_report\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'llm_retry_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RetryLlmCallWorker(), new RetryReportWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("llm_retry_wf", 1,
                Map.of("prompt", "Explain retry patterns", "model", "gpt-4"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Attempts: " + workflow.getOutput().get("attempts"));
        System.out.println("  Retry policy: " + workflow.getOutput().get("retryPolicy"));
        System.out.println("  Response: " + workflow.getOutput().get("response"));

        System.out.println("\n--- Retry Pattern ---");
        System.out.println("  Task retryCount=3, retryLogic=EXPONENTIAL_BACKOFF");
        System.out.println("  Attempt 1: 429 -> retry after 1s");
        System.out.println("  Attempt 2: 429 -> retry after 2s");
        System.out.println("  Attempt 3: Success");

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
