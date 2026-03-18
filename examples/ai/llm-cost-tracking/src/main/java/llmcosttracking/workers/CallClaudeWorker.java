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
 * Worker 2: Calls Claude and returns token usage for cost tracking.
 *
 * Requires CONDUCTOR_ANTHROPIC_API_KEY to be set.
 */
public class CallClaudeWorker implements Worker {

    private static final String API_KEY = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CallClaudeWorker() {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_ANTHROPIC_API_KEY environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "ct_call_claude";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "Hello");

        TaskResult result = new TaskResult(task);

        try {
            return callLiveApi(task, prompt, result);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Claude API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult callLiveApi(Task task, String prompt, TaskResult result) throws Exception {
        String requestBody = MAPPER.writeValueAsString(Map.of(
                "model", "claude-sonnet-4-6",
                "max_tokens", 512,
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                }
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .header("anthropic-version", "2023-06-01")
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

        // Extract response text from content array
        StringBuilder responseText = new StringBuilder();
        JsonNode contentArray = root.path("content");
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    responseText.append(block.path("text").asText(""));
                }
            }
        }

        // Extract token usage
        JsonNode usageNode = root.path("usage");
        int inputTokens = usageNode.path("input_tokens").asInt(0);
        int outputTokens = usageNode.path("output_tokens").asInt(0);

        // Map to the model name used in pricing
        String model = root.path("model").asText("claude-3");
        String pricingModel = model.startsWith("claude-3") ? "claude-3" : model;

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("model", pricingModel);
        usage.put("inputTokens", inputTokens);
        usage.put("outputTokens", outputTokens);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("usage", usage);
        result.getOutputData().put("response", responseText.toString());
        return result;
    }
}
