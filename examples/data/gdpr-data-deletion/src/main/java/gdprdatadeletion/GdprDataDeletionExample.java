package gdprdatadeletion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gdprdatadeletion.workers.FindRecordsWorker;
import gdprdatadeletion.workers.VerifyIdentityWorker;
import gdprdatadeletion.workers.DeleteDataWorker;
import gdprdatadeletion.workers.GenerateAuditLogWorker;

import java.util.List;
import java.util.Map;

/**
 * GDPR Data Deletion Workflow Demo
 *
 * Demonstrates a GDPR right-to-erasure pipeline:
 *   gr_find_records -> gr_verify_identity -> gr_delete_data -> gr_generate_audit_log
 *
 * Run:
 *   java -jar target/gdpr-data-deletion-1.0.0.jar
 */
public class GdprDataDeletionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== GDPR Data Deletion Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "gr_find_records", "gr_verify_identity",
                "gr_delete_data", "gr_generate_audit_log"));
        System.out.println("  Registered: gr_find_records, gr_verify_identity, gr_delete_data, gr_generate_audit_log\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'gdpr_data_deletion'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FindRecordsWorker(),
                new VerifyIdentityWorker(),
                new DeleteDataWorker(),
                new GenerateAuditLogWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("gdpr_data_deletion", 1,
                Map.of("userId", "USR-7042",
                        "requestId", "GDPR-REQ-2024-0315",
                        "verificationToken", "verified-token-abc123"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
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
