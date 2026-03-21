package eventdedup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventdedup.workers.ComputeHashWorker;
import eventdedup.workers.CheckSeenWorker;
import eventdedup.workers.ProcessEventWorker;
import eventdedup.workers.SkipEventWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Deduplication Demo
 *
 * Demonstrates event dedup using a SWITCH-based workflow:
 *   dd_compute_hash -> dd_check_seen -> SWITCH(new->dd_process_event, duplicate->dd_skip_event)
 *
 * Run:
 *   java -jar target/event-dedup-1.0.0.jar
 */
public class EventDedupExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Deduplication Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dd_compute_hash", "dd_check_seen",
                "dd_process_event", "dd_skip_event"));
        System.out.println("  Registered: dd_compute_hash, dd_check_seen, dd_process_event, dd_skip_event\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_dedup'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ComputeHashWorker(),
                new CheckSeenWorker(),
                new ProcessEventWorker(),
                new SkipEventWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_dedup", 1,
                Map.of("eventId", "evt-1001",
                        "eventPayload", Map.of(
                                "type", "order.created",
                                "orderId", "ORD-555",
                                "amount", 99.99)));
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
