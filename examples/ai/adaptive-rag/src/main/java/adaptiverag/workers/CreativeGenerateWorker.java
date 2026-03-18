package adaptiverag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Creative generation worker that produces free-form answers without retrieval.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls OpenAI Chat Completions API (gpt-4o-mini).
 */
public class CreativeGenerateWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CreativeGenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "ar_creative_gen";
    }

    @Override
    public TaskResult execute(Task task) {
        String style = (String) task.getInputData().get("style");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        System.out.println("  [creative-gen] Calling OpenAI for free-form generation (style: " + style + ")");
        try {
            String question = (String) task.getInputData().get("question");
            String systemPrompt = "You are a creative writer. Produce an imaginative, engaging response in the requested style.";
            String userPrompt = "Style: " + (style != null ? style : "creative") + "\n\nPrompt: " + (question != null ? question : "");

            String answer = callChatCompletion(systemPrompt, userPrompt, result);
            if (answer == null) {
                return result;
            }
            result.getOutputData().put("answer", answer);
            result.getOutputData().put("strategy", "creative");
        } catch (Exception e) {
            System.err.println("  [creative-gen] OpenAI call failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
        }

        return result;
    }

    private String callChatCompletion(String systemPrompt, String userPrompt, TaskResult result) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 512,
                "temperature", 0.9
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("  [worker] API error HTTP " + response.statusCode() + ": " + errorBody);
            if (response.statusCode() == 429 || response.statusCode() == 503) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("API rate limited (HTTP " + response.statusCode() + "). Conductor will retry.");
            } else {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("API error HTTP " + response.statusCode() + ": " + errorBody);
            }
            result.getOutputData().put("errorBody", errorBody);
            result.getOutputData().put("httpStatus", response.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }
}
