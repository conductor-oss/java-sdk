package retrylinear;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import retrylinear.workers.RetryLinearWorker;

import java.util.List;
import java.util.Map;

/**
 * LINEAR_BACKOFF Retry Demo
 *
 * Demonstrates Conductor's LINEAR_BACKOFF retry policy where
 * the delay between retries increases linearly (delay * attempt).
 *
 * Configuration:
 *   retryCount: 4
 *   retryLogic: LINEAR_BACKOFF
 *   retryDelaySeconds: 1
 *
 * The worker performs a service that is unavailable for the first
 * 3 attempts and succeeds on the 4th attempt.
 *
 * Run:
 *   java -jar target/retry-linear-1.0.0.jar
 *   java -jar target/retry-linear-1.0.0.jar --workers
 */
public class RetryLinearExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== LINEAR_BACKOFF Retry Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions with LINEAR_BACKOFF retry
        System.out.println("Step 1: Registering task definitions...");
        TaskDef taskDef = new TaskDef();
        taskDef.setName("retry_linear_task");
        taskDef.setRetryCount(4);
        taskDef.setRetryLogic(TaskDef.RetryLogic.LINEAR_BACKOFF);
        taskDef.setRetryDelaySeconds(1);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setOwnerEmail("examples@orkes.io");
        client.registerTaskDefs(List.of(taskDef));
        System.out.println("  Registered: retry_linear_task (LINEAR_BACKOFF, retryCount=4, delay=1s)\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'retry_linear_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RetryLinearWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("retry_linear_demo", 1,
                Map.of("service", "payment-gateway"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion (retries with linear backoff)...");
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
