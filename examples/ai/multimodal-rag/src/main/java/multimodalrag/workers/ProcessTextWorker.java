package multimodalrag.workers;

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
 * Worker that processes text content by generating an embedding vector
 * and extracting keywords. Returns an 8-dimensional embedding and
 * a list of extracted keywords.
 */
public class ProcessTextWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProcessTextWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "mm_process_text";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        String textContent = (String) task.getInputData().get("textContent");
        if (textContent == null) {
            textContent = "";
        }

        TaskResult result = new TaskResult(task);

        List<String> keywords = List.of("multimodal", "search", "retrieval", "embedding", "analysis");

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String textToEmbed = textContent.isEmpty() ? question : textContent;
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "text-embedding-3-small",
                    "input", textToEmbed
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
                    System.out.println("  [embed] Generated embedding via OpenAI (LIVE): " + embedding.size() + " dims");
                    result.getOutputData().put("embedding", embedding);
                    result.getOutputData().put("keywords", keywords);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [embed] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fixed deterministic 8-dimensional embedding
        List<Double> embedding = List.of(0.12, -0.34, 0.56, 0.23, -0.78, 0.45, -0.11, 0.67);

        String preview = textContent.length() > 50
                ? textContent.substring(0, 50) + "..."
                : textContent;
        System.out.println("  [process_text] Embedded text: \"" + preview
                + "\" -> " + embedding.size() + " dims, " + keywords.size() + " keywords");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("embedding", embedding);
        result.getOutputData().put("keywords", keywords);
        return result;
    }
}
