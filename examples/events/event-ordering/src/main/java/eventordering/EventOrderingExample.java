package eventordering;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventordering.workers.BufferEventsWorker;
import eventordering.workers.SortEventsWorker;
import eventordering.workers.ProcessNextWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Ordering Demo
 *
 * Demonstrates buffering out-of-order events, sorting them by sequence
 * number, and processing each in order using a DO_WHILE loop.
 *   oo_buffer_events -> oo_sort_events -> DO_WHILE(oo_process_next)
 *
 * Run:
 *   java -jar target/event-ordering-1.0.0.jar
 */
public class EventOrderingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Ordering Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "oo_buffer_events", "oo_sort_events", "oo_process_next"));
        System.out.println("  Registered: oo_buffer_events, oo_sort_events, oo_process_next\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_ordering'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BufferEventsWorker(),
                new SortEventsWorker(),
                new ProcessNextWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_ordering", 1,
                Map.of("events", List.of(
                        Map.of("seq", 3, "type", "update", "data", "third"),
                        Map.of("seq", 1, "type", "create", "data", "first"),
                        Map.of("seq", 2, "type", "modify", "data", "second")
                )));
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
