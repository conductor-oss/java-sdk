package forkindowhile;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import forkindowhile.workers.ProcessBatchWorker;
import forkindowhile.workers.SummaryWorker;

import java.util.List;
import java.util.Map;

/**
 * FORK inside DO_WHILE Demo — Iterative Parallel Processing
 *
 * Demonstrates a FORK_JOIN inside a DO_WHILE loop in Conductor:
 * each iteration of the loop forks parallel tasks to process items,
 * then a JOIN collects the results before the next iteration.
 * After the loop, fl_summary produces a final report.
 *
 * Run:
 *   java -jar target/fork-in-do-while-1.0.0.jar
 */
public class ForkInDoWhileExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== FORK inside DO_WHILE Demo: Iterative Parallel Processing ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("fl_process_batch", "fl_summary"));
        System.out.println("  Registered: fl_process_batch, fl_summary\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'fork_loop_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessBatchWorker(),
                new SummaryWorker()
        );
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("fork_loop_demo", 1,
                Map.of("totalBatches", 3));
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
