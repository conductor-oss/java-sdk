package claimcheck;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import claimcheck.workers.StorePayloadWorker;
import claimcheck.workers.PassReferenceWorker;
import claimcheck.workers.RetrieveWorker;
import claimcheck.workers.ProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 591: Claim Check -- Store Large Payload by Reference
 *
 * Demonstrates the Claim Check pattern:
 *   clc_store_payload -> clc_pass_reference -> clc_retrieve -> clc_process
 *
 * Run:
 *   java -jar target/claim-check-1.0.0.jar
 */
public class ClaimCheckExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 591: Claim Check ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "clc_store_payload", "clc_pass_reference", "clc_retrieve", "clc_process"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'clc_claim_check'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StorePayloadWorker(),
                new PassReferenceWorker(),
                new RetrieveWorker(),
                new ProcessWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("clc_claim_check", 1,
                Map.of("payload", Map.of(
                                "reportId", "RPT-2024-001",
                                "data", List.of(
                                        Map.of("metric", "cpu_usage", "values", List.of(45, 52, 68, 71, 55)),
                                        Map.of("metric", "memory_usage", "values", List.of(62, 64, 70, 68, 65)),
                                        Map.of("metric", "disk_io", "values", List.of(120, 130, 145, 110, 125))
                                ),
                                "generatedAt", "2024-01-15T10:30:00Z"
                        ),
                        "storageType", "s3"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
