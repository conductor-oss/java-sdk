package workflowmigration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowmigration.workers.WmExportOldWorker;
import workflowmigration.workers.WmTransformWorker;
import workflowmigration.workers.WmImportNewWorker;
import workflowmigration.workers.WmVerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Migration Demo
 *
 * Run:
 *   java -jar target/workflowmigration-1.0.0.jar
 */
public class WorkflowMigrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Migration Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wm_export_old",
                "wm_transform",
                "wm_import_new",
                "wm_verify"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'workflow_migration_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WmExportOldWorker(),
                new WmTransformWorker(),
                new WmImportNewWorker(),
                new WmVerifyWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("workflow_migration_demo", 1,
                Map.of("sourceSystem", "legacy-bpm", "targetSystem", "conductor-oss", "workflowName", "order_fulfillment"));
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