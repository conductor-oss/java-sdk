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
 * Creates a Stripe customer.
 * Input: email
 * Output: customerId
 *
 * Runs in live mode when STRIPE_API_KEY is set,
 * otherwise falls back to fallback mode.
 */
public class CreateCustomerWorker implements Worker {

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CreateCustomerWorker() {
        this.apiKey = System.getenv("STRIPE_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "stp_create_customer";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (liveMode) {
            try {
                String body = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.stripe.com/v1/customers"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String customerId = json.get("id").asText();
                    result.getOutputData().put("customerId", customerId);
                    System.out.println("  [customer] Created " + customerId + " for " + email);
                } else {
                    String errorMsg = json.has("error") ? json.get("error").get("message").asText() : response.body();
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("Stripe API error: " + errorMsg);
                    System.out.println("  [customer] ERROR: " + errorMsg);
                }
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("Stripe API call failed: " + e.getMessage());
                System.out.println("  [customer] ERROR: " + e.getMessage());
            }
        } else {
            String customerId = "cus_" + Long.toString(System.currentTimeMillis(), 36);
            System.out.println("  [customer] Created " + customerId + " for " + email);
            result.getOutputData().put("customerId", customerId);
        }

        return result;
    }
}
