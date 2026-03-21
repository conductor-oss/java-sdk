package ragpinecone.workers;

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
 * Worker that generates a fixed embedding vector for a question.
 * In production this would call an embedding API (e.g. OpenAI text-embedding-ada-002).
 */
public class PineEmbedWorker implements Worker {

    /** Fixed 8-dimensional embedding for deterministic behavior. */
    private static final List<Double> FIXED_EMBEDDING = List.of(
            0.021, -0.034, 0.078, 0.055, -0.012, 0.091, -0.067, 0.043
    );

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public PineEmbedWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "pine_embed";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "What is Pinecone?";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "text-embedding-3-small",
                    "input", question
                ));
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/embeddings"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> apiResponse = mapper.readValue(response.body(), Map.class);
                    List<Map<String, Object>> data = (List<Map<String, Object>>) apiResponse.get("data");
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    System.out.println("  [embed] Generated embedding via OpenAI API (LIVE): " + embedding.size() + " dimensions");
                    result.getOutputData().put("embedding", embedding);
                    result.getOutputData().put("dimension", embedding.size());
                    result.getOutputData().put("model", "text-embedding-3-small");
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI API error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fall through to fallback mode
        System.out.println("  [embed] Embedding question: " + question);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("embedding", FIXED_EMBEDDING);
        result.getOutputData().put("dimension", FIXED_EMBEDDING.size());
        result.getOutputData().put("model", "text-embedding-ada-002");
        return result;
    }
}
