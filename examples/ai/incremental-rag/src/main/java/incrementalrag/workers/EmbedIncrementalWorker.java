package incrementalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that generates embeddings for documents that need to be inserted
 * or updated.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Embeddings API (text-embedding-3-small).
 * Otherwise uses deterministic vectors.
 */
public class EmbedIncrementalWorker implements Worker {

    private static final List<Double> DETERMINISTIC_VECTOR = List.of(0.12, -0.34, 0.56, 0.78, -0.91);

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbedIncrementalWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "ir_embed_incremental";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> docsToEmbed = (List<Map<String, Object>>) task.getInputData().get("docsToEmbed");

        List<Map<String, Object>> embeddings = new ArrayList<>();
        List<String> docIds = new ArrayList<>();
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [embed_incremental] Calling OpenAI to embed " + docsToEmbed.size() + " documents");
            for (Map<String, Object> doc : docsToEmbed) {
                String id = (String) doc.get("id");
                String action = (String) doc.get("action");
                String text = (String) doc.get("text");
                docIds.add(id);

                try {
                    List<Double> vector = callEmbedding(text != null ? text : "", result);
                    if (vector == null) {
                        return result;
                    }
                    embeddings.add(Map.of(
                            "id", id,
                            "vector", vector,
                            "action", action
                    ));
                } catch (Exception e) {
                    System.err.println("  [embed_incremental] OpenAI call failed for " + id + ": " + e.getMessage() + " — using deterministic vector");
                    embeddings.add(Map.of(
                            "id", id,
                            "vector", DETERMINISTIC_VECTOR,
                            "action", action
                    ));
                }
            }
        } else {
            for (Map<String, Object> doc : docsToEmbed) {
                String id = (String) doc.get("id");
                String action = (String) doc.get("action");
                docIds.add(id);
                embeddings.add(Map.of(
                        "id", id,
                        "vector", DETERMINISTIC_VECTOR,
                        "action", action
                ));
            }
            System.out.println("  [embed_incremental] Embedded " + embeddings.size() + " documents");
        }
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("embeddings", embeddings);
        result.getOutputData().put("docIds", docIds);
        result.getOutputData().put("embeddedCount", embeddings.size());
        return result;
    }

    private List<Double> callEmbedding(String text, TaskResult result) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
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
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode embeddingArray = root.path("data").path(0).path("embedding");
        List<Double> embedding = new ArrayList<>();
        for (JsonNode val : embeddingArray) {
            embedding.add(val.asDouble());
        }
        return embedding;
    }
}
