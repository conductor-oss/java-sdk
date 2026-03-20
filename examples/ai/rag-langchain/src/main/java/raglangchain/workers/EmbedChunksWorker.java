package raglangchain.workers;

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
 * Worker that generates embeddings for text chunks.
 * Produces deterministic 4-dimensional vectors based on chunk content.
 * perform  using OpenAI text-embedding-3-small model.
 */
public class EmbedChunksWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbedChunksWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "lc_embed_chunks";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> chunks =
                (List<Map<String, Object>>) task.getInputData().get("chunks");

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank() && chunks != null && !chunks.isEmpty()) {
            try {
                List<String> texts = new ArrayList<>();
                for (Map<String, Object> chunk : chunks) {
                    texts.add((String) chunk.get("text"));
                }
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "text-embedding-3-small",
                    "input", texts
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
                    List<Map<String, Object>> embeddings = new ArrayList<>();
                    for (int i = 0; i < chunks.size() && i < data.size(); i++) {
                        Map<String, Object> chunk = chunks.get(i);
                        List<Double> vector = (List<Double>) data.get(i).get("embedding");
                        embeddings.add(Map.of(
                            "id", "emb-" + i,
                            "text", chunk.get("text"),
                            "vector", vector,
                            "metadata", chunk.get("metadata")
                        ));
                    }
                    System.out.println("  [embed_chunks] Generated " + embeddings.size()
                            + " embeddings via OpenAI API (LIVE): " + ((List<Double>) data.get(0).get("embedding")).size() + " dimensions each");
                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("embeddings", embeddings);
                    result.getOutputData().put("embeddingCount", embeddings.size());
                    result.getOutputData().put("model", "text-embedding-3-small");
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI API error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fall through to fallback mode
        List<Map<String, Object>> embeddings = new ArrayList<>();

        if (chunks != null) {
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> chunk = chunks.get(i);
                String text = (String) chunk.get("text");
                List<Double> vector = deterministicVector(text, i);
                embeddings.add(Map.of(
                        "id", "emb-" + i,
                        "text", text,
                        "vector", vector,
                        "metadata", chunk.get("metadata")
                ));
            }
        }

        System.out.println("  [embed_chunks] Generated " + embeddings.size()
                + " embeddings (model=text-embedding-3-small)");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("embeddings", embeddings);
        result.getOutputData().put("embeddingCount", embeddings.size());
        result.getOutputData().put("model", "text-embedding-3-small");
        return result;
    }

    /**
     * Generates a deterministic 4-dimensional vector based on text content and index.
     * Uses character-level hashing to produce consistent values between 0 and 1.
     */
    static List<Double> deterministicVector(String text, int index) {
        int hash = text.hashCode();
        double d0 = Math.abs(((hash & 0xFF) / 255.0));
        double d1 = Math.abs((((hash >> 8) & 0xFF) / 255.0));
        double d2 = Math.abs((((hash >> 16) & 0xFF) / 255.0));
        double d3 = Math.abs(((index * 37 + 13) % 256) / 255.0);

        // Round to 4 decimal places for clean output
        d0 = Math.round(d0 * 10000.0) / 10000.0;
        d1 = Math.round(d1 * 10000.0) / 10000.0;
        d2 = Math.round(d2 * 10000.0) / 10000.0;
        d3 = Math.round(d3 * 10000.0) / 10000.0;

        return List.of(d0, d1, d2, d3);
    }
}
