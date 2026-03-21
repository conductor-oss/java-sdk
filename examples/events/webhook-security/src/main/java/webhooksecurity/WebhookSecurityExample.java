package webhooksecurity;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webhooksecurity.workers.ComputeHmacWorker;
import webhooksecurity.workers.VerifySignatureWorker;
import webhooksecurity.workers.ProcessWebhookWorker;
import webhooksecurity.workers.RejectWebhookWorker;

import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Webhook Security Workflow Demo
 *
 * Demonstrates a SWITCH-based webhook signature verification workflow:
 *   ws_compute_hmac -> ws_verify_signature -> SWITCH(result:
 *       valid   -> ws_process_webhook,
 *       invalid -> ws_reject_webhook)
 *
 * Run:
 *   java -jar target/webhook-security-1.0.0.jar
 */
public class WebhookSecurityExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Webhook Security Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ws_compute_hmac", "ws_verify_signature",
                "ws_process_webhook", "ws_reject_webhook"));
        System.out.println("  Registered: ws_compute_hmac, ws_verify_signature, ws_process_webhook, ws_reject_webhook\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'webhook_security_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ComputeHmacWorker(),
                new VerifySignatureWorker(),
                new ProcessWebhookWorker(),
                new RejectWebhookWorker()
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
        
        // Store secret via Conductor Secrets API
        String conductorUrl = System.getenv().getOrDefault("CONDUCTOR_BASE_URL", "http://localhost:8080/api");
        HttpClient http = HttpClient.newHttpClient();
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(conductorUrl + "/secrets/webhook_secret"))
                .PUT(HttpRequest.BodyPublishers.ofString("\"whsec_test_secret_2026\""))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  Secret \'webhook_secret\' stored via Conductor Secrets API");

        String workflowId = client.startWorkflow("webhook_security_wf", 1,
                Map.of("payload", "{\"event\":\"push\",\"repo\":\"my-repo\"}",
                        "providedSignature", "hmac_sha256_fixedvalue"));
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
