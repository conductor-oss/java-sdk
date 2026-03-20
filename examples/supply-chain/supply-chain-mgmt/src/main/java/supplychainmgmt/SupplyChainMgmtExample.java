package supplychainmgmt;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import supplychainmgmt.workers.PlanWorker;
import supplychainmgmt.workers.SourceWorker;
import supplychainmgmt.workers.MakeWorker;
import supplychainmgmt.workers.DeliverWorker;
import supplychainmgmt.workers.ReturnWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 651: Supply Chain Management — End-to-End Supply Chain Flow
 *
 * Demonstrates: plan -> source -> make -> deliver -> return
 *
 * Run:
 *   java -jar target/supply-chain-mgmt-1.0.0.jar
 */
public class SupplyChainMgmtExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 651: Supply Chain Management ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "scm_plan", "scm_source", "scm_make", "scm_deliver", "scm_return"));
        System.out.println("  Registered 5 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'scm_supply_chain'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PlanWorker(),
                new SourceWorker(),
                new MakeWorker(),
                new DeliverWorker(),
                new ReturnWorker()
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
        String workflowId = client.startWorkflow("scm_supply_chain", 1,
                Map.of("product", "Industrial Sensor",
                       "quantity", 500,
                       "destination", "Chicago Distribution Center"));
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
