package bulkoperations;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bulkoperations.workers.Step1Worker;
import bulkoperations.workers.Step2Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bulk Workflow Operations — Start, Pause, Resume, Terminate
 *
 * Demonstrates bulk operations on multiple Conductor workflows:
 * start several workflow instances, pause them all, resume them,
 * and terminate selected ones. Shows how to manage workflows in bulk.
 *
 * Run:
 *   java -jar target/bulk-operations-1.0.0.jar
 */
public class BulkOperationsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Bulk Workflow Operations: Start, Pause, Resume, Terminate ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("bulk_step1", "bulk_step2"));
        System.out.println("  Registered: bulk_step1, bulk_step2\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'bulk_ops_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new Step1Worker(),
                new Step2Worker()
        );
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Bulk start workflows
        System.out.println("Step 4: Bulk starting 3 workflows...");
        List<String> workflowIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String workflowId = client.startWorkflow("bulk_ops_demo", 1,
                    Map.of("batchId", String.valueOf(i)));
            workflowIds.add(workflowId);
            System.out.println("  Started workflow " + i + ": " + workflowId);
        }
        System.out.println();

        // Step 5 — Bulk pause
        System.out.println("Step 5: Bulk pausing all workflows...");
        for (String id : workflowIds) {
            client.pauseWorkflow(id);
        }
        Thread.sleep(1000);
        System.out.println("  Checking statuses after pause:");
        for (int i = 0; i < workflowIds.size(); i++) {
            Workflow wf = client.getWorkflow(workflowIds.get(i));
            System.out.println("    Workflow " + (i + 1) + ": " + wf.getStatus().name());
        }
        System.out.println();

        // Step 6 — Bulk resume
        System.out.println("Step 6: Bulk resuming all workflows...");
        for (String id : workflowIds) {
            client.resumeWorkflow(id);
        }
        Thread.sleep(1000);
        System.out.println("  Checking statuses after resume:");
        for (int i = 0; i < workflowIds.size(); i++) {
            Workflow wf = client.getWorkflow(workflowIds.get(i));
            System.out.println("    Workflow " + (i + 1) + ": " + wf.getStatus().name());
        }
        System.out.println();

        // Step 7 — Wait for completion and check results
        System.out.println("Step 7: Waiting for workflows to complete...");
        boolean allPassed = true;
        for (int i = 0; i < workflowIds.size(); i++) {
            Workflow wf = client.waitForWorkflow(workflowIds.get(i), "COMPLETED", 30000);
            String status = wf.getStatus().name();
            System.out.println("  Workflow " + (i + 1) + ": " + status + " — Output: " + wf.getOutput());
            if (!"COMPLETED".equals(status)) {
                allPassed = false;
            }
        }
        System.out.println();

        client.stopWorkers();

        if (allPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
