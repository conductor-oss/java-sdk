package dataversioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dataversioning.workers.DvrSnapshotWorker;
import dataversioning.workers.DvrTagWorker;
import dataversioning.workers.DvrStoreWorker;
import dataversioning.workers.DvrDiffWorker;
import dataversioning.workers.DvrRollbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Versioning Demo
 *
 * Run:
 *   java -jar target/dataversioning-1.0.0.jar
 */
public class DataVersioningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Versioning Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dvr_snapshot",
                "dvr_tag",
                "dvr_store",
                "dvr_diff",
                "dvr_rollback"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'data_versioning_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DvrSnapshotWorker(),
                new DvrTagWorker(),
                new DvrStoreWorker(),
                new DvrDiffWorker(),
                new DvrRollbackWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_versioning_demo", 1,
                Map.of("datasetId", "DS-TRANSACTIONS", "tagName", "v2.3.0", "previousTag", "v2.2.0"));
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