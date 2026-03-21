package paymentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Captures a previously authorized Stripe PaymentIntent.
 *
 * Takes an authorizationId (Stripe PaymentIntent ID) and captures the full amount.
 * In Stripe, capture finalizes the charge that was previously authorized with manual capture.
 *
 * Requires: STRIPE_API_KEY environment variable.
 */
public class CapturePaymentWorker implements Worker {

    private final String stripeApiKey;

    public CapturePaymentWorker() {
        this.stripeApiKey = System.getenv("STRIPE_API_KEY");
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            System.out.println("  [capture] STRIPE_API_KEY not set — running in mock mode.");
        }
    }

    @Override
    public String getTaskDefName() {
        return "pay_capture";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();

        String authorizationId = task.getInputData().get("authorizationId") != null
                ? task.getInputData().get("authorizationId").toString() : "";

        double amount = 0;
        Object amountObj = task.getInputData().get("amount");
        if (amountObj instanceof Number) amount = ((Number) amountObj).doubleValue();

        // Mock mode when Stripe key is not configured
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            String mockCaptureId = "CAPTURE-" + Math.abs(authorizationId.hashCode() % 9000 + 1000);
            System.out.println("  [capture] Authorization " + authorizationId
                    + ": captured $" + amount + " -> " + mockCaptureId + " (mock)");
            output.put("captureId", mockCaptureId);
            output.put("captured", true);
            output.put("stripeStatus", "succeeded");
            output.put("capturedAt", Instant.now().toString());
            output.put("amountCaptured", Math.round(amount * 100));
            output.put("demoMode", true);
            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        try {
            Stripe.apiKey = stripeApiKey;

            // Retrieve the PaymentIntent by its ID
            PaymentIntent intent = PaymentIntent.retrieve(authorizationId);

            String statusBefore = intent.getStatus();
            System.out.println("  [capture] PaymentIntent " + authorizationId
                    + " status before capture: " + statusBefore);

            // Capture the PaymentIntent (finalizes the charge)
            intent = intent.capture();

            System.out.println("  [capture] PaymentIntent " + authorizationId
                    + ": captured $" + amount + " (status: " + intent.getStatus() + ")");

            output.put("captureId", intent.getId());
            output.put("captured", true);
            output.put("stripeStatus", intent.getStatus());
            output.put("capturedAt", Instant.now().toString());
            output.put("amountCaptured", intent.getAmountReceived());
            result.setStatus(TaskResult.Status.COMPLETED);

        } catch (Exception e) {
            System.err.println("  [capture] Stripe error for " + authorizationId + ": " + e.getMessage());
            output.put("error", e.getMessage());
            output.put("captured", false);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Stripe capture failed: " + e.getMessage());
        }

        result.setOutputData(output);
        return result;
    }
}
