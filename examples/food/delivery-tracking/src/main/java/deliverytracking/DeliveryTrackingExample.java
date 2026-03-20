package deliverytracking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import deliverytracking.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 733: Delivery Tracking — Assign Driver, Pickup, Track, Deliver, Confirm
 */
public class DeliveryTrackingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 733: Delivery Tracking ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("dlt_assign_driver", "dlt_pickup", "dlt_track", "dlt_deliver", "dlt_confirm"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AssignDriverWorker(), new PickupWorker(), new TrackWorker(), new DeliverWorker(), new ConfirmWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("delivery_tracking_733", 1,
                Map.of("orderId", "ORD-733", "restaurantAddr", "123 Main St", "customerAddr", "456 Oak Ave"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
