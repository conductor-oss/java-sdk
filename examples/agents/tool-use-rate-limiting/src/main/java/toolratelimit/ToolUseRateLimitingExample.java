package toolratelimit;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import toolratelimit.workers.CheckRateLimitWorker;
import toolratelimit.workers.ExecuteToolWorker;
import toolratelimit.workers.QueueRequestWorker;
import toolratelimit.workers.DelayedExecuteWorker;

import java.util.List;
import java.util.Map;

/**
 * Tool Use Rate Limiting Demo
 *
 * Demonstrates rate-limit-aware tool execution: check the rate limit, then
 * either execute immediately or queue the request for delayed execution.
 *   check_rate_limit -> SWITCH(allowed->execute_tool, default/throttled->queue_request->delayed_execute)
 *
 * Run:
 *   java -jar target/tool-use-rate-limiting-1.0.0.jar
 */
public class ToolUseRateLimitingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Tool Use Rate Limiting Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rl_check_rate_limit", "rl_execute_tool",
                "rl_queue_request", "rl_delayed_execute"));
        System.out.println("  Registered: rl_check_rate_limit, rl_execute_tool, rl_queue_request, rl_delayed_execute\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'tool_use_rate_limiting'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckRateLimitWorker(),
                new ExecuteToolWorker(),
                new QueueRequestWorker(),
                new DelayedExecuteWorker()
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
        String workflowId = client.startWorkflow("tool_use_rate_limiting", 1,
                Map.of("toolName", "translation_api",
                        "toolArgs", Map.of("text", "Hello, how are you today?",
                                "from", "en", "to", "fr"),
                        "apiKey", "key-prod-abc123"));
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
