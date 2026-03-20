package webhookratelimiting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webhookratelimiting.workers.IdentifySenderWorker;
import webhookratelimiting.workers.CheckRateWorker;
import webhookratelimiting.workers.ProcessAllowedWorker;
import webhookratelimiting.workers.QueueThrottledWorker;

import java.util.List;
import java.util.Map;

/**
 * Webhook Rate Limiting Demo
 *
 * Demonstrates a SWITCH-based rate limiting workflow:
 *   wl_identify_sender -> wl_check_rate -> SWITCH(decision:
 *       allowed  -> wl_process_allowed,
 *       throttled -> wl_queue_throttled)
 *
 * Run:
 *   java -jar target/webhook-rate-limiting-1.0.0.jar
 */
public class WebhookRateLimitingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Webhook Rate Limiting Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wl_identify_sender", "wl_check_rate",
                "wl_process_allowed", "wl_queue_throttled"));
        System.out.println("  Registered: wl_identify_sender, wl_check_rate, wl_process_allowed, wl_queue_throttled\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'webhook_rate_limiting'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IdentifySenderWorker(),
                new CheckRateWorker(),
                new ProcessAllowedWorker(),
                new QueueThrottledWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("webhook_rate_limiting", 1,
                Map.of("senderId", "partner-api-xyz",
                        "payload", Map.of("event", "data.sync", "records", 150),
                        "rateLimit", 100));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
