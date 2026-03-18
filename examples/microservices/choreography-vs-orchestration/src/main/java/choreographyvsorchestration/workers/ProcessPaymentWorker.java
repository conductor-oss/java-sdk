package choreographyvsorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessPaymentWorker implements Worker {

    // Track processed idempotency keys to prevent double-charges
    private static final Set<String> PROCESSED_PAYMENTS = ConcurrentHashMap.newKeySet();

    /** Expose processed set for testing. */
    public static Set<String> getProcessedPayments() {
        return PROCESSED_PAYMENTS;
    }

    @Override
    public String getTaskDefName() {
        return "cvo_process_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String orderId = (String) task.getInputData().getOrDefault("orderId", null);
        Object amountObj = task.getInputData().get("amount");
        String customerId = (String) task.getInputData().getOrDefault("customerId", "unknown");

        // Validate orderId
        if (orderId == null || orderId.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: orderId");
            return r;
        }

        // Validate and parse amount
        if (amountObj == null) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Missing required field: amount");
            return r;
        }

        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            try {
                amount = Double.parseDouble(String.valueOf(amountObj));
            } catch (NumberFormatException e) {
                r.setStatus(TaskResult.Status.FAILED);
                r.setReasonForIncompletion("Invalid amount: " + amountObj);
                return r;
            }
        }

        if (amount <= 0) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Amount must be positive, got: " + amount);
            return r;
        }

        if (amount > 50000) {
            r.setStatus(TaskResult.Status.FAILED);
            r.setReasonForIncompletion("Amount exceeds maximum allowed ($50,000): $" + amount);
            return r;
        }

        // Compute idempotency hash from orderId + amount
        String idempotencyKey = computeIdempotencyKey(orderId, amount);

        // Check for duplicate payment
        if (!PROCESSED_PAYMENTS.add(idempotencyKey)) {
            System.out.println("  [payment] Duplicate payment detected for order " + orderId + " — returning previous result");
            r.setStatus(TaskResult.Status.COMPLETED);
            r.getOutputData().put("charged", true);
            r.getOutputData().put("duplicate", true);
            r.getOutputData().put("orderId", orderId);
            r.getOutputData().put("amount", amount);
            r.getOutputData().put("idempotencyKey", idempotencyKey);
            r.getOutputData().put("transactionId", "TXN-" + idempotencyKey.substring(0, 12).toUpperCase());
            return r;
        }

        String transactionId = "TXN-" + idempotencyKey.substring(0, 12).toUpperCase();

        System.out.println("  [payment] Charged $" + String.format("%.2f", amount)
                + " for order " + orderId + " (txn=" + transactionId + ")");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("charged", true);
        r.getOutputData().put("duplicate", false);
        r.getOutputData().put("orderId", orderId);
        r.getOutputData().put("amount", amount);
        r.getOutputData().put("customerId", customerId);
        r.getOutputData().put("transactionId", transactionId);
        r.getOutputData().put("idempotencyKey", idempotencyKey);
        r.getOutputData().put("processedAt", Instant.now().toString());
        return r;
    }

    private static String computeIdempotencyKey(String orderId, double amount) {
        try {
            String payload = orderId + "|" + String.format("%.2f", amount);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            // Fallback: simple string concatenation
            return orderId + "-" + String.format("%.2f", amount);
        }
    }
}
