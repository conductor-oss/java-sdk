package serviceorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import serviceorchestration.workers.AuthenticateWorker;
import serviceorchestration.workers.CatalogLookupWorker;
import serviceorchestration.workers.AddToCartWorker;
import serviceorchestration.workers.CheckoutWorker;

import java.util.List;
import java.util.Map;

/**
 * Service Orchestration Demo
 *
 * Orchestrates multiple microservices in sequence:
 *   so_authenticate -> so_catalog_lookup -> so_add_to_cart -> so_checkout
 *
 * Run:
 *   java -jar target/service-orchestration-1.0.0.jar
 */
public class ServiceOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Service Orchestration Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "so_authenticate", "so_catalog_lookup", "so_add_to_cart", "so_checkout"));
        System.out.println("  Registered: so_authenticate, so_catalog_lookup, so_add_to_cart, so_checkout\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'service_orchestration_291'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AuthenticateWorker(),
                new CatalogLookupWorker(),
                new AddToCartWorker(),
                new CheckoutWorker()
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

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("service_orchestration_291", 1,
                Map.of("userId", "user-42",
                        "productId", "PROD-100",
                        "quantity", 2));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
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
