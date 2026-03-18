package checkoutflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import checkoutflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 453: Checkout Flow
 *
 * Runs an e-commerce checkout workflow:
 * validate cart -> calculate tax -> process payment -> confirm order.
 *
 * Run:
 *   java -jar target/checkout-flow-1.0.0.jar
 */
public class CheckoutFlowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 453: Checkout Flow ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "chk_validate_cart", "chk_calculate_tax", "chk_process_payment", "chk_confirm_order"));
        System.out.println("  Registered 4 tasks.\n");

        System.out.println("Step 2: Registering workflow 'checkout_flow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateCartWorker(),
                new CalculateTaxWorker(),
                new ProcessPaymentWorker(),
                new ConfirmOrderWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("checkout_flow", 1, Map.of(
                "cartId", "cart-abc123",
                "userId", "usr-501",
                "shippingAddress", Map.of("street", "123 Main St", "city", "San Francisco", "state", "CA", "zip", "94105"),
                "paymentMethod", Map.of("type", "credit_card", "last4", "4242")));
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
