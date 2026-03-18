package dowhile;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dowhile.workers.ProcessItemWorker;
import dowhile.workers.SummarizeWorker;

import java.util.List;
import java.util.Map;

/**
 * DO_WHILE Loop Demo — Iterate until a condition is met
 *
 * Demonstrates the DO_WHILE task pattern in Conductor:
 * a loop task repeatedly executes dw_process_item until
 * iteration >= batchSize, then dw_summarize reports the results.
 *
 * Run:
 *   java -jar target/do-while-1.0.0.jar
 */
public class DoWhileExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== DO_WHILE Loop Demo: Process Items in Batch ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("dw_process_item", "dw_summarize"));
        System.out.println("  Registered: dw_process_item, dw_summarize\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'do_while_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ProcessItemWorker(),
                new SummarizeWorker()
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
        String workflowId = client.startWorkflow("do_while_demo", 1,
                Map.of("batchSize", 5));
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
