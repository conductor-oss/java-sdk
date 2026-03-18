package forkjoin;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import forkjoin.workers.GetProductWorker;
import forkjoin.workers.GetInventoryWorker;
import forkjoin.workers.GetReviewsWorker;
import forkjoin.workers.MergeResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * FORK_JOIN Demo — Run Tasks in Parallel
 *
 * Demonstrates Conductor's FORK_JOIN pattern: three parallel branches
 * fetch product, inventory, and review data simultaneously, then a
 * JOIN waits for all branches before merging results into a product page.
 *
 * Run:
 *   java -jar target/fork-join-1.0.0.jar
 */
public class ForkJoinExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== FORK_JOIN Demo: Parallel Product Page Assembly ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fj_get_product", "fj_get_inventory", "fj_get_reviews", "fj_merge_results"));
        System.out.println("  Registered: fj_get_product, fj_get_inventory, fj_get_reviews, fj_merge_results\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'fork_join_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GetProductWorker(),
                new GetInventoryWorker(),
                new GetReviewsWorker(),
                new MergeResultsWorker()
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
        String workflowId = client.startWorkflow("fork_join_demo", 1,
                Map.of("productId", "PROD-001"));
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
