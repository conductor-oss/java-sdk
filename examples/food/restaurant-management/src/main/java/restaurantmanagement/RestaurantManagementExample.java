package restaurantmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import restaurantmanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 732: Restaurant Management — Reservations, Seating, Order, Kitchen, Checkout
 */
public class RestaurantManagementExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 732: Restaurant Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("rst_reservations", "rst_seating", "rst_order", "rst_kitchen", "rst_checkout"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ReservationsWorker(), new SeatingWorker(), new OrderWorker(),
                new KitchenWorker(), new CheckoutWorker());
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("restaurant_management_732", 1,
                Map.of("guestName", "Smith", "partySize", 4));

        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
