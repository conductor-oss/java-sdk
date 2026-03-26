package nestedsubworkflows;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import nestedsubworkflows.workers.CheckFraudWorker;
import nestedsubworkflows.workers.ChargeWorker;
import nestedsubworkflows.workers.FulfillWorker;

import java.util.List;
import java.util.Map;

/**
 * Nested Sub-Workflows Demo — Multi-level Orchestration (3 levels deep)
 *
 * Hierarchy:
 *   Level 1 (root): nested_order     — Order workflow
 *   Level 2:        nested_payment   — Payment (calls fraud check sub-workflow)
 *   Level 3:        nested_fraud_check — Fraud checking (deepest child)
 *
 * Run:
 *   java -jar target/nested-sub-workflows-1.0.0.jar
 *   java -jar target/nested-sub-workflows-1.0.0.jar --workers
 */
public class NestedSubWorkflowsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Nested Sub-Workflows Demo: Order -> Payment -> Fraud Check ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("nest_check_fraud", "nest_charge", "nest_fulfill"));
        System.out.println("  Registered: nest_check_fraud, nest_charge, nest_fulfill\n");

        // Step 2 — Register workflows (deepest child first: fraud -> payment -> order)
        System.out.println("Step 2: Registering workflows (deepest first)...");
        client.registerWorkflow("fraud-workflow.json");
        System.out.println("  Registered: nested_fraud_check (Level 3 — deepest)");
        client.registerWorkflow("payment-workflow.json");
        System.out.println("  Registered: nested_payment (Level 2)");
        client.registerWorkflow("order-workflow.json");
        System.out.println("  Registered: nested_order (Level 1 — root)\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckFraudWorker(),
                new ChargeWorker(),
                new FulfillWorker()
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

        // Step 4 — Start the root workflow
        System.out.println("Step 4: Starting root workflow 'nested_order'...\n");
        String workflowId = client.startWorkflow("nested_order", 1,
                Map.of(
                        "orderId", "ORD-5001",
                        "amount", 149.99,
                        "email", "customer@example.com",
                        "items", List.of("Widget A", "Widget B")
                ));
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
