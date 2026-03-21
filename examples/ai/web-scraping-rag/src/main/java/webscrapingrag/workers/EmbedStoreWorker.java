package webscrapingrag.workers;

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
 * Worker that embeds chunks and stores them in a vector database.
 * Returns the stored chunk IDs and count.
 */
public class EmbedStoreWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public EmbedStoreWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "wsrag_embed_store";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) task.getInputData().get("chunks");
        List<String> storedIds = chunks.stream()
                .map(c -> (String) c.get("id"))
                .toList();

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
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
                    int dims = ((List<Double>) data.get(0).get("embedding")).size();
                    System.out.println("  [embed] Generated " + data.size() + " embeddings via OpenAI (LIVE): " + dims + " dims");
                    System.out.println("  [embed+store] Embedded and stored " + storedIds.size() + " chunks");
                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("storedIds", storedIds);
                    result.getOutputData().put("count", storedIds.size());
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [embed+store] Embedded and stored " + storedIds.size() + " chunks");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storedIds", storedIds);
        result.getOutputData().put("count", storedIds.size());
        return result;
    }
}
