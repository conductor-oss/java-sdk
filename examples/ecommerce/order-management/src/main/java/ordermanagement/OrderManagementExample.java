package ordermanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ordermanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 455: Order Management
 *
 * Runs an order management workflow:
 * create order -> validate -> fulfill -> ship -> deliver.
 */
public class OrderManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 455: Order Management ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ord_create", "ord_validate", "ord_fulfill", "ord_ship", "ord_deliver"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow 'order_management'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateOrderWorker(),
                new ValidateOrderWorker(),
                new FulfillOrderWorker(),
                new ShipOrderWorker(),
                new DeliverOrderWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("order_management", 1, Map.of(
                "customerId", "cust-301",
                "items", List.of(
                        Map.of("sku", "LAPTOP-PRO", "name", "Laptop Pro 16", "price", 1999.00, "qty", 1),
                        Map.of("sku", "USB-C-HUB", "name", "USB-C Hub", "price", 49.99, "qty", 1)),
                "shippingAddress", Map.of("street", "456 Oak Ave", "city", "Austin", "state", "TX", "zip", "73301"),
                "shippingMethod", "express"));
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
