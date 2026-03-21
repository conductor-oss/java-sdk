package eventbatching;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventbatching.workers.CollectEventsWorker;
import eventbatching.workers.CreateBatchesWorker;
import eventbatching.workers.ProcessBatchWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Batching Demo
 *
 * Demonstrates a pipeline that collects events, splits them into batches,
 * then processes each batch in a DO_WHILE loop:
 *   eb_collect_events -> eb_create_batches -> DO_WHILE(eb_process_batch)
 *
 * Run:
 *   java -jar target/event-batching-1.0.0.jar
 */
public class EventBatchingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Batching Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "eb_collect_events", "eb_create_batches", "eb_process_batch"));
        System.out.println("  Registered: eb_collect_events, eb_create_batches, eb_process_batch\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_batching'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CollectEventsWorker(),
                new CreateBatchesWorker(),
                new ProcessBatchWorker()
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
        String workflowId = client.startWorkflow("event_batching", 1,
                Map.of("events", List.of(
                                Map.of("id", "e1", "type", "click"),
                                Map.of("id", "e2", "type", "view"),
                                Map.of("id", "e3", "type", "click"),
                                Map.of("id", "e4", "type", "purchase"),
                                Map.of("id", "e5", "type", "view"),
                                Map.of("id", "e6", "type", "click")),
                        "batchSize", 3));
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
