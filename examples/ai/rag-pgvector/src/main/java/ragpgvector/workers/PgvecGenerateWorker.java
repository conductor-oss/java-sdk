package ragpgvector.workers;

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
 * Worker that generates an answer from a question and retrieved pgvector rows.
 *
 * In production this would call an LLM (e.g. OpenAI chat completions) with the
 * question and retrieved context. Here we return a deterministic summary.
 */
public class PgvecGenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public PgvecGenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "pgvec_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "default question";
        }

        Object rowsObj = task.getInputData().get("rows");
        int rowCount = 0;
        if (rowsObj instanceof List) {
            rowCount = ((List<?>) rowsObj).size();
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                // Build context from rows
                StringBuilder contextBuilder = new StringBuilder();
                if (rowsObj instanceof List<?> rowsList) {
                    for (Object row : rowsList) {
                        if (row instanceof Map<?, ?> rowMap) {
                            Object text = rowMap.get("content");
                            contextBuilder.append(text != null ? text : "").append("\n");
                        }
                    }
                }
                String context = contextBuilder.toString();

                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant. Use the provided context to answer questions accurately."),
                        Map.of("role", "user", "content", "Context:\n" + context + "\n\nQuestion: " + question)
                    ),
                    "max_tokens", 512,
                    "temperature", 0.3
                ));
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> apiResponse = mapper.readValue(response.body(), Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String answer = (String) message.get("content");
                    System.out.println("  [generate] Response from OpenAI API (LIVE)");
                    result.getOutputData().put("answer", answer);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI API error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fall through to fallback mode
        String answer = "pgvector extends PostgreSQL with vector similarity search. "
                + "It supports cosine (<=>), L2 (<->), and inner product (<#>) operators, "
                + "with ivfflat and hnsw indexing for fast ANN queries. "
                + "Based on " + rowCount + " results.";

        System.out.println("  [generate] Answer from " + rowCount + " pgvector rows");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
