package stripeintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import stripeintegration.workers.CreateCustomerWorker;
import stripeintegration.workers.PaymentIntentWorker;
import stripeintegration.workers.ChargeWorker;
import stripeintegration.workers.SendReceiptWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 435: Stripe Integration
 *
 * Performs a Stripe payment integration workflow:
 * create customer -> payment intent -> charge -> send receipt.
 *
 * Run:
 *   java -jar target/stripe-integration-1.0.0.jar
 */
public class StripeIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 435: Stripe Integration ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "stp_create_customer", "stp_payment_intent", "stp_charge", "stp_send_receipt"));
        System.out.println("  Registered: stp_create_customer, stp_payment_intent, stp_charge, stp_send_receipt\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'stripe_integration_435'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateCustomerWorker(),
                new PaymentIntentWorker(),
                new ChargeWorker(),
                new SendReceiptWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("stripe_integration_435", 1,
                Map.of("email", "customer@example.com",
                        "amount", 4999,
                        "currency", "usd",
                        "description", "Premium subscription"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Customer ID: " + workflow.getOutput().get("customerId"));
        System.out.println("  Charge ID: " + workflow.getOutput().get("chargeId"));
        System.out.println("  Receipt Sent: " + workflow.getOutput().get("receiptSent"));

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
