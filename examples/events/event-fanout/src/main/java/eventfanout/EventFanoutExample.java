package eventfanout;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventfanout.workers.ReceiveEventWorker;
import eventfanout.workers.AnalyticsWorker;
import eventfanout.workers.StorageWorker;
import eventfanout.workers.NotificationWorker;
import eventfanout.workers.AggregateWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Fan-Out Workflow Demo
 *
 * Demonstrates a FORK_JOIN-based event fan-out workflow:
 *   fo_receive_event -> FORK(fo_analytics, fo_storage, fo_notification) -> JOIN -> fo_aggregate
 *
 * Run:
 *   java -jar target/event-fanout-1.0.0.jar
 */
public class EventFanoutExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Fan-Out Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fo_receive_event", "fo_analytics",
                "fo_storage", "fo_notification", "fo_aggregate"));
        System.out.println("  Registered: fo_receive_event, fo_analytics, fo_storage, fo_notification, fo_aggregate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_fanout_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveEventWorker(),
                new AnalyticsWorker(),
                new StorageWorker(),
                new NotificationWorker(),
                new AggregateWorker()
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
        String workflowId = client.startWorkflow("event_fanout_wf", 1,
                Map.of("eventId", "evt-fanout-001",
                        "eventType", "order.created",
                        "payload", Map.of(
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
