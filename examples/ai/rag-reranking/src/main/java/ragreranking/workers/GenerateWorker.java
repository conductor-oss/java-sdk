package ragreranking.workers;

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
import java.util.stream.Collectors;

/**
 * Worker that generates an answer using re-ranked context documents.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI chat completions API (gpt-4o-mini).
 * Otherwise falls back to a default answer.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "rerank_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, Object>> context =
                (List<Map<String, Object>>) task.getInputData().get("context");
        int contextSize = (context != null) ? context.size() : 0;

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String contextText = "";
                if (context != null && !context.isEmpty()) {
                    contextText = context.stream()
                            .map(doc -> (String) doc.get("text"))
                            .filter(t -> t != null)
                            .collect(Collectors.joining("\n"));
                }
                String answer = callOpenAIChat(question, contextText, result);
                if (answer == null) {
                    return result;
                }
                System.out.println("  [generate] Answer (live OpenAI) from " + contextSize + " re-ranked documents");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("answer", answer);
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            String answer = "Re-ranking improves RAG precision: cross-encoder models score "
                    + "query-document pairs jointly (unlike bi-encoders which embed independently). "
                    + "Popular models include Cohere Rerank and ms-marco-MiniLM. "
                    + "This two-stage retrieve-then-rerank approach yields higher quality context "
                    + "for generation. Based on " + contextSize + " re-ranked sources.";

            System.out.println("  [generate] Answer from " + contextSize + " re-ranked documents");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("answer", answer);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String callOpenAIChat(String question, String contextText, TaskResult result) throws Exception {
        String systemPrompt = "You are a helpful assistant. Answer the user's question based on the provided context. "
                + "If the context is insufficient, say so.";
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
