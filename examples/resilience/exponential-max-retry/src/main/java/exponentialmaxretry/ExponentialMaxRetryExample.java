package exponentialmaxretry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import exponentialmaxretry.workers.DeadLetterLogWorker;
import exponentialmaxretry.workers.UnreliableApiWorker;

import java.util.List;
import java.util.Map;

/**
 * Exponential Backoff with Max Retries and Dead Letter Queue
 *
 * Demonstrates Conductor's EXPONENTIAL_BACKOFF retry logic where a failed task
 * is retried with exponentially increasing delays for up to 3 attempts.
 * When all retries are exhausted, a failure workflow (dead letter handler)
 * is triggered to log the failure.
 *
 * Run:
 *   java -jar target/exponential-max-retry-1.0.0.jar
 *   java -jar target/exponential-max-retry-1.0.0.jar --workers
 */
public class ExponentialMaxRetryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Exponential Backoff with Max Retries & Dead Letter Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef unreliableApiTask = new TaskDef();
        unreliableApiTask.setName("emr_unreliable_api");
        unreliableApiTask.setRetryCount(3);
        unreliableApiTask.setRetryLogic(TaskDef.RetryLogic.EXPONENTIAL_BACKOFF);
        unreliableApiTask.setRetryDelaySeconds(1);
        unreliableApiTask.setTimeoutSeconds(60);
        unreliableApiTask.setResponseTimeoutSeconds(30);
        unreliableApiTask.setOwnerEmail("examples@orkes.io");

        TaskDef deadLetterLogTask = new TaskDef();
        deadLetterLogTask.setName("emr_dead_letter_log");
        deadLetterLogTask.setRetryCount(0);
        deadLetterLogTask.setTimeoutSeconds(60);
        deadLetterLogTask.setResponseTimeoutSeconds(30);
        deadLetterLogTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(unreliableApiTask, deadLetterLogTask));

        System.out.println("\n  Registered: emr_unreliable_api");
        System.out.println("    Retries: 3 (EXPONENTIAL_BACKOFF, 1s base delay)");
        System.out.println("  Registered: emr_dead_letter_log");
        System.out.println("    Retries: 0 (dead letter logger)\n");

        // Step 2 — Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("dead-letter-handler.json");
        client.registerWorkflow("workflow.json");
        System.out.println("  Main workflow and dead letter handler registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new UnreliableApiWorker(), new DeadLetterLogWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow (shouldSucceed=true, expect success)
        System.out.println("Step 4: Starting workflow (shouldSucceed=true)...\n");
        String workflowId = client.startWorkflow("emr_exponential_max_retry", 1,
                Map.of("shouldSucceed", true));
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
