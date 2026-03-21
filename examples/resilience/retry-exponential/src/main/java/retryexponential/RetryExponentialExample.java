package retryexponential;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import retryexponential.workers.RetryExpoTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * Exponential Backoff Retry -- doubles wait time between retries.
 *
 * Demonstrates Conductor's EXPONENTIAL_BACKOFF retry logic:
 * - retryCount: 4
 * - retryLogic: EXPONENTIAL_BACKOFF
 * - retryDelaySeconds: 1 (delays: 1s, 2s, 4s, 8s)
 *
 * The worker runs an API that returns 429 for the first 2 calls, then succeeds.
 *
 * Run:
 *   java -jar target/retry-exponential-1.0.0.jar
 *   java -jar target/retry-exponential-1.0.0.jar --workers
 */
public class RetryExponentialExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Retry Exponential Backoff Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definition with exponential backoff retry
        System.out.println("Step 1: Registering task definition with EXPONENTIAL_BACKOFF retry...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("retry_expo_task");
        taskDef.setRetryCount(4);
        taskDef.setRetryLogic(TaskDef.RetryLogic.EXPONENTIAL_BACKOFF);
        taskDef.setRetryDelaySeconds(1);
        taskDef.setTimeoutSeconds(120);
        taskDef.setResponseTimeoutSeconds(60);
        taskDef.setOwnerEmail("examples@orkes.io");
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  Registered: retry_expo_task");
        System.out.println("    retryCount: 4");
        System.out.println("    retryLogic: EXPONENTIAL_BACKOFF");
        System.out.println("    retryDelaySeconds: 1 (delays: 1s, 2s, 4s, 8s)\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'retry_expo_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RetryExpoTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("retry_expo_demo", 1,
                Map.of("apiUrl", "https://api.example.com/data"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion (exponential backoff retries may take a moment)...");
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
