package inventorymanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import inventorymanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 456: Inventory Management
 *
 * Runs an inventory management workflow:
 * check stock -> reserve units -> update inventory -> reorder if low.
 */
public class InventoryManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 456: Inventory Management ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("inv_check_stock", "inv_reserve", "inv_update", "inv_reorder"));
        System.out.println("  Registered 4 tasks.\n");

        System.out.println("Step 2: Registering workflow 'inventory_management'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckStockWorker(),
                new ReserveStockWorker(),
                new UpdateInventoryWorker(),
                new ReorderWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("inventory_management", 1, Map.of(
                "sku", "WH-1000XM5",
                "requestedQty", 20,
                "warehouseId", "WH-EAST-01",
                "reorderThreshold", 30));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
