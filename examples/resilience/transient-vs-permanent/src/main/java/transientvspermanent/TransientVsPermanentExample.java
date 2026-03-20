package transientvspermanent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import transientvspermanent.workers.SmartTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * Transient vs Permanent Error Detection — smart error classification
 *
 * Demonstrates how a Conductor worker can distinguish between transient errors
 * (which should be retried) and permanent errors (which should fail immediately
 * with FAILED_WITH_TERMINAL_ERROR to avoid wasting retry attempts).
 *
 * Run:
 *   java -jar target/transient-vs-permanent-1.0.0.jar
 *   java -jar target/transient-vs-permanent-1.0.0.jar --workers
 */
public class TransientVsPermanentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Transient vs Permanent Error Detection Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition with retry config
        System.out.println("Step 1: Registering task definition with retry config...");

        TaskDef smartTask = new TaskDef();
        smartTask.setName("tvp_smart_task");
        smartTask.setRetryCount(3);
        smartTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        smartTask.setRetryDelaySeconds(1);
        smartTask.setTimeoutSeconds(60);
        smartTask.setResponseTimeoutSeconds(30);
        smartTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(smartTask));

        System.out.println("\n  Registered: tvp_smart_task");
        System.out.println("    Retries: 3 (FIXED, 1s delay)");
        System.out.println("    Timeout: 60s total, 30s response\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'transient_vs_permanent_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new SmartTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Test transient error (should retry and succeed)
        System.out.println("Step 4: Starting workflow with transient error...\n");
        String transientId = client.startWorkflow("transient_vs_permanent_demo", 1,
                Map.of("errorType", "transient"));
        System.out.println("  Workflow ID: " + transientId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow transientWorkflow = client.waitForWorkflow(transientId, "COMPLETED", 30000);
        String transientStatus = transientWorkflow.getStatus().name();
        System.out.println("  Status: " + transientStatus);
        System.out.println("  Output: " + transientWorkflow.getOutput());

        // Step 5 — Test permanent error (should fail immediately)
        System.out.println("\nStep 5: Starting workflow with permanent error...\n");
        String permanentId = client.startWorkflow("transient_vs_permanent_demo", 1,
                Map.of("errorType", "permanent"));
        System.out.println("  Workflow ID: " + permanentId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow permanentWorkflow = client.waitForWorkflow(permanentId, "FAILED", 30000);
        String permanentStatus = permanentWorkflow.getStatus().name();
        System.out.println("  Status: " + permanentStatus);
        System.out.println("  Output: " + permanentWorkflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(transientStatus) && "FAILED".equals(permanentStatus)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
