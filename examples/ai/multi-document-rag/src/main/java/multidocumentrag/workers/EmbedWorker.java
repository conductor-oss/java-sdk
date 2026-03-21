package multidocumentrag.workers;

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
import java.util.Random;

/**
 * Worker that generates an embedding vector for a query.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Embeddings API (text-embedding-3-small).
 * Otherwise returns a fixed deterministic embedding.
 */
public class EmbedWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbedWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "mdrag_embed";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [embed] Calling OpenAI to embed query: \"" + question + "\"");
            try {
                List<Double> embedding = callEmbedding(question != null ? question : "", result);
                if (embedding == null) {
                    return result;
                }
                result.getOutputData().put("embedding", embedding);
                result.getOutputData().put("mode", "live");
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI call failed: " + e.getMessage() + " — falling back to deterministic");
                result.getOutputData().put("embedding", getDeterministicEmbedding());
                result.getOutputData().put("mode", "deterministic");
            }
        } else {
            System.out.println("  [embed] Query: \"" + question + "\"");
            result.getOutputData().put("embedding", getDeterministicEmbedding());
        }

        return result;
    }

    private List<Double> getDeterministicEmbedding() {
        Random rng = new Random(42);
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double val = Math.round((rng.nextDouble() * 2 - 1) * 10000.0) / 10000.0;
            embedding.add(val);
        }
        return embedding;
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
