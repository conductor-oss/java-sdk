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
 * Calls GPT-4 via the OpenAI Chat Completions API.
 *
 * <p>Requires CONDUCTOR_OPENAI_API_KEY to be set. Makes a real API call to
 * POST https://api.openai.com/v1/chat/completions. Returns status "success"
 * with the model's response on HTTP 200, or status "failed" with the error
 * on any non-200 response or exception.</p>
 */
public class FbCallGpt4Worker implements Worker {

    private static final String API_KEY_ENV = "CONDUCTOR_OPENAI_API_KEY";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4";

    private final ObjectMapper mapper = new ObjectMapper();

    public FbCallGpt4Worker() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set " + API_KEY_ENV + " environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "fb_call_gpt4";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String apiKey = System.getenv(API_KEY_ENV);

        return callLive(task, prompt, apiKey);
    }

    private TaskResult callLive(Task task, String prompt, String apiKey) {
        System.out.println("  [fb_call_gpt4] Calling OpenAI GPT-4 API with prompt: " + prompt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 512
            );
            String requestBody = mapper.writeValueAsString(body);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                String content = json.at("/choices/0/message/content").asText("");
                String modelUsed = json.path("model").asText(MODEL);
                System.out.println("  [fb_call_gpt4] GPT-4 returned success");
                result.getOutputData().put("status", "success");
                result.getOutputData().put("response", content);
                result.getOutputData().put("model", modelUsed);
            } else {
                String error = response.statusCode() + " " + response.body();
                System.out.println("  [fb_call_gpt4] GPT-4 returned error: " + error);
                result.getOutputData().put("status", "failed");
                result.getOutputData().put("response", null);
                result.getOutputData().put("error", error);
            }
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.println("  [fb_call_gpt4] GPT-4 call failed: " + error);
            result.getOutputData().put("status", "failed");
            result.getOutputData().put("response", null);
            result.getOutputData().put("error", error);
        }

        return result;
    }
}
