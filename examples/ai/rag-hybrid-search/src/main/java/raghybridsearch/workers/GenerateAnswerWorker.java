package raghybridsearch.workers;

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
 * Answer generation worker.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI chat completions API (gpt-4o-mini).
 */
public class GenerateAnswerWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GenerateAnswerWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    /** Package-private constructor for testing with an explicit API key. */
    GenerateAnswerWorker(String apiKey, HttpClient httpClient) {
        this.openaiApiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public String getTaskDefName() {
        return "hs_generate_answer";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> context = (List<String>) task.getInputData().get("context");
        int contextSize = (context != null) ? context.size() : 0;

        TaskResult result = new TaskResult(task);

        try {
            String contextText = (context != null) ? String.join("\n", context) : "";
            String question = (String) task.getInputData().get("question");
            if (question == null) question = "Answer based on the context provided.";
            String answer = callOpenAIChat(question, contextText, result);
            if (answer == null) {
                return result;
            }
            System.out.println("  [llm] Answer (OpenAI) from " + contextSize + " docs");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("answer", answer);
            result.getOutputData().put("tokensUsed", answer.split("\\s+").length);
        } catch (Exception e) {
            System.err.println("  [llm] OpenAI API error: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String callOpenAIChat(String question, String contextText, TaskResult result) throws Exception {
        String systemPrompt = "You are a helpful assistant. Answer the user's question based on the provided context "
                + "from hybrid search results (vector + keyword search merged via RRF).";
        String userPrompt = "Context:\n" + contextText + "\n\nQuestion: " + question;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 512
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
            return null;
        }

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
