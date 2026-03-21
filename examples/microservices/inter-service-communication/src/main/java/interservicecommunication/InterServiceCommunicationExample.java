package interservicecommunication;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import interservicecommunication.workers.*;
import java.util.List;
import java.util.Map;

public class InterServiceCommunicationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 305: Inter-Service Communication ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("isc_order_service", "isc_inventory_service", "isc_shipping_service", "isc_notification_service"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new OrderServiceWorker(), new InventoryServiceWorker(), new ShippingServiceWorker(), new NotificationServiceWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("inter_service_comm_workflow", 1,
                Map.of("orderId", "ORD-500", "items", List.of("widget-a", "gadget-b"), "customerId", "CUST-42"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
