package eventdrivenmicroservices;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventdrivenmicroservices.workers.OrderServiceWorker;
import eventdrivenmicroservices.workers.EmitOrderCreatedWorker;
import eventdrivenmicroservices.workers.PaymentServiceWorker;
import eventdrivenmicroservices.workers.EmitPaymentProcessedWorker;
import eventdrivenmicroservices.workers.ShippingServiceWorker;
import eventdrivenmicroservices.workers.EmitShipmentCreatedWorker;
import eventdrivenmicroservices.workers.NotificationServiceWorker;
import eventdrivenmicroservices.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Event-Driven Microservices Demo
 *
 * Demonstrates a sequential event-driven microservices architecture:
 *   order_service -> emit_order_created -> payment_service -> emit_payment_processed
 *   -> shipping_service -> emit_shipment_created -> notification_service -> finalize
 *
 * Run:
 *   java -jar target/event-driven-microservices-1.0.0.jar
 */
public class EventDrivenMicroservicesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event-Driven Microservices Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dm_order_service", "dm_emit_order_created",
                "dm_payment_service", "dm_emit_payment_processed",
                "dm_shipping_service", "dm_emit_shipment_created",
                "dm_notification_service", "dm_finalize"));
        System.out.println("  Registered: dm_order_service, dm_emit_order_created, dm_payment_service, dm_emit_payment_processed, dm_shipping_service, dm_emit_shipment_created, dm_notification_service, dm_finalize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_driven_microservices'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new OrderServiceWorker(),
                new EmitOrderCreatedWorker(),
                new PaymentServiceWorker(),
                new EmitPaymentProcessedWorker(),
                new ShippingServiceWorker(),
                new EmitShipmentCreatedWorker(),
                new NotificationServiceWorker(),
                new FinalizeWorker()
        );
        client.startWorkers(workers);
        System.out.println("  8 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_driven_microservices", 1,
                Map.of("customerId", "CUST-DM-100",
                        "items", List.of(
                                Map.of("name", "Laptop Stand", "price", 49.99, "qty", 1),
                                Map.of("name", "USB-C Hub", "price", 34.99, "qty", 2),
                                Map.of("name", "Monitor Light Bar", "price", 79.99, "qty", 1)),
                        "shippingAddress", "789 Elm Street, San Jose, CA 95112",
                        "paymentMethod", "credit_card"));
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
