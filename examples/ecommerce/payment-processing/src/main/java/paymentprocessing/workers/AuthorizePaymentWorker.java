package paymentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a Stripe PaymentIntent with manual capture (authorize only, capture later).
 *
 * Uses the real Stripe Java SDK to create a PaymentIntent in test mode.
 * The returned authorizationId is a real Stripe PaymentIntent ID (pi_xxx).
 *
 * Requires: STRIPE_API_KEY environment variable (must be a sk_test_ key for test mode).
 */
public class AuthorizePaymentWorker implements Worker {

    private final String stripeApiKey;

    public AuthorizePaymentWorker() {
        this.stripeApiKey = System.getenv("STRIPE_API_KEY");
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            System.out.println("  [authorize] STRIPE_API_KEY not set — running in mock mode.");
        }
    }

    @Override
    public String getTaskDefName() {
        return "pay_authorize";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        double amount = 0;
        Object amountObj = task.getInputData().get("amount");
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

        String orderId = task.getInputData().get("orderId") != null
                ? task.getInputData().get("orderId").toString() : "UNKNOWN";

        // Determine currency from input or default to usd
        String currency = "usd";
        Object currObj = task.getInputData().get("currency");
        if (currObj != null && !currObj.toString().isBlank()) {
            currency = currObj.toString().toLowerCase();
        }

        // Mock mode when Stripe key is not configured
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            String mockAuthId = "AUTHORIZATION-" + Math.abs(orderId.hashCode() % 9000 + 1000);
            System.out.println("  [authorize] Order " + orderId + ": authorized " + mockAuthId
                    + " for $" + amount + " " + currency + " (mock)");
            output.put("authorizationId", mockAuthId);
            output.put("authorized", true);
            output.put("stripeStatus", "requires_capture");
            output.put("amountInCents", Math.round(amount * 100));
            output.put("currency", currency);
            output.put("expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString());
            output.put("demoMode", true);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        try {
            Stripe.apiKey = stripeApiKey;

            // Convert to smallest currency unit (cents for USD)
            long amountInCents = Math.round(amount * 100);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .addPaymentMethodType("card")
                    .putMetadata("orderId", orderId)
                    .setDescription("Payment for order " + orderId)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            System.out.println("  [authorize] Order " + orderId
                    + ": created PaymentIntent " + intent.getId() + " for $" + amount + " " + currency
                    + " (status: " + intent.getStatus() + ")");

            output.put("authorizationId", intent.getId());
            output.put("authorized", true);
            output.put("stripeStatus", intent.getStatus());
            output.put("clientSecret", intent.getClientSecret());
            output.put("amountInCents", amountInCents);
            output.put("currency", currency);
            output.put("expiresAt", Instant.now().plus(7, ChronoUnit.DAYS).toString());
            result.setStatus(TaskResult.Status.COMPLETED);

        } catch (Exception e) {
            System.err.println("  [authorize] Stripe error for order " + orderId + ": " + e.getMessage());
            output.put("error", e.getMessage());
            output.put("authorized", false);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Stripe authorization failed: " + e.getMessage());
        }

        result.setOutputData(output);
        return result;
    }
}
