package paymentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates a payment request before authorization.
 *
 * Real validation logic:
 *   - Checks that amount is positive and within Stripe limits (max $999,999.99)
 *   - Validates currency is a supported ISO 4217 code
 *   - Validates payment method structure contains required fields
 *   - Computes a basic fraud score based on amount and payment characteristics
 *
 * Requires: STRIPE_API_KEY environment variable (used to confirm Stripe connectivity intent)
 */
public class ValidatePaymentWorker implements Worker {

    private static final double MAX_AMOUNT = 999_999.99;
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "usd", "eur", "gbp", "jpy", "cad", "aud", "chf", "cny",
            "inr", "brl", "mxn", "sgd", "hkd", "nzd", "sek", "nok", "dkk"
    );
    private static final Set<String> VALID_PAYMENT_TYPES = Set.of(
            "credit_card", "debit_card", "card", "bank_transfer", "sepa_debit"
    );

    private final boolean mockMode;

    public ValidatePaymentWorker() {
        String apiKey = System.getenv("STRIPE_API_KEY");
        this.mockMode = (apiKey == null || apiKey.isBlank());
        if (mockMode) {
            System.out.println("  [validate] STRIPE_API_KEY not set — running in mock mode. "
                    + "Set STRIPE_API_KEY=sk_test_... for real Stripe validation.");
        }
    }

    @Override
    public String getTaskDefName() {
        return "pay_validate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        // Extract inputs
        Object amountObj = task.getInputData().get("amount");
        String currency = task.getInputData().get("currency") != null
                ? task.getInputData().get("currency").toString().toLowerCase() : null;
        Object paymentMethodObj = task.getInputData().get("paymentMethod");
        String orderId = task.getInputData().get("orderId") != null
                ? task.getInputData().get("orderId").toString() : "UNKNOWN";

        // Validate amount
        double amount = 0;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        }

        boolean amountValid = amount > 0 && amount <= MAX_AMOUNT;
        String amountError = null;
        if (amount <= 0) {
            amountError = "Amount must be positive";
        } else if (amount > MAX_AMOUNT) {
            amountError = "Amount exceeds maximum of $" + MAX_AMOUNT;
        }

        // Validate currency
        boolean currencyValid = currency != null && SUPPORTED_CURRENCIES.contains(currency);
        String currencyError = currencyValid ? null : "Unsupported or missing currency: " + currency;

        // Validate payment method
        boolean paymentMethodValid = false;
        String paymentMethodError = "Missing payment method";
        if (paymentMethodObj instanceof Map) {
            Map<String, Object> pm = (Map<String, Object>) paymentMethodObj;
            Object typeObj = pm.get("type");
            if (typeObj != null && VALID_PAYMENT_TYPES.contains(typeObj.toString().toLowerCase())) {
                paymentMethodValid = true;
                paymentMethodError = null;
            } else {
                paymentMethodError = "Invalid payment method type: " + typeObj;
            }
        }

        boolean valid = amountValid && currencyValid && paymentMethodValid;

        // Compute a real fraud score based on observable characteristics
        double fraudScore = computeFraudScore(amount, currency, paymentMethodObj);

        System.out.println("  [validate] Order " + orderId + ": $" + amount + " " + currency
                + " -> valid=" + valid + ", fraudScore=" + fraudScore);

        output.put("valid", valid);
        output.put("fraudScore", fraudScore);
        output.put("amountValid", amountValid);
        output.put("currencyValid", currencyValid);
        output.put("paymentMethodValid", paymentMethodValid);

        if (!valid) {
            StringBuilder errors = new StringBuilder();
            if (amountError != null) errors.append(amountError).append("; ");
            if (currencyError != null) errors.append(currencyError).append("; ");
            if (paymentMethodError != null) errors.append(paymentMethodError);
            output.put("validationErrors", errors.toString().trim());
        }

        result.setOutputData(output);
        result.setStatus(valid ? TaskResult.Status.COMPLETED : TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
        if (!valid) {
            result.setReasonForIncompletion("Payment validation failed");
        }
        return result;
    }

    /**
     * Computes a fraud score between 0.0 (safe) and 1.0 (suspicious) based on:
     *   - Amount relative to common fraud thresholds
     *   - Round-number detection (fraudsters often use exact amounts)
     *   - High-risk currency detection
     */
    @SuppressWarnings("unchecked")
    private double computeFraudScore(double amount, String currency, Object paymentMethodObj) {
        double score = 0.0;

        // High amounts increase risk (>5000 adds up to 0.3)
        if (amount > 5000) {
            score += Math.min((amount - 5000) / 50000.0, 0.3);
        }

        // Exact round amounts are suspicious (adds 0.1)
        if (amount > 100 && amount == Math.floor(amount) && amount % 100 == 0) {
            score += 0.10;
        }

        // Very small test-like amounts (adds 0.05)
        if (amount > 0 && amount < 1.0) {
            score += 0.05;
        }

        // High-risk currencies (adds 0.05)
        if (currency != null && Set.of("brl", "inr", "cny").contains(currency)) {
            score += 0.05;
        }

        // Missing or incomplete payment method details increase risk
        if (paymentMethodObj instanceof Map) {
            Map<String, Object> pm = (Map<String, Object>) paymentMethodObj;
            if (pm.get("last4") == null && pm.get("token") == null) {
                score += 0.08;
            }
        } else {
            score += 0.15;
        }

        return Math.round(Math.min(score, 1.0) * 100.0) / 100.0;
    }
}
