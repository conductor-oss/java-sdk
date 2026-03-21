package conversationalrag.workers;

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
import java.util.stream.Collectors;

/**
 * Worker that embeds the user query with conversational context.
 * Combines recent history with the current message to form a contextual query
 * for better retrieval.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Embeddings API (text-embedding-3-small).
 * Otherwise returns a fixed embedding vector.
 */
public class EmbedWithContextWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbedWithContextWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "crag_embed_with_context";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String userMessage = (String) task.getInputData().get("userMessage");
        if (userMessage == null) {
            userMessage = "";
        }

        List<Map<String, String>> history = (List<Map<String, String>>) task.getInputData().get("history");
        if (history == null) {
            history = List.of();
        }

        // Combine recent history (last 2 turns) with current message for contextual query
        String recentContext = history.stream()
                .skip(Math.max(0, history.size() - 2))
                .map(turn -> turn.get("user"))
                .collect(Collectors.joining(" "));

        String contextualQuery = recentContext.isEmpty()
                ? userMessage
                : recentContext + " " + userMessage;

        String preview = contextualQuery.length() > 60
                ? contextualQuery.substring(0, 60) + "..."
                : contextualQuery;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [embed] Calling OpenAI to embed contextual query: \"" + preview + "\"");
            try {
                List<Double> embedding = callEmbedding(contextualQuery, result);
                if (embedding == null) {
                    return result;
                }
                result.getOutputData().put("embedding", embedding);
                result.getOutputData().put("contextualQuery", contextualQuery);
                result.getOutputData().put("mode", "live");
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI call failed: " + e.getMessage() + " — falling back to deterministic");
                result.getOutputData().put("embedding", List.of(0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7));
                result.getOutputData().put("contextualQuery", contextualQuery);
                result.getOutputData().put("mode", "deterministic");
            }
        } else {
            System.out.println("  [embed] Contextual query: \"" + preview + "\"");
            // Fixed deterministic embedding (8 dimensions)
            List<Double> embedding = List.of(0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7);
            result.getOutputData().put("embedding", embedding);
            result.getOutputData().put("contextualQuery", contextualQuery);
        }

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
