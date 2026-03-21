package exclusivejoin;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import exclusivejoin.workers.VendorAWorker;
import exclusivejoin.workers.VendorBWorker;
import exclusivejoin.workers.VendorCWorker;
import exclusivejoin.workers.SelectBestWorker;

import java.util.List;
import java.util.Map;

/**
 * EXCLUSIVE_JOIN Demo — First-Response-Wins / Vendor Racing Pattern
 *
 * Demonstrates Conductor's FORK_JOIN pattern for parallel vendor queries:
 * three vendor branches run in parallel, a JOIN waits for all to complete,
 * then a select_best worker picks the winner based on lowest price and
 * fastest response time.
 *
 * Run:
 *   java -jar target/exclusive-join-1.0.0.jar
 */
public class ExclusiveJoinExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== EXCLUSIVE_JOIN Demo: Vendor Racing / First-Response-Wins ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ej_vendor_a", "ej_vendor_b", "ej_vendor_c", "ej_select_best"));
        System.out.println("  Registered: ej_vendor_a, ej_vendor_b, ej_vendor_c, ej_select_best\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'exclusive_join_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new VendorAWorker(),
                new VendorBWorker(),
                new VendorCWorker(),
                new SelectBestWorker()
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
        String workflowId = client.startWorkflow("exclusive_join_demo", 1,
                Map.of("query", "wireless-keyboard"));
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
