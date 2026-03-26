package recurringbilling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import recurringbilling.workers.*;

import java.util.*;

/**
 * Recurring Billing — Invoice Generation, Payment Processing, and Notification
 *
 * Fetches subscription details, calculates charges (prorating, discounts, tax),
 * processes payment against a saved method, generates an invoice, and sends a
 * billing notification (receipt or failure notice). Conductor handles the
 * sequence, retries on payment gateway failures, and provides a complete audit
 * trail linking every invoice to its payment attempt and notification.
 *
 * Uses conductor-oss Java SDK v5 from https://github.com/conductor-oss/conductor/tree/main/conductor-clients
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/recurring-billing-1.0.0.jar
 */
public class RecurringBillingExample {

    private static final List<String> TASK_NAMES = List.of(
            "billing_fetch_subscription",
            "billing_calculate_charges",
            "billing_process_payment",
            "billing_generate_invoice",
            "billing_send_notification"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new FetchSubscription(),
                new CalculateCharges(),
                new ProcessPayment(),
                new GenerateInvoice(),
                new SendBillingNotification()
        );
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Recurring Billing Demo: Invoice Generation & Payment Processing ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'recurring_billing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Use the Conductor CLI or UI to start workflows.\n");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Allow workers to start polling
        Thread.sleep(2000);

        // Step 4 — Run billing for a Pro subscriber with loyalty discount
        System.out.println("Step 4: Running recurring billing for sample subscriber...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("customerId", "cust_1024");
        input.put("planId", "plan_pro");

        String workflowId = client.startWorkflow("recurring_billing", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Billing Results ---");

            Map<String, Object> invoice = (Map<String, Object>) output.get("invoice");
            if (invoice != null) {
                System.out.println("  Invoice        : " + invoice.get("invoiceNumber"));
                System.out.println("  Customer       : " + invoice.get("customerName"));
                System.out.println("  Amount         : $" + formatCents(((Number) invoice.get("totalCents")).intValue()));
                System.out.println("  Payment status : " + invoice.get("paymentStatus"));
                System.out.println("  Invoice status : " + invoice.get("status"));
            }

            Map<String, Object> notification = (Map<String, Object>) output.get("notification");
            if (notification != null) {
                System.out.println("  Notification   : " + notification.get("type")
                        + " sent to " + notification.get("recipientEmail"));
            }

            Map<String, Object> charges = (Map<String, Object>) output.get("charges");
            if (charges != null) {
                System.out.println("\n--- Charge Breakdown ---");
                System.out.println("  Base price     : $" + formatCents(((Number) charges.get("basePriceCents")).intValue()));
                if (Boolean.TRUE.equals(charges.get("prorated"))) {
                    System.out.println("  Prorated to    : $" + formatCents(((Number) charges.get("proratedCents")).intValue()));
                }
                if (Boolean.TRUE.equals(charges.get("discountApplied"))) {
                    System.out.println("  Discount (" + charges.get("discountPercent") + "%) : -$"
                            + formatCents(((Number) charges.get("discountCents")).intValue()));
                }
                System.out.println("  Subtotal       : $" + formatCents(((Number) charges.get("subtotalCents")).intValue()));
                System.out.println("  Tax            : $" + formatCents(((Number) charges.get("taxCents")).intValue()));
                System.out.println("  Total          : $" + formatCents(((Number) charges.get("totalCents")).intValue()));
            }
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nWorkflow did not complete (status: " + status + ")");
            workflow.getTasks().stream()
                    .filter(t -> t.getStatus().name().equals("FAILED"))
                    .forEach(t -> System.out.println("  Failed task: " + t.getReferenceTaskName()
                            + " — " + t.getReasonForIncompletion()));
            System.out.println("Result: BILLING_ERROR");
            System.exit(1);
        }

        // Check payment outcome
        Map<String, Object> paymentResult = output != null
                ? (Map<String, Object>) output.get("payment") : null;
        if (paymentResult != null && "declined".equals(paymentResult.get("status"))) {
            System.out.println("\nResult: PAYMENT_FAILED — " + paymentResult.get("declineMessage"));
            System.exit(1);
        } else {
            System.out.println("\nResult: BILLING_SUCCESS — invoice generated and payment processed");
            System.exit(0);
        }
    }

    private static String formatCents(int cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
