package webhookcallback;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webhookcallback.workers.ReceiveRequestWorker;
import webhookcallback.workers.ProcessDataWorker;
import webhookcallback.workers.NotifyCallbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Webhook Callback Demo
 *
 * Demonstrates a sequential pipeline of three workers that receive a webhook
 * request, process the data, and send a callback notification:
 *   wc_receive_request -> wc_process_data -> wc_notify_callback
 *
 * Run:
 *   java -jar target/webhook-callback-1.0.0.jar
 */
public class WebhookCallbackExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Webhook Callback Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wc_receive_request", "wc_process_data", "wc_notify_callback"));
        System.out.println("  Registered: wc_receive_request, wc_process_data, wc_notify_callback\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'webhook_callback_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveRequestWorker(),
                new ProcessDataWorker(),
                new NotifyCallbackWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("webhook_callback_wf", 1,
                Map.of("requestId", "req-fixed-001",
                        "data", Map.of("type", "customer_import", "recordCount", 150),
                        "callbackUrl", "https://api.partner.com/webhooks/completion"));
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
