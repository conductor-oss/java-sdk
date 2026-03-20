package retryfixed;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import retryfixed.workers.RetryFixedWorker;

import java.util.List;
import java.util.Map;

/**
 * FIXED Retry Strategy — constant delay between retry attempts
 *
 * Demonstrates Conductor's FIXED retry logic where a failed task is retried
 * after a constant delay (1 second) for up to 3 attempts.
 *
 * The worker calls failing N times (controlled by failCount input),
 * then succeeds. This lets you observe the retry behavior.
 *
 * Run:
 *   java -jar target/retry-fixed-1.0.0.jar
 *   java -jar target/retry-fixed-1.0.0.jar --workers
 */
public class RetryFixedExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== FIXED Retry Strategy Demo: Constant Delay Between Retries ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with FIXED retry config
        System.out.println("Step 1: Registering task definition with FIXED retry config...");

        TaskDef retryFixedTask = new TaskDef();
        retryFixedTask.setName("retry_fixed_task");
        retryFixedTask.setRetryCount(3);
        retryFixedTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        retryFixedTask.setRetryDelaySeconds(1);
        retryFixedTask.setTimeoutSeconds(60);
        retryFixedTask.setResponseTimeoutSeconds(30);
        retryFixedTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(retryFixedTask));

        System.out.println("\n  Registered: retry_fixed_task");
        System.out.println("    Retries: 3 (FIXED, 1s delay)");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'retry_fixed_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new RetryFixedWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow (fail 2 times, succeed on 3rd attempt)
        System.out.println("Step 4: Starting workflow (failCount=2)...\n");
        String workflowId = client.startWorkflow("retry_fixed_demo", 1,
                Map.of("failCount", 2));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
