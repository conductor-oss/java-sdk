package eventdrivenworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventdrivenworkflow.workers.ReceiveEventWorker;
import eventdrivenworkflow.workers.ClassifyEventWorker;
import eventdrivenworkflow.workers.HandleOrderWorker;
import eventdrivenworkflow.workers.HandlePaymentWorker;
import eventdrivenworkflow.workers.HandleGenericWorker;

import java.util.List;
import java.util.Map;

/**
 * Event-Driven Workflow Demo
 *
 * Demonstrates a SWITCH-based event routing workflow:
 *   ed_receive_event -> ed_classify_event -> SWITCH(category:
 *       order  -> ed_handle_order,
 *       payment -> ed_handle_payment,
 *       default -> ed_handle_generic)
 *
 * Run:
 *   java -jar target/event-driven-workflow-1.0.0.jar
 */
public class EventDrivenWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event-Driven Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ed_receive_event", "ed_classify_event",
                "ed_handle_order", "ed_handle_payment", "ed_handle_generic"));
        System.out.println("  Registered: ed_receive_event, ed_classify_event, ed_handle_order, ed_handle_payment, ed_handle_generic\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_driven_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveEventWorker(),
                new ClassifyEventWorker(),
                new HandleOrderWorker(),
                new HandlePaymentWorker(),
                new HandleGenericWorker()
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
        String workflowId = client.startWorkflow("event_driven_wf", 1,
                Map.of("eventId", "evt-fixed-001",
                        "eventType", "order.created",
                        "eventData", Map.of(
                                "orderId", "ORD-9921",
                                "amount", 340.50,
                                "customer", "acme-corp")));
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
