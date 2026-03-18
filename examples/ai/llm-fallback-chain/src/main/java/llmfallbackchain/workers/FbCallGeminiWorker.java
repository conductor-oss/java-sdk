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
 * Calls Gemini via the Google Generative Language API.
 *
 * <p>Requires GOOGLE_API_KEY to be set. Makes a real API call to
 * POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent.
 * Returns status "success" with the model's response on HTTP 200, or
 * status "failed" with the error on any non-200 response or exception.</p>
 */
public class FbCallGeminiWorker implements Worker {

    private static final String API_KEY_ENV = "GOOGLE_API_KEY";
    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";
    private static final String MODEL = "gemini-2.0-flash";

    private final ObjectMapper mapper = new ObjectMapper();

    public FbCallGeminiWorker() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set " + API_KEY_ENV + " environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "fb_call_gemini";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String apiKey = System.getenv(API_KEY_ENV);

        return callLive(task, prompt, apiKey);
    }

    private TaskResult callLive(Task task, String prompt, String apiKey) {
        System.out.println("  [fb_call_gemini] Calling Google Gemini API with prompt: " + prompt);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            );
            String requestBody = mapper.writeValueAsString(body);

            String url = String.format(API_URL_TEMPLATE, apiKey);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = mapper.readTree(response.body());
                String content = json.at("/candidates/0/content/parts/0/text").asText("");
                System.out.println("  [fb_call_gemini] Gemini returned success");
                result.getOutputData().put("status", "success");
                result.getOutputData().put("response", content);
                result.getOutputData().put("model", MODEL);
            } else {
                String error = response.statusCode() + " " + response.body();
                System.out.println("  [fb_call_gemini] Gemini returned error: " + error);
                result.getOutputData().put("status", "failed");
                result.getOutputData().put("response", null);
                result.getOutputData().put("error", error);
            }
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.println("  [fb_call_gemini] Gemini call failed: " + error);
            result.getOutputData().put("status", "failed");
            result.getOutputData().put("response", null);
            result.getOutputData().put("error", error);
        }

        return result;
    }
}
