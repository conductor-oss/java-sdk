package stripeintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Charges a payment (confirms and captures a payment intent).
 * Input: paymentIntentId, customerId
 * Output: chargeId, status, capturedAt
 *
 * Runs in live mode when STRIPE_API_KEY is set,
 * otherwise falls back to fallback mode.
 */
public class ChargeWorker implements Worker {

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ChargeWorker() {
        this.apiKey = System.getenv("STRIPE_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "stp_charge";
    }

    @Override
    public TaskResult execute(Task task) {
        String paymentIntentId = (String) task.getInputData().get("paymentIntentId");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.stripe.com/v1/payment_intents/" + paymentIntentId + "/confirm"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String status = json.get("status").asText();
                    String chargeId = json.has("latest_charge") && !json.get("latest_charge").isNull()
                            ? json.get("latest_charge").asText()
                            : "ch_" + paymentIntentId.substring(3);
                    result.getOutputData().put("chargeId", chargeId);
                    result.getOutputData().put("status", status);
                    result.getOutputData().put("capturedAt", java.time.Instant.now().toString());
                    System.out.println("  [charge] Charged " + chargeId + " via intent " + paymentIntentId);
                } else {
                    String errorMsg = json.has("error") ? json.get("error").get("message").asText() : response.body();
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Stripe API error: " + errorMsg);
                    System.out.println("  [charge] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("Stripe API call failed: " + e.getMessage());
                System.out.println("  [charge] ERROR: " + e.getMessage());
            }
        } else {
            String chargeId = "ch_" + Long.toString(System.currentTimeMillis(), 36);
            System.out.println("  [charge] Charged " + chargeId + " via intent " + paymentIntentId);
            result.getOutputData().put("chargeId", chargeId);
            result.getOutputData().put("status", "succeeded");
            result.getOutputData().put("capturedAt", java.time.Instant.now().toString());
        }

        return result;
    }
}
