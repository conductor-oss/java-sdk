package ragmongodb.workers;

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
 * Worker that generates an answer from the question and retrieved documents.
 * In production this would call an LLM (e.g. GPT-4, Claude) with the docs as context.
 */
public class MongoGenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public MongoGenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "mongo_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        List<Object> docs = (List<Object>) task.getInputData().get("docs");
        int docCount = docs != null ? docs.size() : 0;

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String context = (docs != null) ? mapper.writeValueAsString(docs) : "No results available.";
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
        String answer = "MongoDB Atlas Vector Search uses the $vectorSearch aggregation stage "
                + "to find semantically similar documents. It requires a vector search index "
                + "on the embedding field and supports numCandidates for ANN tuning. "
                + "Based on " + docCount + " retrieved docs.";

        System.out.println("  [generate] Answer from " + docCount + " MongoDB documents");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
