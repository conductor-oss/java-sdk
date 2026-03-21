package eventhandlers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventhandlers.workers.ProcessEventWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Handlers — Trigger workflows from external events.
 *
 * Demonstrates the event-driven pattern: external events (Kafka, SQS, NATS)
 * trigger workflow starts via Conductor's event handler mechanism. This example
 * shows the workflow and worker side; event handlers themselves are registered
 * via REST API.
 *
 * Run:
 *   java -jar target/event-handlers-1.0.0.jar
 */
public class EventHandlersExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Handlers: Trigger Workflows from External Events ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("eh_process_event"));
        System.out.println("  Registered: eh_process_event\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_triggered_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ProcessEventWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Perform an event triggering the workflow
        System.out.println("Step 4: Performing event trigger (starting workflow)...\n");
        String workflowId = client.startWorkflow("event_triggered_workflow", 1,
                Map.of(
                        "eventType", "order.created",
                        "payload", Map.of("orderId", "ORD-12345", "amount", 99.99)
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
