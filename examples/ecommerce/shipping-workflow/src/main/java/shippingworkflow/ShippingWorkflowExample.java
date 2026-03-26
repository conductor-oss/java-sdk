package shippingworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import shippingworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 457: Shipping Workflow
 *
 * Performs a shipping workflow:
 * select carrier -> create label -> track shipment -> deliver -> confirm.
 */
public class ShippingWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 457: Shipping Workflow ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("shp_select_carrier", "shp_create_label", "shp_track", "shp_deliver", "shp_confirm"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow 'shipping_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SelectCarrierWorker(),
                new CreateLabelWorker(),
                new TrackShipmentWorker(),
                new DeliverShipmentWorker(),
                new ConfirmDeliveryWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("shipping_workflow", 1, Map.of(
                "orderId", "ORD-9901",
                "weight", 3.5,
                "dimensions", Map.of("length", 12, "width", 8, "height", 6),
                "origin", Map.of("city", "San Francisco", "state", "CA", "zip", "94105"),
                "destination", Map.of("city", "Austin", "state", "TX", "zip", "73301"),
                "shippingSpeed", "express"));
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
