package llmcosttracking.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Worker 3: Calls Gemini and returns token usage for cost tracking.
 *
 * Requires GOOGLE_API_KEY to be set.
 */
public class CallGeminiWorker implements Worker {

    private static final String API_KEY = System.getenv("GOOGLE_API_KEY");
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CallGeminiWorker() {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "Set GOOGLE_API_KEY environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "ct_call_gemini";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "Hello");

        TaskResult result = new TaskResult(task);

        try {
            return callLiveApi(task, prompt, result);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Gemini API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult callLiveApi(Task task, String prompt, TaskResult result) throws Exception {
        String requestBody = MAPPER.writeValueAsString(Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "maxOutputTokens", 512
                )
        ));

        String apiUrl = String.format(API_URL_TEMPLATE, API_KEY);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("  [worker] API error HTTP " + response.statusCode() + ": " + errorBody);
            // 429 (rate limit) and 503 (overloaded) are retryable
            if (response.statusCode() == 429 || response.statusCode() == 503) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("API rate limited (HTTP " + response.statusCode() + "). Conductor will retry.");
            } else {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("API error HTTP " + response.statusCode() + ": " + errorBody);
            }
            result.getOutputData().put("errorBody", errorBody);
            result.getOutputData().put("httpStatus", response.statusCode());
            return result;
        }

        JsonNode root = MAPPER.readTree(response.body());

        // Extract response text from candidates
        String responseText = root
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");

        // Extract token usage from usageMetadata
        JsonNode usageMeta = root.path("usageMetadata");
        int inputTokens = usageMeta.path("promptTokenCount").asInt(0);
        int outputTokens = usageMeta.path("candidatesTokenCount").asInt(0);

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("model", "gemini");
        usage.put("inputTokens", inputTokens);
        usage.put("outputTokens", outputTokens);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("usage", usage);
        result.getOutputData().put("response", responseText);
        return result;
    }
}
