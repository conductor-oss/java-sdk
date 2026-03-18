package procurementworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import procurementworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 652: Procurement Workflow — Requisition to Payment
 *
 * Demonstrates: requisition -> approve -> purchase -> receive -> pay
 *
 * Run:
 *   java -jar target/procurement-workflow-1.0.0.jar
 */
public class ProcurementWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 652: Procurement Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "prw_requisition", "prw_approve", "prw_purchase", "prw_receive", "prw_pay"));
        System.out.println("  Registered 5 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'prw_procurement'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RequisitionWorker(),
                new ApproveWorker(),
                new PurchaseWorker(),
                new ReceiveWorker(),
                new PayWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("prw_procurement", 1,
                Map.of("item", "Office Laptops",
                       "quantity", 100,
                       "budget", 50000,
                       "requester", "eng-manager"));
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
