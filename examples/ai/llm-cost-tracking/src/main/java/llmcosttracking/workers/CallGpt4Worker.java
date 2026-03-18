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
 * Worker 1: Calls GPT-4 and returns token usage for cost tracking.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set.
 */
public class CallGpt4Worker implements Worker {

    private static final String API_KEY = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CallGpt4Worker() {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "ct_call_gpt4";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "Hello");

        TaskResult result = new TaskResult(task);

        try {
            return callLiveApi(task, prompt, result);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "GPT-4 API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult callLiveApi(Task task, String prompt, TaskResult result) throws Exception {
        String requestBody = MAPPER.writeValueAsString(Map.of(
                "model", "gpt-4",
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                },
                "max_tokens", 512
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
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

        // Extract response text
        String responseText = root.path("choices").path(0).path("message").path("content").asText("");

        // Extract token usage
        JsonNode usageNode = root.path("usage");
        int inputTokens = usageNode.path("prompt_tokens").asInt(0);
        int outputTokens = usageNode.path("completion_tokens").asInt(0);

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("model", "gpt-4");
        usage.put("inputTokens", inputTokens);
        usage.put("outputTokens", outputTokens);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("usage", usage);
        result.getOutputData().put("response", responseText);
        return result;
    }
}
