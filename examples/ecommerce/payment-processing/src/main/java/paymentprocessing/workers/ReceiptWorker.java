package paymentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates a payment receipt after successful capture.
 *
 * Retrieves the real PaymentIntent from Stripe to include actual charge details
 * in the receipt. The receipt ID is a SHA-256 hash of the payment details for
 * deterministic, verifiable receipts.
 *
 * Requires: STRIPE_API_KEY environment variable.
 */
public class ReceiptWorker implements Worker {

    private final String stripeApiKey;

    public ReceiptWorker() {
        this.stripeApiKey = System.getenv("STRIPE_API_KEY");
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            System.out.println("  [receipt] STRIPE_API_KEY not set — running in mock mode.");
        }
    }

    @Override
    public String getTaskDefName() {
        return "pay_receipt";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String orderId = task.getInputData().get("orderId") != null
                ? task.getInputData().get("orderId").toString() : "UNKNOWN";
        String captureId = task.getInputData().get("captureId") != null
                ? task.getInputData().get("captureId").toString() : "";
        Object amountObj = task.getInputData().get("amount");
        double amount = 0;
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();
        String currency = task.getInputData().get("currency") != null
                ? task.getInputData().get("currency").toString().toUpperCase() : "USD";

        Instant now = Instant.now();

        // Mock mode when Stripe key is not configured
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            String receiptId = generateReceiptId(orderId, captureId, now);
            System.out.println("  [receipt] Generated " + receiptId + " for order "
                    + orderId + ": $" + amount + " " + currency + " (mock)");
            output.put("receiptId", receiptId);
            output.put("orderId", orderId);
            output.put("captureId", captureId);
            output.put("amount", amount);
            output.put("currency", currency);
            output.put("generatedAt", now.toString());
            output.put("demoMode", true);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        try {
            Stripe.apiKey = stripeApiKey;

            // Retrieve real payment details from Stripe
            PaymentIntent intent = PaymentIntent.retrieve(captureId);

            String receiptId = generateReceiptId(orderId, captureId, now);

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss z")
                    .withZone(ZoneId.of("UTC"));

            output.put("receiptId", receiptId);
            output.put("orderId", orderId);
            output.put("paymentIntentId", intent.getId());
            output.put("amount", amount);
            output.put("amountCaptured", intent.getAmountReceived() / 100.0);
            output.put("currency", currency);
            output.put("stripeStatus", intent.getStatus());
            output.put("generatedAt", formatter.format(now));
            output.put("receiptTimestamp", now.toString());

            System.out.println("  [receipt] Generated receipt " + receiptId + " for order "
                    + orderId + ": $" + amount + " " + currency + " (Stripe: " + intent.getStatus() + ")");

            result.setStatus(TaskResult.Status.COMPLETED);

        } catch (Exception e) {
            // If Stripe lookup fails, still generate a receipt from task data
            String receiptId = generateReceiptId(orderId, captureId, now);

            output.put("receiptId", receiptId);
            output.put("orderId", orderId);
            output.put("captureId", captureId);
            output.put("amount", amount);
            output.put("currency", currency);
            output.put("generatedAt", now.toString());
            output.put("stripeError", e.getMessage());

            System.out.println("  [receipt] Generated receipt " + receiptId + " for order "
                    + orderId + " (Stripe lookup failed: " + e.getMessage() + ")");

            result.setStatus(TaskResult.Status.COMPLETED);
        }

        result.setOutputData(output);
        return result;
    }

    /**
     * Generates a deterministic receipt ID from order, capture, and timestamp.
     * Uses SHA-256 to produce a verifiable, unique receipt identifier.
     */
    private String generateReceiptId(String orderId, String captureId, Instant timestamp) {
        String input = orderId + "|" + captureId + "|" + timestamp.getEpochSecond();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("RCPT-");
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in all JVMs
            throw new RuntimeException(e);
        }
    }
}
