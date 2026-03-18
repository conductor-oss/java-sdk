package correctiverag.workers;

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
 * Generates an answer from web search results (the "irrelevant"/default branch).
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls OpenAI Chat Completions API (gpt-4o-mini).
 */
public class GenerateFromWebWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GenerateFromWebWorker() {
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
        return "cr_generate_from_web";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<?> context = (List<?>) task.getInputData().get("context");
        int count = context != null ? context.size() : 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        System.out.println("  [llm] Calling OpenAI to generate answer from " + count + " web results...");
        try {
            String question = (String) task.getInputData().get("question");
            String contextStr = context != null ? context.toString() : "";

            String systemPrompt = "You are a helpful assistant. Generate a comprehensive answer from the provided web search results.";
            String userPrompt = "Web search results: " + contextStr + "\n\nQuestion: " + (question != null ? question : "");

            String answer = callChatCompletion(systemPrompt, userPrompt, result);
            if (answer == null) {
                return result;
            }
            result.getOutputData().put("answer", answer);
            result.getOutputData().put("source", "webSearch");
            result.getOutputData().put("tokensUsed", answer.split("\\s+").length * 2);
        } catch (Exception e) {
            System.err.println("  [llm] OpenAI call failed: " + e.getMessage());
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
                "temperature", 0.3
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
