package stripeintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Creates a payment intent.
 * Input: customerId, amount, currency, description
 * Output: paymentIntentId, status
 *
 * Runs in live mode when STRIPE_API_KEY is set,
 * otherwise falls back to fallback mode.
 */
public class PaymentIntentWorker implements Worker {

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PaymentIntentWorker() {
        this.apiKey = System.getenv("STRIPE_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "stp_payment_intent";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        Object amount = task.getInputData().get("amount");
        String currency = (String) task.getInputData().get("currency");
        String description = (String) task.getInputData().get("description");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                StringBuilder body = new StringBuilder();
                body.append("amount=").append(URLEncoder.encode(String.valueOf(amount), StandardCharsets.UTF_8));
                body.append("&currency=").append(URLEncoder.encode(currency, StandardCharsets.UTF_8));
                body.append("&customer=").append(URLEncoder.encode(customerId, StandardCharsets.UTF_8));
                if (description != null) {
                    body.append("&description=").append(URLEncoder.encode(description, StandardCharsets.UTF_8));
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.stripe.com/v1/payment_intents"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String paymentIntentId = json.get("id").asText();
                    String status = json.get("status").asText();
                    result.getOutputData().put("paymentIntentId", paymentIntentId);
                    result.getOutputData().put("status", status);
                    System.out.println("  [intent] Created " + paymentIntentId + ": " + amount + " " + currency);
                } else {
                    String errorMsg = json.has("error") ? json.get("error").get("message").asText() : response.body();
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Stripe API error: " + errorMsg);
                    System.out.println("  [intent] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("Stripe API call failed: " + e.getMessage());
                System.out.println("  [intent] ERROR: " + e.getMessage());
            }
        } else {
            String paymentIntentId = "pi_" + Long.toString(System.currentTimeMillis(), 36);
            System.out.println("  [intent] Created " + paymentIntentId + ": " + amount + " " + currency);
            result.getOutputData().put("paymentIntentId", paymentIntentId);
            result.getOutputData().put("status", "requires_capture");
        }

        return result;
    }
}
