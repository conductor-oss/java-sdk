package azureintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import azureintegration.workers.AzBlobUploadWorker;
import azureintegration.workers.AzCosmosDbWriteWorker;
import azureintegration.workers.AzEventHubPublishWorker;
import azureintegration.workers.AzVerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Azure Integration Demo
 *
 * Runs an Azure multi-service integration workflow using FORK_JOIN:
 *   FORK(Blob upload, CosmosDB write, EventHub publish) -> JOIN -> verify.
 */
public class AzureIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 443: Azure Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("az_blob_upload", "az_cosmosdb_write", "az_eventhub_publish", "az_verify"));
        System.out.println("  Registered: az_blob_upload, az_cosmosdb_write, az_eventhub_publish, az_verify\n");

        System.out.println("Step 2: Registering workflow 'azure_integration'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AzBlobUploadWorker(), new AzCosmosDbWriteWorker(),
                new AzEventHubPublishWorker(), new AzVerifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("azure_integration", 1,
                Map.of("containerName", "data-events",
                        "databaseName", "events-db",
                        "eventHubName", "data-stream",
                        "payload", Map.of("id", "evt-7001", "type", "inventory.updated",
                                "timestamp", "2026-01-15T10:00:00Z",
                                "data", Map.of("sku", "SKU-100", "qty", 250))));
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
