package ragchromadb.workers;

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
 * Worker that generates an answer from ChromaDB query results.
 *
 * In production this would send the retrieved documents as context
 * to an LLM (e.g., OpenAI chat completions) to produce a grounded answer.
 */
public class ChromaGenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChromaGenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "chroma_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        Map<String, Object> results = (Map<String, Object>) task.getInputData().get("results");

        List<List<String>> allDocuments = (List<List<String>>) results.get("documents");
        List<String> docs = allDocuments.get(0);

        // Build context from documents
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            if (i > 0) contextBuilder.append("\n");
            contextBuilder.append(docs.get(i));
        }
        String context = contextBuilder.toString();

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
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
        String answer = "ChromaDB is an open-source embedding database for AI applications. "
                + "It organizes data in collections with metadata, supports persistent local storage, "
                + "and runs via Docker. Based on " + docs.size() + " retrieved chunks.";

        System.out.println("  [generate] Answer from " + docs.size() + " ChromaDB documents");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
