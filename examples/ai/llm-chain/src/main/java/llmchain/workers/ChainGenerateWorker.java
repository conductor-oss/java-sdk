package llmchain.workers;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Worker 2: LLM generation. Takes formattedPrompt, model, temperature, maxTokens.
 * Returns rawText JSON string and token usage stats.
 *
 * <p>Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI Chat Completions API.</p>
 */
public class ChainGenerateWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final HttpClient httpClient;

    public ChainGenerateWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.httpClient = HttpClient.newHttpClient();
    }

    /** Package-private constructor for testing with an explicit HTTP client. */
    ChainGenerateWorker(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    @Override
    public String getTaskDefName() {
        return "chain_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        String formattedPrompt = (String) task.getInputData().get("formattedPrompt");
        double temperature = 0.2;
        int maxTokens = 1024;

        TaskResult result = new TaskResult(task);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "temperature", temperature,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", formattedPrompt)
                    )
            );

            String body = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error",
                        "OpenAI API error " + response.statusCode() + ": " + response.body());
                return result;
            }

            JsonNode root = MAPPER.readTree(response.body());
            String rawText = root.at("/choices/0/message/content").asText();
            JsonNode usage = root.at("/usage");

            Map<String, Object> tokenUsage = Map.of(
                    "promptTokens", usage.get("prompt_tokens").asInt(),
                    "completionTokens", usage.get("completion_tokens").asInt(),
                    "totalTokens", usage.get("total_tokens").asInt()
            );

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("rawText", rawText);
            result.getOutputData().put("tokenUsage", tokenUsage);
            result.getOutputData().put("model", "gpt-4o-mini");
            result.getOutputData().put("temperature", temperature);
            result.getOutputData().put("maxTokens", maxTokens);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "OpenAI call failed: " + e.getMessage());
        }

        return result;
    }
}
