package choreographyvsorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import choreographyvsorchestration.workers.*;
import java.util.List;
import java.util.Map;

public class ChoreographyVsOrchestrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 330: Choreography vs Orchestration ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cvo_place_order", "cvo_reserve_inventory", "cvo_process_payment", "cvo_ship_order"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new PlaceOrderWorker(), new ReserveInventoryWorker(), new ProcessPaymentWorker(), new ShipOrderWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("--- Orchestrated Flow (Conductor) ---");
        String wfId = client.startWorkflow("orchestrated_order_flow", 1,
                Map.of("orderId", "ORD-900", "items", List.of("widget"), "customerId", "CUST-1"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Tracking: " + wf.getOutput().get("trackingId"));

        System.out.println("\n--- Comparison ---");
        System.out.println("  Choreography: Services emit events, no central view");
        System.out.println("  Orchestration: Conductor coordinates, full visibility");
        System.out.println("  Winner for complex flows: Orchestration");

        client.stopWorkers();
        String status = wf.getStatus().name();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
