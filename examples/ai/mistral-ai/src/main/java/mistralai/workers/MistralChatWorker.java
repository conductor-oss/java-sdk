package mistralai.workers;

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
 * Calls the Mistral Chat Completions API, or returns a default response when
 * MISTRAL_API_KEY is not set.
 *
 * Live mode: POST https://api.mistral.ai/v1/chat/completions
 * Fallback mode: returns a fixed, deterministic response.
 *
 * Input: requestBody (the composed request)
 * Output: chatResponse (Mistral API response)
 */
public class MistralChatWorker implements Worker {

    private static final String MISTRAL_API_URL = "https://api.mistral.ai/v1/chat/completions";

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MistralChatWorker() {
        this.apiKey = System.getenv("MISTRAL_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = liveMode ? HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build() : null;
        this.objectMapper = new ObjectMapper();

        if (liveMode) {
            System.out.println("  [mistral_chat] Live mode: MISTRAL_API_KEY detected");
        } else {
            System.out.println("  [mistral_chat] Fallback mode: set MISTRAL_API_KEY for live API calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "mistral_chat";
    }

    @Override
    public TaskResult execute(Task task) {
        if (liveMode) {
            return executeLive(task);
        }
        return executeFallback(task);
    }

    private TaskResult executeLive(Task task) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) task.getInputData().get("requestBody");
            if (requestBody == null) {
                requestBody = new LinkedHashMap<>();
                requestBody.put("model", "mistral-large-latest");
                requestBody.put("messages", List.of(
                        Map.of("role", "user", "content", "Hello")));
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            System.out.println("  [mistral_chat] Calling Mistral API (live)...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MISTRAL_API_URL))
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
                        "Mistral API returned HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> chatResponse = objectMapper.readValue(response.body(), Map.class);

            System.out.println("  [mistral_chat] Mistral API call completed successfully");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("chatResponse", chatResponse);
            return result;

        } catch (Exception e) {
            System.err.println("  [mistral_chat] API call failed: " + e.getMessage());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Mistral API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeFallback(Task task) {
        Map<String, Object> assistantMessage = new LinkedHashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content",
                "Key contract analysis:\n\n"
                + "**Obligations:**\n"
                + "1. Licensee must use software only for intended purposes\n"
                + "2. Licensee must maintain confidentiality of proprietary information\n"
                + "3. Licensor must provide software updates for the duration of the agreement\n\n"
                + "**Key Terms:**\n"
                + "- License is non-transferable and non-exclusive\n"
                + "- Agreement is valid for 12 months with auto-renewal\n"
                + "- Either party may terminate with 30 days written notice");

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", assistantMessage);
        choice.put("finish_reason", "stop");

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", 156);
        usage.put("completion_tokens", 134);
        usage.put("total_tokens", 290);

        Map<String, Object> chatResponse = new LinkedHashMap<>();
        chatResponse.put("id", "cmpl-mst-abc123xyz");
        chatResponse.put("object", "chat.completion");
        chatResponse.put("model", "mistral-large-latest");
        chatResponse.put("choices", List.of(choice));
        chatResponse.put("usage", usage);

        System.out.println("  [mistral_chat] deterministic Mistral API call completed.");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chatResponse", chatResponse);
        return result;
    }
}
