package paymentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reconciles a captured payment by verifying it against Stripe records.
 *
 * Real reconciliation logic:
 *   - Retrieves the PaymentIntent from Stripe and verifies status is "succeeded"
 *   - Verifies the captured amount matches the expected amount
 *   - Calculates the settlement date (T+2 business days for card payments)
 *   - Computes Stripe processing fees (2.9% + $0.30 for domestic cards)
 *   - Records the net amount after fees
 *
 * Requires: STRIPE_API_KEY environment variable.
 */
public class ReconcileWorker implements Worker {

    private static final double STRIPE_PERCENTAGE_FEE = 0.029;
    private static final double STRIPE_FIXED_FEE = 0.30;

    private final String stripeApiKey;

    public ReconcileWorker() {
        this.stripeApiKey = System.getenv("STRIPE_API_KEY");
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            System.out.println("  [reconcile] STRIPE_API_KEY not set — running in mock mode.");
        }
    }

    @Override
    public String getTaskDefName() {
        return "pay_reconcile";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String captureId = task.getInputData().get("captureId") != null
                ? task.getInputData().get("captureId").toString() : "";
        String merchantId = task.getInputData().get("merchantId") != null
                ? task.getInputData().get("merchantId").toString() : "UNKNOWN";
        Object amountObj = task.getInputData().get("amount");
        double expectedAmount = 0;
        if (amountObj instanceof Number) expectedAmount = ((Number) amountObj).doubleValue();

        // Mock mode when Stripe key is not configured
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            double processingFee = Math.round((expectedAmount * STRIPE_PERCENTAGE_FEE + STRIPE_FIXED_FEE) * 100.0) / 100.0;
            double netAmount = Math.round((expectedAmount - processingFee) * 100.0) / 100.0;
            String settlementDate = calculateSettlementDate(Instant.now());
            System.out.println("  [reconcile] Capture " + captureId + ": $" + expectedAmount
                    + " reconciled for merchant " + merchantId + " (mock)");
            output.put("reconciled", true);
            output.put("stripeStatus", "succeeded");
            output.put("expectedAmount", expectedAmount);
            output.put("actualAmount", expectedAmount);
            output.put("amountMatches", true);
            output.put("processingFee", processingFee);
            output.put("netAmount", netAmount);
            output.put("settlementDate", settlementDate);
            output.put("merchantId", merchantId);
            output.put("reconciledAt", Instant.now().toString());
            output.put("demoMode", true);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        try {
            Stripe.apiKey = stripeApiKey;

            // Retrieve and verify the PaymentIntent
            PaymentIntent intent = PaymentIntent.retrieve(captureId);
            String stripeStatus = intent.getStatus();
            long amountReceived = intent.getAmountReceived();
            double actualAmount = amountReceived / 100.0;

            // Verify status
            boolean statusOk = "succeeded".equals(stripeStatus);

            // Verify amount matches (within 1 cent tolerance for rounding)
            boolean amountMatches = Math.abs(actualAmount - expectedAmount) < 0.02;

            boolean reconciled = statusOk && amountMatches;

            // Calculate processing fees (Stripe standard: 2.9% + $0.30)
            double processingFee = Math.round((actualAmount * STRIPE_PERCENTAGE_FEE + STRIPE_FIXED_FEE) * 100.0) / 100.0;
            double netAmount = Math.round((actualAmount - processingFee) * 100.0) / 100.0;

            // Settlement date: T+2 business days
            String settlementDate = calculateSettlementDate(Instant.now());

            System.out.println("  [reconcile] Capture " + captureId
                    + ": $" + actualAmount + " (expected $" + expectedAmount + ")"
                    + " -> reconciled=" + reconciled + ", net=$" + netAmount
                    + ", merchant=" + merchantId);

            output.put("reconciled", reconciled);
            output.put("stripeStatus", stripeStatus);
            output.put("expectedAmount", expectedAmount);
            output.put("actualAmount", actualAmount);
            output.put("amountMatches", amountMatches);
            output.put("processingFee", processingFee);
            output.put("netAmount", netAmount);
            output.put("settlementDate", settlementDate);
            output.put("merchantId", merchantId);
            output.put("reconciledAt", Instant.now().toString());

            if (!reconciled) {
                StringBuilder reason = new StringBuilder();
                if (!statusOk) reason.append("Stripe status is '").append(stripeStatus).append("' (expected 'succeeded'). ");
                if (!amountMatches) reason.append("Amount mismatch: expected $").append(expectedAmount)
                        .append(" but got $").append(actualAmount).append(".");
                output.put("reconciliationError", reason.toString().trim());
            }

            result.setStatus(reconciled ? TaskResult.Status.COMPLETED : TaskResult.Status.FAILED);
            if (!reconciled) {
                result.setReasonForIncompletion("Reconciliation failed");
            }

        } catch (Exception e) {
            System.err.println("  [reconcile] Stripe error for " + captureId + ": " + e.getMessage());
            output.put("reconciled", false);
            output.put("error", e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Reconciliation failed: " + e.getMessage());
        }

        result.setOutputData(output);
        return result;
    }

    /**
     * Calculates the settlement date as T+2 business days from now.
     * Skips weekends (Saturday/Sunday).
     */
    private String calculateSettlementDate(Instant from) {
        LocalDate date = from.atZone(ZoneId.of("UTC")).toLocalDate();
        int businessDays = 0;
        while (businessDays < 2) {
            date = date.plusDays(1);
            if (date.getDayOfWeek().getValue() <= 5) { // Monday=1 through Friday=5
                businessDays++;
            }
        }
        return date.toString();
    }
}
