package orkescloud;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import orkescloud.workers.CloudGreetWorker;

import java.util.List;
import java.util.Map;

/**
 * Orkes Conductor Cloud Connection Example
 *
 * Demonstrates connecting to either Orkes Conductor Cloud (managed) or a local
 * Conductor instance, depending on environment variables.
 *
 * Cloud mode:
 *   Set CONDUCTOR_SERVER_URL, CONDUCTOR_KEY_ID, and CONDUCTOR_KEY_SECRET to
 *   connect to Orkes Cloud. Uses cloud_greet task and orkes_cloud_test workflow.
 *
 * Local mode (fallback):
 *   If cloud credentials are not set, falls back to a local Conductor instance
 *   using CONDUCTOR_BASE_URL (default: http://localhost:8080/api). Uses
 *   local_greet task and local_connection_test workflow.
 *
 * Run:
 *   java -jar target/orkes-cloud-1.0.0.jar
 *   java -jar target/orkes-cloud-1.0.0.jar --workers
 */
public class OrkesCloudExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        // Check for Orkes Cloud credentials
        String serverUrl = System.getenv("CONDUCTOR_SERVER_URL");
        String keyId = System.getenv("CONDUCTOR_KEY_ID");
        String keySecret = System.getenv("CONDUCTOR_KEY_SECRET");

        boolean useCloud = serverUrl != null && !serverUrl.isBlank()
                && keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();

        System.out.println("=== Orkes Conductor Cloud Connection Example ===\n");

        ConductorClientHelper client;
        String taskName;
        String workflowName;
        String workflowResource;

        if (useCloud) {
            System.out.println("Mode: CLOUD");
            System.out.println("  Server URL:  " + serverUrl);
            System.out.println("  Key ID:      " + keyId.substring(0, Math.min(8, keyId.length())) + "...");
            System.out.println("  Auth method: API key via X-Authorization header");
            System.out.println("");
            System.out.println("  Note: The conductor-oss SDK v5 does not have a built-in credentials()");
            System.out.println("  method. Auth headers are injected via addHeaderSupplier(). For production");
            System.out.println("  Orkes Cloud usage, consider the official Orkes Java SDK.\n");
            client = new ConductorClientHelper(serverUrl, keyId, keySecret);
            taskName = "cloud_greet";
            workflowName = "orkes_cloud_test";
            workflowResource = "workflow.json";
        } else {
            System.out.println("Mode: LOCAL (no cloud credentials found)");
            System.out.println("  To connect to Orkes Cloud, set these environment variables:");
            System.out.println("    CONDUCTOR_SERVER_URL  — e.g., https://play.orkes.io/api");
            System.out.println("    CONDUCTOR_KEY_ID      — your Orkes Cloud API key ID");
            System.out.println("    CONDUCTOR_KEY_SECRET  — your Orkes Cloud API key secret\n");
            String localUrl = System.getenv("CONDUCTOR_BASE_URL") != null
                    ? System.getenv("CONDUCTOR_BASE_URL")
                    : "http://localhost:8080/api";
            System.out.println("  Connecting to local Conductor at: " + localUrl + "\n");
            client = new ConductorClientHelper();
            taskName = "local_greet";
            workflowName = "local_connection_test";
            workflowResource = "workflow.json";
        }

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");
        client.registerTaskDefs(List.of(taskName));
        System.out.println("  Registered: " + taskName + "\n");

        // Step 2 — Register workflow (override name and task name based on mode)
        System.out.println("Step 2: Registering workflow '" + workflowName + "'...");
        client.registerWorkflow(workflowResource, workflowName, taskName);
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start worker
        System.out.println("Step 3: Starting worker...");
        boolean cloudMode = client.isCloudMode();
        List<Worker> workers = List.of(new CloudGreetWorker(cloudMode));
        client.startWorkers(workers);
        System.out.println("  1 worker polling for '" + taskName + "' tasks.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow(workflowName, 1,
                Map.of("name", "Developer"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        // Print Orkes Cloud features
        printCloudFeatures(cloudMode);

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }

    private static void printCloudFeatures(boolean cloudMode) {
        System.out.println("\n--- Orkes Conductor Cloud Features ---");
        if (cloudMode) {
            System.out.println("  You are connected to Orkes Cloud. Available features:");
        } else {
            System.out.println("  Running locally. These features are available with Orkes Cloud:");
        }
        System.out.println("  - Fully managed Conductor — no infrastructure to maintain");
        System.out.println("  - Multi-tenant isolation with role-based access control");
        System.out.println("  - Built-in monitoring, alerting, and workflow analytics");
        System.out.println("  - Automatic scaling for high-throughput workloads");
        System.out.println("  - Enterprise SSO and audit logging");
        System.out.println("  - Secrets management integration (Vault, AWS Secrets Manager)");
        System.out.println("  - Workflow versioning and environment promotion");
        System.out.println("  - AI/LLM orchestration with native model integrations");
        System.out.println("  - Webhook and event-driven workflow triggers");
        System.out.println("  - SLA-backed uptime and dedicated support plans");
        System.out.println("");
        System.out.println("  Learn more: https://orkes.io/cloud/");
    }
}
