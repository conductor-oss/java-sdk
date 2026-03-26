package returnsprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import returnsprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 458: Returns Processing
 *
 * Performs a returns processing workflow with SWITCH:
 * receive return -> inspect -> SWITCH(refund/exchange/reject) -> process outcome.
 */
public class ReturnsProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 458: Returns Processing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("ret_receive", "ret_inspect", "ret_refund", "ret_exchange", "ret_reject"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow 'returns_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveReturnWorker(),
                new InspectReturnWorker(),
                new RefundWorker(),
                new ExchangeWorker(),
                new RejectWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("returns_processing", 1, Map.of(
                "orderId", "ORD-8801",
                "returnReason", "Item did not match description",
                "items", List.of(Map.of("sku", "WH-1000XM5", "name", "Wireless Headphones", "qty", 1, "price", 129.99)),
                "customerId", "cust-301"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
