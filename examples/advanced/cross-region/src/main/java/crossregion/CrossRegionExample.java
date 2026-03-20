package crossregion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import crossregion.workers.XrReplicateWorker;
import crossregion.workers.XrSyncWorker;
import crossregion.workers.XrVerifyConsistencyWorker;

import java.util.List;
import java.util.Map;

/**
 * Cross-Region Demo
 *
 * Run:
 *   java -jar target/crossregion-1.0.0.jar
 */
public class CrossRegionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Cross-Region Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "xr_replicate",
                "xr_sync",
                "xr_verify_consistency"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'cross_region_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new XrReplicateWorker(),
                new XrSyncWorker(),
                new XrVerifyConsistencyWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cross_region_demo", 1,
                Map.of("primaryRegion", "us-east-1", "replicaRegion", "eu-west-1", "datasetId", "DS-USERS-2024"));
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