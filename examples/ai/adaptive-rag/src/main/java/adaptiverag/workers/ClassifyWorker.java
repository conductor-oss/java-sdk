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
 * Classifies a question as factual, analytical, or creative.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls OpenAI Chat Completions API (gpt-4o-mini).
 */
public class ClassifyWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClassifyWorker() {
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
        return "ar_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        System.out.println("  [classify] Calling OpenAI to classify: \"" + question + "\"");
        try {
            String systemPrompt = "You are a query classifier. Classify the question into exactly one category: factual, analytical, or creative. "
                    + "Respond with ONLY a JSON object: {\"queryType\": \"...\", \"confidence\": 0.XX}. "
                    + "factual = simple factual lookups. analytical = comparisons, trade-offs, multi-step reasoning. creative = imaginative, stories, poems.";
            String userPrompt = "Classify: " + (question != null ? question : "");

            String responseText = callChatCompletion(systemPrompt, userPrompt, result);
            if (responseText == null) {
                return result;
            }
            // Parse the JSON response
            JsonNode parsed = objectMapper.readTree(responseText);
            String queryType = parsed.path("queryType").asText("factual");
            double confidence = parsed.path("confidence").asDouble(0.85);

            // Validate queryType
            if (!"factual".equals(queryType) && !"analytical".equals(queryType) && !"creative".equals(queryType)) {
                queryType = "factual";
            }

            System.out.println("  [classify] \"" + question + "\" -> " + queryType + " (" + confidence + ")");
            result.getOutputData().put("queryType", queryType);
            result.getOutputData().put("confidence", confidence);
        } catch (Exception e) {
            System.err.println("  [classify] OpenAI call failed: " + e.getMessage());
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
                "max_tokens", 100,
                "temperature", 0.1
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
