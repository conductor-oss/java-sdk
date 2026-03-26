package workflowvariables;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowvariables.workers.BuildSummaryWorker;
import workflowvariables.workers.CalcPriceWorker;
import workflowvariables.workers.CalcShippingWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Variables -- Persisting State Across Tasks
 *
 * Demonstrates how Conductor workflow variables and JSONPath expressions
 * let data flow across task boundaries:
 * - ${workflow.input.x} -- access workflow input
 * - ${taskRef.output.y} -- access a prior task's output
 * - INLINE tasks for computed values (tier-based discount)
 * - A final summary task that pulls from all preceding tasks
 *
 * Run:
 *   java -jar target/workflow-variables-1.0.0.jar
 */
public class WorkflowVariablesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Variables: Persisting State Across Tasks ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("wv_calc_price", "wv_calc_shipping", "wv_build_summary"));
        System.out.println("  Registered: wv_calc_price, wv_calc_shipping, wv_build_summary\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'workflow_variables_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CalcPriceWorker(),
                new CalcShippingWorker(),
                new BuildSummaryWorker()
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

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("workflow_variables_demo", 1,
                Map.of(
                        "orderId", "ORD-5001",
                        "customerTier", "gold",
                        "items", List.of(
                                Map.of("name", "Laptop Stand", "price", 49.99, "qty", 1),
                                Map.of("name", "USB Cable", "price", 12.99, "qty", 3)
                        )
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
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
