package foodordering;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import foodordering.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 731: Food Ordering — Browse, Order, Pay, Prepare, Deliver
 *
 * End-to-end food ordering workflow from menu browsing to delivery.
 */
public class FoodOrderingExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 731: Food Ordering ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        // Register task definitions
        helper.registerTaskDefs(List.of(
                "fod_browse", "fod_order", "fod_pay", "fod_prepare", "fod_deliver"));

        // Register workflow
        helper.registerWorkflow("workflow.json");

        // Start workers
        List<Worker> workers = List.of(
                new BrowseWorker(),
                new OrderWorker(),
                new PayWorker(),
                new PrepareWorker(),
                new DeliverWorker());
        helper.startWorkers(workers);

        // Start workflow
        String workflowId = helper.startWorkflow("food_ordering_731", 1,
                Map.of("customerId", "CUST-42", "restaurantId", "REST-10"));

        // Wait for completion
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
