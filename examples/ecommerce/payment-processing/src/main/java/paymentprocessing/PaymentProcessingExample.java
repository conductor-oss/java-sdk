package paymentprocessing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import paymentprocessing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 454: Payment Processing
 *
 * Real payment processing workflow using the Stripe Java SDK in test mode:
 * validate -> authorize (create PaymentIntent) -> capture -> generate receipt -> reconcile.
 *
 * Requires STRIPE_API_KEY environment variable (use sk_test_ key for test mode).
 */
public class PaymentProcessingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 454: Payment Processing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("pay_validate", "pay_authorize", "pay_capture", "pay_receipt", "pay_reconcile"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow 'payment_processing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidatePaymentWorker(),
                new AuthorizePaymentWorker(),
                new CapturePaymentWorker(),
                new ReceiptWorker(),
                new ReconcileWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("payment_processing", 1, Map.of(
                "orderId", "ORD-8801",
                "amount", 259.97,
                "currency", "USD",
                "paymentMethod", Map.of("type", "credit_card", "brand", "visa", "last4", "1234"),
                "merchantId", "merch-100"));
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
