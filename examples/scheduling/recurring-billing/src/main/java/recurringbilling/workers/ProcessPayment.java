package recurringbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

/**
 * perform  payment processing against a saved payment method. Produces
 * deterministic output: payments succeed unless the payment method's last4
 * ends with "0000" (performing a declined card) or the amount exceeds
 * 1,000,000 cents ($10,000 — performing a processor limit).
 *
 * In production, this would integrate with Stripe, Braintree, or similar.
 */
public class ProcessPayment implements Worker {

    // deterministic processor limit: $10,000
    static final int MAX_CHARGE_CENTS = 1_000_000;

    // last4 values that perform decline
    static final String DECLINED_LAST4 = "0000";

    @Override
    public String getTaskDefName() {
        return "billing_process_payment";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("[billing_process_payment] Processing payment...");

        TaskResult result = new TaskResult(task);

        Map<String, Object> subscription = (Map<String, Object>) task.getInputData().get("subscription");
        Map<String, Object> charges = (Map<String, Object>) task.getInputData().get("charges");

        if (subscription == null || charges == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("subscription and charges data are required");
            return result;
        }

        int totalCents = ((Number) charges.get("totalCents")).intValue();
        String currency = (String) charges.getOrDefault("currency", "USD");
        Map<String, Object> paymentMethod = (Map<String, Object>) subscription.get("paymentMethod");
        String customerId = (String) subscription.get("customerId");
        String subscriptionId = (String) subscription.get("subscriptionId");

        if (paymentMethod == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("No payment method on file for customer " + customerId);
            return result;
        }

        String last4 = (String) paymentMethod.get("last4");
        String methodType = (String) paymentMethod.get("type");

        // Deterministic transaction ID from inputs
        String transactionId = "txn_" + Math.abs((customerId + "_" + totalCents).hashCode());

        // Perform payment gateway response
        Map<String, Object> paymentResult = new LinkedHashMap<>();
        paymentResult.put("transactionId", transactionId);
        paymentResult.put("amountCents", totalCents);
        paymentResult.put("currency", currency);
        paymentResult.put("paymentMethodType", methodType);
        paymentResult.put("paymentMethodLast4", last4);
        paymentResult.put("customerId", customerId);
        paymentResult.put("subscriptionId", subscriptionId);
        paymentResult.put("processedAt", Instant.now().toString());

        // Check for deterministic.decline conditions
        if (DECLINED_LAST4.equals(last4)) {
            paymentResult.put("status", "declined");
            paymentResult.put("declineCode", "card_declined");
            paymentResult.put("declineMessage", "The card ending in " + last4
                    + " was declined. Please update your payment method.");
            paymentResult.put("retryable", true);

            System.out.println("  DECLINED: Card ending in " + last4
                    + " for $" + formatCents(totalCents));

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("payment", paymentResult);
            return result;
        }

        if (totalCents > MAX_CHARGE_CENTS) {
            paymentResult.put("status", "declined");
            paymentResult.put("declineCode", "amount_too_large");
            paymentResult.put("declineMessage", "Charge of $" + formatCents(totalCents)
                    + " exceeds the single-transaction limit of $" + formatCents(MAX_CHARGE_CENTS));
            paymentResult.put("retryable", false);

            System.out.println("  DECLINED: Amount $" + formatCents(totalCents)
                    + " exceeds limit");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("payment", paymentResult);
            return result;
        }

        // Payment succeeds
        paymentResult.put("status", "succeeded");
        paymentResult.put("declineCode", null);
        paymentResult.put("declineMessage", null);
        paymentResult.put("retryable", false);

        System.out.println("  SUCCESS: $" + formatCents(totalCents)
                + " charged to " + methodType + " ending " + last4
                + " (txn: " + transactionId + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("payment", paymentResult);
        return result;
    }

    private static String formatCents(int cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
