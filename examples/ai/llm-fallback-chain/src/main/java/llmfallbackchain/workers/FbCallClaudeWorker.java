package llmfallbackchain.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls Claude via the Anthropic Messages API.
 *
 * <p>Requires CONDUCTOR_ANTHROPIC_API_KEY to be set. Makes a real API call to
 * POST https://api.anthropic.com/v1/messages. Returns status "success"
 * with the model's response on HTTP 200, or status "failed" with the error
 * on any non-200 response or exception.</p>
 */
public class FbCallClaudeWorker implements Worker {

    private static final String API_KEY_ENV = "CONDUCTOR_ANTHROPIC_API_KEY";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ObjectMapper mapper = new ObjectMapper();

    public FbCallClaudeWorker() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set " + API_KEY_ENV + " environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "fb_call_claude";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String apiKey = System.getenv(API_KEY_ENV);

        return callLive(task, prompt, apiKey);
    }

    private TaskResult callLive(Task task, String prompt, String apiKey) {
        System.out.println("  [fb_call_claude] Calling Anthropic Claude API with prompt: " + prompt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "max_tokens", 512,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String requestBody = mapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                String content = json.at("/content/0/text").asText("");
                String modelUsed = json.path("model").asText(MODEL);
                System.out.println("  [fb_call_claude] Claude returned success");
                result.getOutputData().put("status", "success");
                result.getOutputData().put("response", content);
                result.getOutputData().put("model", modelUsed);
            } else {
                String error = response.statusCode() + " " + response.body();
                System.out.println("  [fb_call_claude] Claude returned error: " + error);
                result.getOutputData().put("status", "failed");
                result.getOutputData().put("response", null);
                result.getOutputData().put("error", error);
            }
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.println("  [fb_call_claude] Claude call failed: " + error);
            result.getOutputData().put("status", "failed");
            result.getOutputData().put("response", null);
            result.getOutputData().put("error", error);
        }

        return result;
    }
}
