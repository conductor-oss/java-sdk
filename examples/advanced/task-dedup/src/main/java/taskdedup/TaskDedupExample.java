package taskdedup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskdedup.workers.HashInputWorker;
import taskdedup.workers.CheckSeenWorker;
import taskdedup.workers.ExecuteNewWorker;
import taskdedup.workers.ReturnCachedWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 581: Task Deduplication -- Hash-Based Duplicate Detection
 *
 * Demonstrates a SWITCH-based deduplication workflow:
 *   tdd_hash_input -> tdd_check_seen -> SWITCH(status:
 *       new -> tdd_execute_new,
 *       dup -> tdd_return_cached,
 *       default -> tdd_execute_new)
 *
 * Run:
 *   java -jar target/task-dedup-1.0.0.jar
 */
public class TaskDedupExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 581: Task Deduplication ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tdd_hash_input", "tdd_check_seen",
                "tdd_execute_new", "tdd_return_cached"));
        System.out.println("  Registered: tdd_hash_input, tdd_check_seen, tdd_execute_new, tdd_return_cached\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'tdd_task_dedup'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new HashInputWorker(),
                new CheckSeenWorker(),
                new ExecuteNewWorker(),
                new ReturnCachedWorker()
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
        String workflowId = client.startWorkflow("tdd_task_dedup", 1,
                Map.of("payload", Map.of("orderId", "ORD-123", "amount", 99.99),
                        "cacheEnabled", true));
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
