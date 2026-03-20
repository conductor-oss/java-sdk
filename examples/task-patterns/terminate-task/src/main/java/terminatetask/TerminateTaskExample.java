package terminatetask;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import terminatetask.workers.ProcessWorker;
import terminatetask.workers.ValidateWorker;

import java.util.List;
import java.util.Map;

/**
 * TERMINATE Task — Early Exit with Custom Output
 *
 * Demonstrates the TERMINATE task for early workflow exit.
 * Uses SWITCH + TERMINATE for conditional early exit based on validation.
 *
 * TERMINATE immediately ends a workflow with a specified status and output.
 * Useful for:
 * - Validation failures (stop before doing expensive work)
 * - Short-circuit evaluation (skip remaining steps)
 * - Conditional early success (no more work needed)
 *
 * Run:
 *   java -jar target/terminate-task-1.0.0.jar
 */
public class TerminateTaskExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== TERMINATE Task: Early Workflow Exit and Validation ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("term_validate", "term_process"));
        System.out.println("  Registered: term_validate, term_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'terminate_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new ValidateWorker(), new ProcessWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: Valid order ($500 USD) — should COMPLETE with APPROVED
        System.out.println("--- Scenario 1: Valid order ($500 USD) ---");
        String wfId1 = client.startWorkflow("terminate_demo", 1,
                Map.of("orderId", "ORD-001", "amount", 500, "currency", "USD"));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + wf1.getOutput());

        if (!"COMPLETED".equals(status1) || !"APPROVED".equals(wf1.getOutput().get("status"))) {
            System.out.println("  UNEXPECTED — expected COMPLETED with APPROVED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: workflow completed, order approved.\n");
        }

        // Scenario 2: Invalid order ($50,000 BTC) — should FAIL via TERMINATE with REJECTED
        System.out.println("--- Scenario 2: Invalid order ($50,000 BTC) ---");
        String wfId2 = client.startWorkflow("terminate_demo", 1,
                Map.of("orderId", "ORD-002", "amount", 50000, "currency", "BTC"));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "FAILED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + wf2.getOutput());

        if (!"FAILED".equals(status2) || !"REJECTED".equals(wf2.getOutput().get("status"))) {
            System.out.println("  UNEXPECTED — expected FAILED with REJECTED\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: workflow failed via TERMINATE, order rejected.\n");
        }

        // Summary
        System.out.println("Key insight: TERMINATE skipped the 'process' step entirely.");
        System.out.println("The workflow ended immediately after validation failed.");

        client.stopWorkers();

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
