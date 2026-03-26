package eventsplit;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventsplit.workers.ReceiveCompositeWorker;
import eventsplit.workers.SplitEventWorker;
import eventsplit.workers.ProcessSubAWorker;
import eventsplit.workers.ProcessSubBWorker;
import eventsplit.workers.ProcessSubCWorker;
import eventsplit.workers.CombineResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Split Demo
 *
 * Splits a composite event into multiple sub-events for parallel processing:
 *   sp_receive_composite -> sp_split_event -> FORK(sp_process_sub_a, sp_process_sub_b, sp_process_sub_c)
 *   -> JOIN -> sp_combine_results
 *
 * Run:
 *   java -jar target/event-split-1.0.0.jar
 */
public class EventSplitExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Split Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sp_receive_composite", "sp_split_event",
                "sp_process_sub_a", "sp_process_sub_b", "sp_process_sub_c",
                "sp_combine_results"));
        System.out.println("  Registered: sp_receive_composite, sp_split_event, sp_process_sub_a, sp_process_sub_b, sp_process_sub_c, sp_combine_results\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'event_split'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveCompositeWorker(),
                new SplitEventWorker(),
                new ProcessSubAWorker(),
                new ProcessSubBWorker(),
                new ProcessSubCWorker(),
                new CombineResultsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_split", 1,
                Map.of("compositeEvent", Map.of(
                        "type", "purchase_complete",
                        "order", Map.of("orderId", "ORD-700", "items", 3, "total", 299.99),
                        "customer", Map.of("id", "CUST-88", "name", "Alice"),
                        "shipping", Map.of("method", "express", "address", "456 Oak Ave"))));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
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
