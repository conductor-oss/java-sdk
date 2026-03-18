package idempotentprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import idempotentprocessing.workers.CheckProcessedWorker;
import idempotentprocessing.workers.ProcessWorker;
import idempotentprocessing.workers.RecordWorker;
import idempotentprocessing.workers.SkipWorker;

import java.util.List;
import java.util.Map;

/**
 * Idempotent Processing Demo
 *
 * Submits the same message twice to demonstrate that the first run processes it
 * normally while the second run detects the duplicate and skips processing.
 *
 * Run:
 *   java -jar target/idempotent-processing-1.0.0.jar
 */
public class IdempotentProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Idempotent Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "idp_check_processed",
                "idp_process",
                "idp_record",
                "idp_skip"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'idp_idempotent_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckProcessedWorker(),
                new ProcessWorker(),
                new RecordWorker(),
                new SkipWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String messageId = "msg-abc-123";

        // --- Run 1: First submission (new message) ---
        System.out.println("=== Run 1: First submission of " + messageId + " ===\n");
        String workflowId1 = client.startWorkflow("idp_idempotent_processing", 1,
                Map.of("messageId", messageId, "payload", "order data"));
        System.out.println("  Workflow ID: " + workflowId1);

        Workflow workflow1 = client.waitForWorkflow(workflowId1, "COMPLETED", 60000);
        String status1 = workflow1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + workflow1.getOutput());
        System.out.println();

        if (!"COMPLETED".equals(status1)) {
            System.out.println("Result: FAILED (Run 1 did not complete)");
            client.stopWorkers();
            System.exit(1);
        }

        Thread.sleep(1000);

        // --- Run 2: Duplicate submission (same messageId) ---
        System.out.println("=== Run 2: Duplicate submission of " + messageId + " ===\n");
        String workflowId2 = client.startWorkflow("idp_idempotent_processing", 1,
                Map.of("messageId", messageId, "payload", "order data"));
        System.out.println("  Workflow ID: " + workflowId2);

        Workflow workflow2 = client.waitForWorkflow(workflowId2, "COMPLETED", 60000);
        String status2 = workflow2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + workflow2.getOutput());
        System.out.println();

        client.stopWorkers();

        if ("COMPLETED".equals(status1) && "COMPLETED".equals(status2)) {
            System.out.println("Result: PASSED — both runs completed, duplicate was detected on Run 2");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
