package dockersetup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dockersetup.workers.DockerTestWorker;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Docker Setup Verification — Health Check and Smoke Test
 *
 * Verifies your local Conductor Docker setup is working:
 * - Health check: tries to reach the server
 * - Registers a test task and workflow
 * - Starts a worker, runs the workflow end-to-end
 * - Reports success or failure with helpful diagnostics
 *
 * Run:
 *   java -jar target/docker-setup-1.0.0.jar
 */
public class DockerSetupExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        String serverUrl = ConductorClientHelper.getServerUrl();
        String baseUrl = serverUrl.endsWith("/api")
                ? serverUrl.substring(0, serverUrl.length() - 4)
                : serverUrl;

        System.out.println("=== Docker Setup Verification ===\n");
        System.out.println("Conductor URL: " + serverUrl + "\n");

        // Step 1 — Health check
        System.out.println("Step 1: Health check...");
        String healthUrl = baseUrl + "/health";
        if (!isHealthy(healthUrl)) {
            System.out.println("  FAILED — Conductor is not reachable at " + healthUrl + "\n");
            System.out.println("Start Conductor with Docker:");
            System.out.println("  docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest\n");
            System.out.println("Then wait for it to be ready:");
            System.out.println("  until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done\n");
            System.exit(1);
            return;
        }
        System.out.println("  Conductor is healthy.\n");

        var client = new ConductorClientHelper();

        // Step 2 — Register task definition
        System.out.println("Step 2: Registering task definition...");
        client.registerTaskDefs(List.of("docker_test_task"));
        System.out.println("  Registered: docker_test_task\n");

        // Step 3 — Register workflow
        System.out.println("Step 3: Registering workflow 'docker_setup_test'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 4 — Start worker
        System.out.println("Step 4: Starting worker...");
        List<Worker> workers = List.of(new DockerTestWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 5 — Start the workflow
        System.out.println("Step 5: Starting workflow...\n");
        String workflowId = client.startWorkflow("docker_setup_test", 1,
                Map.of("message", "Verifying Docker setup"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 6 — Wait for completion
        System.out.println("Step 6: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nDocker setup: ALL GOOD");
            System.out.println("  UI:  " + baseUrl.replace("/api", "") + ":1234");
            System.out.println("  API: " + serverUrl);
            System.exit(0);
        } else {
            System.out.println("\nDocker setup: FAILED");
            System.out.println("  Workflow finished with status: " + status);
            System.exit(1);
        }
    }

    private static boolean isHealthy(String healthUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(healthUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        }
    }
}
