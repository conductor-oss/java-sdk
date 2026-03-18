package understandingworkflows;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import understandingworkflows.workers.CalculateTotalWorker;
import understandingworkflows.workers.SendConfirmationWorker;
import understandingworkflows.workers.ValidateOrderWorker;

import java.util.List;
import java.util.Map;

/**
 * Order Pipeline — Understanding Workflows, Tasks, and Workers
 *
 * Demonstrates the three core Conductor concepts using a 3-step order pipeline:
 *   validate_order -> calculate_total -> send_confirmation
 *
 * Run:
 *   java -jar target/understanding-workflows-1.0.0.jar
 *   java -jar target/understanding-workflows-1.0.0.jar --workers
 */
public class OrderPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Order Pipeline: Understanding Workflows, Tasks, and Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("validate_order", "calculate_total", "send_confirmation"));
        System.out.println("  Registered: validate_order, calculate_total, send_confirmation\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'order_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateOrderWorker(),
                new CalculateTotalWorker(),
                new SendConfirmationWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        Map<String, Object> input = Map.of(
                "orderId", "ORD-1001",
                "customerEmail", "alice@example.com",
                "items", List.of(
                        Map.of("name", "Laptop", "price", 999.99, "qty", 1),
                        Map.of("name", "Mouse", "price", 29.99, "qty", 2)
                )
        );
        String workflowId = client.startWorkflow("order_pipeline", 1, input);
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
