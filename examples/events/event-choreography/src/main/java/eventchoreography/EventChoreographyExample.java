package eventchoreography;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventchoreography.workers.OrderServiceWorker;
import eventchoreography.workers.EmitOrderEventWorker;
import eventchoreography.workers.PaymentServiceWorker;
import eventchoreography.workers.EmitPaymentEventWorker;
import eventchoreography.workers.InventoryServiceWorker;
import eventchoreography.workers.EmitInventoryEventWorker;
import eventchoreography.workers.NotificationServiceWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Choreography Demo
 *
 * Choreography pattern: services communicate through events with no central
 * orchestrator (deterministic.in Conductor). Each service emits an event that
 * triggers the next service.
 *
 * Pattern:
 *   ch_order_service -> ch_emit_order_event -> ch_payment_service ->
 *   ch_emit_payment_event -> ch_inventory_service -> ch_emit_inventory_event ->
 *   ch_notification_service
 *
 * Run:
 *   java -jar target/event-choreography-1.0.0.jar
 */
public class EventChoreographyExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Choreography Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ch_order_service", "ch_emit_order_event",
                "ch_payment_service", "ch_emit_payment_event",
                "ch_inventory_service", "ch_emit_inventory_event",
                "ch_notification_service"));
        System.out.println("  Registered: ch_order_service, ch_emit_order_event, ch_payment_service, ch_emit_payment_event, ch_inventory_service, ch_emit_inventory_event, ch_notification_service\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_choreography'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new OrderServiceWorker(),
                new EmitOrderEventWorker(),
                new PaymentServiceWorker(),
                new EmitPaymentEventWorker(),
                new InventoryServiceWorker(),
                new EmitInventoryEventWorker(),
                new NotificationServiceWorker()
        );
        client.startWorkers(workers);
        System.out.println("  7 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_choreography", 1,
                Map.of("orderId", "ORD-CHOR-100",
                        "customerId", "CUST-42",
                        "items", List.of(
                                Map.of("sku", "ITEM-A", "price", 29.99),
                                Map.of("sku", "ITEM-B", "price", 49.99))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
