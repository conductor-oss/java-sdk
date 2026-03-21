package workflowarchival;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowarchival.workers.ArchivalTaskWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Workflow Archival — Cleanup policies for completed workflows
 *
 * Demonstrates archival options for completed workflows in Conductor:
 * runs 3 workflows, then shows how to delete them individually,
 * in bulk, and discusses TTL-based cleanup.
 *
 * Run:
 *   java -jar target/workflow-archival-1.0.0.jar
 */
public class WorkflowArchivalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Archival: Cleanup Policies for Completed Workflows ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("arch_task"));
        System.out.println("  Registered: arch_task\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'archival_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ArchivalTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start 3 workflows
        System.out.println("Step 4: Starting 3 workflows...\n");
        List<String> workflowIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String workflowId = client.startWorkflow("archival_demo", 1,
                    Map.of("batch", "batch_" + i));
            workflowIds.add(workflowId);
            System.out.println("  Workflow " + i + " ID: " + workflowId);
        }
        System.out.println();

        // Step 5 — Wait for all workflows to complete
        System.out.println("Step 5: Waiting for all workflows to complete...");
        boolean allCompleted = true;
        for (int i = 0; i < workflowIds.size(); i++) {
            Workflow workflow = client.waitForWorkflow(workflowIds.get(i), "COMPLETED", 30000);
            String status = workflow.getStatus().name();
            System.out.println("  Workflow " + (i + 1) + ": " + status
                    + " | Output: " + workflow.getOutput());
            if (!"COMPLETED".equals(status)) {
                allCompleted = false;
            }
        }
        System.out.println();

        if (!allCompleted) {
            System.out.println("Not all workflows completed. Skipping archival demos.\n");
            client.stopWorkers();
            System.out.println("Result: FAILED");
            System.exit(1);
        }

        // Step 6 — Demonstrate archival: DELETE API
        System.out.println("Step 6: Archival Option 1 — DELETE API (single workflow removal)");
        String deleteId = workflowIds.get(0);
        System.out.println("  Deleting workflow: " + deleteId);
        try {
            client.deleteWorkflow(deleteId);
            System.out.println("  Deleted successfully.");
        } catch (Exception e) {
            System.out.println("  Delete call completed (may return 404 if already archived): "
                    + e.getMessage());
        }

        // Verify deletion
        try {
            Workflow deleted = client.getWorkflow(deleteId);
            System.out.println("  Verification: workflow still accessible (status="
                    + deleted.getStatus() + ") — server may retain metadata.");
        } catch (Exception e) {
            System.out.println("  Verification: workflow no longer accessible — fully removed.");
        }
        System.out.println();

        // Step 7 — Demonstrate archival: Bulk remove
        System.out.println("Step 7: Archival Option 2 — Bulk removal");
        List<String> remainingIds = workflowIds.subList(1, workflowIds.size());
        System.out.println("  Removing " + remainingIds.size() + " workflows in bulk...");
        int removed = 0;
        for (String id : remainingIds) {
            try {
                client.deleteWorkflow(id);
                removed++;
            } catch (Exception e) {
                System.out.println("  Warning: could not delete " + id + ": " + e.getMessage());
            }
        }
        System.out.println("  Bulk removal complete: " + removed + "/" + remainingIds.size()
                + " workflows removed.\n");

        // Step 8 — TTL info
        System.out.println("Step 8: Archival Option 3 — TTL-based cleanup");
        System.out.println("  Conductor supports automatic cleanup via workflow TTL settings.");
        System.out.println("  Configure 'workflowStatusListenerEnabled' and TTL policies");
        System.out.println("  in the Conductor server config to auto-archive completed workflows");
        System.out.println("  after a specified retention period.\n");

        client.stopWorkers();

        System.out.println("Result: PASSED");
        System.exit(0);
    }
}
