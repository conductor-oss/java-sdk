package firstaiworkflow.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the OpenAI Chat Completions API.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set.
 *
 * Input: prompt, model, maxTokens, temperature
 * Output: rawResponse, tokenUsage
 */
public class AiCallLlmWorker implements Worker {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiCallLlmWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        System.out.println("  [ai_call_llm] CONDUCTOR_OPENAI_API_KEY detected");
    }

    /** Package-private constructor for testing with an explicit HTTP client. */
    AiCallLlmWorker(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "ai_call_llm";
    }

    @Override
    public TaskResult execute(Task task) {
        try {
            String prompt = (String) task.getInputData().get("prompt");
            String model = (String) task.getInputData().get("model");
            Object maxTokens = task.getInputData().get("maxTokens");
            Object temperature = task.getInputData().get("temperature");

            if (model == null || model.isBlank()) {
                model = "gpt-4";
            }
            if (maxTokens == null) {
                maxTokens = 1024;
            }
            if (temperature == null) {
                temperature = 0.7;
            }

            System.out.println("  [ai_call_llm worker] Calling " + model + " with prompt length "
                    + (prompt != null ? prompt.length() : 0));

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt != null ? prompt : "")));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error",
                        "OpenAI API returned HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> apiResponse = objectMapper.readValue(response.body(), Map.class);

            // Extract the response text from choices[0].message.content
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
            String rawResponse = "";
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    rawResponse = (String) message.getOrDefault("content", "");
                }
            }

            // Extract token usage
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) apiResponse.get("usage");
            Map<String, Object> tokenUsage = new LinkedHashMap<>();
            if (usage != null) {
                tokenUsage.put("promptTokens", usage.getOrDefault("prompt_tokens", 0));
                tokenUsage.put("completionTokens", usage.getOrDefault("completion_tokens", 0));
                tokenUsage.put("totalTokens", usage.getOrDefault("total_tokens", 0));
            }

            System.out.println("  [ai_call_llm worker] OpenAI API call completed successfully");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("rawResponse", rawResponse);
            result.getOutputData().put("tokenUsage", tokenUsage);
            return result;

        } catch (Exception e) {
            System.err.println("  [ai_call_llm worker] API call failed: " + e.getMessage());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "OpenAI API call failed: " + e.getMessage());
            return result;
        }
    }
}
