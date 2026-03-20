package tooluseerror;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tooluseerror.workers.TryPrimaryToolWorker;
import tooluseerror.workers.FormatSuccessWorker;
import tooluseerror.workers.TryFallbackToolWorker;
import tooluseerror.workers.FormatFallbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Error Handling Demo
 *
 * Demonstrates error handling with tool fallback: tries a primary tool,
 * and on failure routes to a fallback tool via a SWITCH task.
 *   try_primary -> SWITCH(success -> format_success,
 *                         default -> try_fallback -> format_fallback)
 *
 * Run:
 *   java -jar target/tool-use-error-handling-1.0.0.jar
 */
public class ToolUseErrorHandlingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Error Handling Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "te_try_primary_tool", "te_format_success",
                "te_try_fallback_tool", "te_format_fallback"));
        System.out.println("  Registered: te_try_primary_tool, te_format_success, te_try_fallback_tool, te_format_fallback\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_error_handling'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new TryPrimaryToolWorker(),
                new FormatSuccessWorker(),
                new TryFallbackToolWorker(),
                new FormatFallbackWorker()
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
        String workflowId = client.startWorkflow("tool_use_error_handling", 1,
                Map.of("query", "What are the coordinates of San Francisco?",
                        "primaryTool", "google_geocoding_api",
                        "fallbackTool", "openstreetmap_nominatim"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
