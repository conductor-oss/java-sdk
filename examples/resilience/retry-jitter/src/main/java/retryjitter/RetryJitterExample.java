package retryjitter;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import retryjitter.workers.JitterApiCallWorker;

import java.util.List;
import java.util.Map;

/**
 * Retry with Jitter — adds random delay before retry to avoid thundering herd
 *
 * Demonstrates how adding jitter (a small random-ish delay) to retried API calls
 * prevents multiple clients from retrying in lockstep and overwhelming a service.
 *
 * The worker computes a deterministic jitter value from the endpoint name, then
 * sleeps that amount before making the call.
 *
 * Run:
 *   java -jar target/retry-jitter-1.0.0.jar
 *   java -jar target/retry-jitter-1.0.0.jar --workers
 */
public class RetryJitterExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Retry with Jitter Demo: Avoid Thundering Herd ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with retry config
        System.out.println("Step 1: Registering task definition with retry config...");

        TaskDef jitterTask = new TaskDef();
        jitterTask.setName("jitter_api_call");
        jitterTask.setRetryCount(3);
        jitterTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        jitterTask.setRetryDelaySeconds(1);
        jitterTask.setTimeoutSeconds(60);
        jitterTask.setResponseTimeoutSeconds(30);
        jitterTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(jitterTask));

        System.out.println("\n  Registered: jitter_api_call");
        System.out.println("    Retries: 3 (FIXED, 1s delay)");
        System.out.println("    Jitter: deterministic, based on endpoint hashCode\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'retry_jitter_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new JitterApiCallWorker());
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
        System.out.println("Step 4: Starting workflow with endpoint='https://api.example.com/data'...\n");
        String workflowId = client.startWorkflow("retry_jitter_demo", 1,
                Map.of("endpoint", "https://api.example.com/data"));
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
