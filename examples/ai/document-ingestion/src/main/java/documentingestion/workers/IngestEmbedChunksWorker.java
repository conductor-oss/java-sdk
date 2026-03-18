package documentingestion.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker 3: Generates embeddings for each text chunk.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI Embeddings API.
 */
public class IngestEmbedChunksWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public IngestEmbedChunksWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
    }

    @Override
    public String getTaskDefName() {
        return "ingest_embed_chunks";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) task.getInputData().get("chunks");

        TaskResult result = new TaskResult(task);

        try {
            List<Map<String, Object>> vectors = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> chunk = chunks.get(i);
                String chunkText = (String) chunk.get("text");

                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "text-embedding-3-small",
                    "input", chunkText
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

                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("text", chunkText.length() > 100 ? chunkText.substring(0, 100) : chunkText);
                    metadata.put("wordCount", chunk.get("wordCount"));

                    Map<String, Object> vector = new LinkedHashMap<>();
                    vector.put("id", chunk.get("id"));
                    vector.put("embedding", embedding);
                    vector.put("metadata", metadata);
                    vectors.add(vector);
                } else {
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
                    return result;
                }
            }
            System.out.println("  [embed] Generated " + vectors.size() + " embeddings via OpenAI");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("vectors", vectors);
            result.getOutputData().put("model", "text-embedding-3-small");
            return result;
        } catch (Exception e) {
            System.err.println("  [embed] OpenAI error: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            return result;
        }
    }
}
