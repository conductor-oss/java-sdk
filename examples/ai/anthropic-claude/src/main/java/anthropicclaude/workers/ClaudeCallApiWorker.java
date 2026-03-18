package anthropicclaude.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Claude API.
 *
 * <p>Requires CONDUCTOR_ANTHROPIC_API_KEY to be set. Makes a real HTTP call to the
 * Claude Messages API using java.net.http.HttpClient (built into Java 21).</p>
 */
public class ClaudeCallApiWorker implements Worker {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;

    public ClaudeCallApiWorker() {
        this.apiKey = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_ANTHROPIC_API_KEY environment variable to run this worker");
        }
    }

    /**
     * Package-private constructor for testing — allows injecting an API key.
     */
    ClaudeCallApiWorker(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getTaskDefName() {
        return "claude_call_api";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            Map<String, Object> requestBody = (Map<String, Object>) task.getInputData().get("requestBody");
            String requestJson = MAPPER.writeValueAsString(requestBody);
            String model = resolveRequestedModel(requestBody);

            System.out.println("  [api] Calling Anthropic Claude API (model=" + model + ")...");
            System.out.println("  [api] Request body: " + requestJson.substring(0, Math.min(200, requestJson.length())) + "...");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLAUDE_API_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return handleHttpFailure(result, response.statusCode(), response.body());
            }

            Map<String, Object> apiResponse = MAPPER.readValue(response.body(), Map.class);

            // Extract usage for logging
            Map<String, Object> usage = (Map<String, Object>) apiResponse.get("usage");
            if (usage != null) {
                System.out.println("  [api] Response from Anthropic API: "
                        + usage.get("input_tokens") + " input + "
                        + usage.get("output_tokens") + " output tokens");
            } else {
                System.out.println("  [api] Response from Anthropic API");
            }

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("apiResponse", apiResponse);
            return result;

        } catch (Exception e) {
            System.out.println("  [api] API call failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Claude API call failed: " + e.getMessage());
            return result;
        }
    }

    TaskResult handleHttpFailure(TaskResult result, int statusCode, String responseBody) {
        String body = responseBody == null || responseBody.isBlank() ? "<empty response body>" : responseBody;
        String summary = summarizeBody(body);

        System.err.println("  [api] API error HTTP " + statusCode + ": " + summary);

        if (statusCode >= 400 && statusCode < 500 && statusCode != 429) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
        } else {
            result.setStatus(TaskResult.Status.FAILED);
        }
        result.setReasonForIncompletion("Claude API HTTP " + statusCode + ": " + summary);
        result.getOutputData().put("errorBody", body);
        result.getOutputData().put("httpStatus", statusCode);
        return result;
    }

    private String resolveRequestedModel(Map<String, Object> requestBody) {
        if (requestBody == null) {
            return configuredDefaultModel();
        }

        Object model = requestBody.get("model");
        if (model == null || model.toString().isBlank()) {
            return configuredDefaultModel();
        }
        return model.toString();
    }

    public static String configuredDefaultModel() {
        String configured = System.getenv("ANTHROPIC_MODEL");
        return configured != null && !configured.isBlank() ? configured : DEFAULT_MODEL;
    }

    private static String summarizeBody(String body) {
        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 217) + "...";
    }
}
