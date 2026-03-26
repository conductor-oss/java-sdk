package ragmultiquery.workers;

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
 * Worker that expands a user question into multiple search query variants.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI chat completions API (gpt-4o-mini).
 * Otherwise falls back to fixed deterministic query variants.
 */
public class ExpandQueriesWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ExpandQueriesWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "mq_expand_queries";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                List<String> queries = callOpenAIExpand(question, result);
                if (queries == null) {
                    return result;
                }
                System.out.println("  [expand] Rephrasing (live OpenAI): \"" + question + "\"");
                System.out.println("  [expand] Generated " + queries.size() + " query variants");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("queries", queries);
            } catch (Exception e) {
                System.err.println("  [expand] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            List<String> queries = List.of(
                    "What are the benefits of workflow orchestration?",
                    "Why use Conductor for microservice coordination?",
                    "Advantages of orchestration vs choreography patterns"
            );

            System.out.println("  [expand] Rephrasing: \"" + question + "\"");
            System.out.println("  [expand] Generated " + queries.size() + " query variants");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("queries", queries);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> callOpenAIExpand(String question, TaskResult result) throws Exception {
        String systemPrompt = "You are a search query optimizer. Given a question, generate exactly 3 alternative "
                + "search queries that would help find relevant documents from different angles. "
                + "Return ONLY the 3 queries, one per line, without numbering or bullet points.";

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

        while (queries.size() < 3) {
            queries.add(question);
        }

        return queries.subList(0, 3);
    }
}
