package webhookretry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webhookretry.workers.PrepareWebhookWorker;
import webhookretry.workers.AttemptDeliveryWorker;
import webhookretry.workers.CheckResultWorker;
import webhookretry.workers.RecordOutcomeWorker;

import java.util.List;
import java.util.Map;

/**
 * Webhook Retry Workflow Demo
 *
 * Demonstrates a DO_WHILE retry loop for webhook delivery:
 *   wr_prepare_webhook -> DO_WHILE(wr_attempt_delivery -> wr_check_result, 3 iters) -> wr_record_outcome
 *
 * Run:
 *   java -jar target/webhook-retry-1.0.0.jar
 */
public class WebhookRetryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Webhook Retry Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wr_prepare_webhook", "wr_attempt_delivery",
                "wr_check_result", "wr_record_outcome"));
        System.out.println("  Registered: wr_prepare_webhook, wr_attempt_delivery, wr_check_result, wr_record_outcome\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'webhook_retry_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PrepareWebhookWorker(),
                new AttemptDeliveryWorker(),
                new CheckResultWorker(),
                new RecordOutcomeWorker()
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
        String workflowId = client.startWorkflow("webhook_retry_wf", 1,
                Map.of("webhookUrl", "https://api.example.com/webhook",
                        "payload", Map.of(
                                "event", "order.completed",
                                "orderId", "ORD-7890")));
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
