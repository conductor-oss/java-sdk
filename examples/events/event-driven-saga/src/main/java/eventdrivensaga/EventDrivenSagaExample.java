package eventdrivensaga;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventdrivensaga.workers.CreateOrderWorker;
import eventdrivensaga.workers.ProcessPaymentWorker;
import eventdrivensaga.workers.ShipOrderWorker;
import eventdrivensaga.workers.CompensatePaymentWorker;
import eventdrivensaga.workers.CancelOrderWorker;

import java.util.List;
import java.util.Map;

/**
 * Event-Driven Saga Demo
 *
 * Demonstrates a saga pattern with compensation:
 *   ds_create_order -> ds_process_payment -> SWITCH(paymentStatus:
 *       success  -> ds_ship_order,
 *       failed   -> ds_compensate_payment -> ds_cancel_order)
 *
 * Run:
 *   java -jar target/event-driven-saga-1.0.0.jar
 */
public class EventDrivenSagaExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event-Driven Saga Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ds_create_order", "ds_process_payment",
                "ds_ship_order", "ds_compensate_payment", "ds_cancel_order"));
        System.out.println("  Registered: ds_create_order, ds_process_payment, ds_ship_order, ds_compensate_payment, ds_cancel_order\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_driven_saga'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateOrderWorker(),
                new ProcessPaymentWorker(),
                new ShipOrderWorker(),
                new CompensatePaymentWorker(),
                new CancelOrderWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_driven_saga", 1,
                Map.of("orderId", "ORD-2001",
                        "amount", 149.99,
                        "shippingAddress", "123 Main St, Springfield, IL"));
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
