package ragfusion.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that rewrites the original question into 3 variant queries
 * for multi-perspective retrieval. When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI
 * chat completions API (gpt-4o-mini). Otherwise falls back to deterministic variants.
 */
public class RewriteQueriesWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RewriteQueriesWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "rf_rewrite_queries";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                List<String> variants = callOpenAIRewrite(question, result);
                if (variants == null) {
                    return result;
                }
                System.out.println("  [rewrite] Generated " + variants.size() + " query variants (live OpenAI) for: \"" + question + "\"");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("query1", variants.get(0));
                result.getOutputData().put("query2", variants.get(1));
                result.getOutputData().put("query3", variants.size() > 2 ? variants.get(2) : variants.get(1));
                result.getOutputData().put("variantCount", 3);
            } catch (Exception e) {
                System.err.println("  [rewrite] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            List<String> variants = List.of(
                    "What are the key aspects of: " + question,
                    "Explain in detail: " + question,
                    "Provide a comprehensive overview of: " + question
            );

            System.out.println("  [rewrite] Generated 3 query variants for: \"" + question + "\"");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("query1", variants.get(0));
            result.getOutputData().put("query2", variants.get(1));
            result.getOutputData().put("query3", variants.get(2));
            result.getOutputData().put("variantCount", 3);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> callOpenAIRewrite(String question, TaskResult result) throws Exception {
        String systemPrompt = "You are a search query optimizer. Given a question, generate exactly 3 alternative "
                + "phrasings that would help find relevant documents. Return ONLY the 3 queries, one per line, "
                + "without numbering or bullet points.";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", question)
                ),
                "max_tokens", 256
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
        String content = (String) message.get("content");

        List<String> queries = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                queries.add(trimmed);
            }
        }

        // Ensure at least 3 variants
        while (queries.size() < 3) {
            queries.add(question);
        }

        return queries.subList(0, 3);
    }
}
