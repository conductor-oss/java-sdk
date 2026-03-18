package googlegemini.workers;

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
 * Calls the Google Gemini generateContent API.
 *
 * <p>Requires GOOGLE_API_KEY to be set. Makes a real HTTP call to the Gemini REST API
 * using java.net.http.HttpClient.</p>
 */
public class GeminiGenerateWorker implements Worker {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiGenerateWorker() {
        this.apiKey = System.getenv("GOOGLE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set GOOGLE_API_KEY environment variable to run this worker");
        }
    }

    /** Package-private constructor for testing. */
    GeminiGenerateWorker(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getTaskDefName() {
        return "gemini_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            return executeLive(task, result);
        } catch (Exception e) {
            System.err.println("  [gen] Gemini API error: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Gemini API call failed: " + e.getMessage());
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private TaskResult executeLive(Task task, TaskResult result) throws Exception {
        // Extract the prompt from the prepared request body
        Map<String, Object> requestBody = (Map<String, Object>) task.getInputData().get("requestBody");
        String model = resolveModel(task);
        String bodyJson = mapper.writeValueAsString(requestBody);

        System.out.println("  [gen] Calling Google Gemini API (model=" + model + ")...");
        System.out.println("  [gen] Request body: " + bodyJson.substring(0, Math.min(200, bodyJson.length())) + "...");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildGeminiUrl(model)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String retryAfter = response.headers().firstValue("retry-after").orElse(null);
            return handleHttpFailure(result, model, response.statusCode(), response.body(), retryAfter);
        }

        Map<String, Object> apiResponse = mapper.readValue(response.body(), Map.class);

        // Log token usage if available
        Map<String, Object> usage = (Map<String, Object>) apiResponse.get("usageMetadata");
        if (usage != null) {
            System.out.println("  [gen] Response: " + usage.get("totalTokenCount") + " tokens");
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("apiResponse", apiResponse);
        result.getOutputData().put("model", model);
        result.getOutputData().put("live", true);
        return result;
    }

    TaskResult handleHttpFailure(TaskResult result, String model, int statusCode, String responseBody, String retryAfter) {
        String body = responseBody == null || responseBody.isBlank() ? "<empty response body>" : responseBody;
        String summary = summarizeBody(body);

        if (statusCode == 429) {
            String retryMessage = retryAfter != null && !retryAfter.isBlank()
                    ? " Retry-After: " + retryAfter + " seconds."
                    : "";
            System.err.println("  [gen] Gemini quota exceeded for model " + model + "." + retryMessage);
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion(
                    "Gemini API quota exceeded (HTTP 429) for model " + model
                            + ". Verify billing/quota in Google AI Studio or Google Cloud."
                            + retryMessage);
        } else if (statusCode == 503 || statusCode >= 500) {
            System.err.println("  [gen] Gemini API transient error HTTP " + statusCode + ": " + summary);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Gemini API transient error HTTP " + statusCode + ": " + summary);
        } else {
            System.err.println("  [gen] Gemini API error HTTP " + statusCode + ": " + summary);
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Gemini API HTTP " + statusCode + ": " + summary);
        }

        result.getOutputData().put("errorBody", body);
        result.getOutputData().put("httpStatus", statusCode);
        result.getOutputData().put("model", model);
        result.getOutputData().put("live", true);
        if (retryAfter != null && !retryAfter.isBlank()) {
            result.getOutputData().put("retryAfterSeconds", retryAfter);
        }
        return result;
    }

    private String resolveModel(Task task) {
        Object configured = task.getInputData().get("model");
        if (configured != null && !configured.toString().isBlank()) {
            return configured.toString();
        }
        return configuredDefaultModel();
    }

    public static String configuredDefaultModel() {
        String configured = System.getenv("GEMINI_MODEL");
        return configured != null && !configured.isBlank() ? configured : DEFAULT_MODEL;
    }

    private String buildGeminiUrl(String model) {
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
    }

    private static String summarizeBody(String body) {
        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 217) + "...";
    }
}
