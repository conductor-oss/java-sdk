package webhooktrigger;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webhooktrigger.workers.ProcessEventWorker;
import webhooktrigger.workers.ValidatePayloadWorker;
import webhooktrigger.workers.TransformDataWorker;
import webhooktrigger.workers.StoreResultWorker;

import java.util.List;
import java.util.Map;

/**
 * Webhook Trigger Demo
 *
 * Demonstrates a sequential pipeline of four workers that process a webhook event:
 * process event, validate payload, transform data, and store the result.
 *   wt_process_event -> wt_validate_payload -> wt_transform_data -> wt_store_result
 *
 * Run:
 *   java -jar target/webhook-trigger-1.0.0.jar
 */
public class WebhookTriggerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Webhook Trigger Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wt_process_event", "wt_validate_payload",
                "wt_transform_data", "wt_store_result"));
        System.out.println("  Registered: wt_process_event, wt_validate_payload, wt_transform_data, wt_store_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'webhook_trigger_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessEventWorker(),
                new ValidatePayloadWorker(),
                new TransformDataWorker(),
                new StoreResultWorker()
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
        String workflowId = client.startWorkflow("webhook_trigger_wf", 1,
                Map.of("eventType", "order.created",
                        "payload", Map.of(
                                "orderId", "ORD-2026-4821",
                                "customer", "acme-corp",
                                "amount", 1250.00,
                                "currency", "USD",
                                "items", 3),
                        "source", "shopify-webhook",
                        "timestamp", "2026-03-08T10:00:00Z"));
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
