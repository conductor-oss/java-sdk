package bulkheadpattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bulkheadpattern.workers.ClassifyRequestWorker;
import bulkheadpattern.workers.AllocatePoolWorker;
import bulkheadpattern.workers.ExecuteRequestWorker;
import bulkheadpattern.workers.ReleasePoolWorker;

import java.util.List;
import java.util.Map;

/**
 * Bulkhead Pattern Demo
 *
 * Demonstrates resource isolation using task pools:
 *   bh_classify_request -> bh_allocate_pool -> bh_execute_request -> bh_release_pool
 *
 * Run:
 *   java -jar target/bulkhead-pattern-1.0.0.jar
 */
public class BulkheadPatternExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Bulkhead Pattern Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "bh_classify_request", "bh_allocate_pool",
                "bh_execute_request", "bh_release_pool"));
        System.out.println("  Registered: bh_classify_request, bh_allocate_pool, bh_execute_request, bh_release_pool\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'bulkhead_pattern_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyRequestWorker(),
                new AllocatePoolWorker(),
                new ExecuteRequestWorker(),
                new ReleasePoolWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("bulkhead_pattern_workflow", 1,
                Map.of("serviceName", "payment-service",
                        "priority", "high",
                        "request", Map.of("action", "charge")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
