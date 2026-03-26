package datasync;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datasync.workers.DetectChangesWorker;
import datasync.workers.ResolveConflictsWorker;
import datasync.workers.ApplyUpdatesWorker;
import datasync.workers.VerifyConsistencyWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Sync Workflow Demo
 *
 * Demonstrates a bidirectional data sync pipeline:
 *   sy_detect_changes -> sy_resolve_conflicts -> sy_apply_updates -> sy_verify_consistency
 *
 * Run:
 *   java -jar target/data-sync-1.0.0.jar
 */
public class DataSyncExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Sync Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sy_detect_changes", "sy_resolve_conflicts",
                "sy_apply_updates", "sy_verify_consistency"));
        System.out.println("  Registered: sy_detect_changes, sy_resolve_conflicts, sy_apply_updates, sy_verify_consistency\n");

        System.out.println("Step 2: Registering workflow 'data_sync'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectChangesWorker(),
                new ResolveConflictsWorker(),
                new ApplyUpdatesWorker(),
                new VerifyConsistencyWorker()
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
        String workflowId = client.startWorkflow("data_sync", 1,
                Map.of("systemA", Map.of("name", "crm", "endpoint", "https://crm.internal/api"),
                        "systemB", Map.of("name", "erp", "endpoint", "https://erp.internal/api"),
                        "syncMode", "bidirectional",
                        "conflictStrategy", "latest_wins"));
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
