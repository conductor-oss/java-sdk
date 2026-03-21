package digitalassetmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import digitalassetmanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 521: Digital Asset Management
 *
 * DAM workflow: ingest asset, auto-tag with metadata, version control,
 * store in repository, and distribute to channels.
 *
 * Run:
 *   java -jar target/digital-asset-management-1.0.0.jar
 */
public class DigitalAssetManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 521: Digital Asset Management ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dam_ingest_asset", "dam_auto_tag", "dam_version_control",
                "dam_store_asset", "dam_distribute_asset"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IngestAssetWorker(),
                new AutoTagWorker(),
                new VersionControlWorker(),
                new StoreAssetWorker(),
                new DistributeAssetWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow(
                "digital_asset_management_workflow", 1,
                Map.of("assetId", "ASSET-521-001",
                        "assetType", "image",
                        "sourceUrl", "https://uploads.example.com/dam/521.psd",
                        "projectId", "PROJ-SUMMER-2026"));
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
