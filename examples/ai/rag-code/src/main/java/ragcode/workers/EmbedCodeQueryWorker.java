package ragcode.workers;

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
 * Worker that embeds a parsed code query into a vector representation.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI embeddings API (text-embedding-3-small).
 * Otherwise falls back to a fixed deterministic.embedding and code filter.
 */
public class EmbedCodeQueryWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbedCodeQueryWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "cr_embed_code_query";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String parsedIntent = (String) task.getInputData().get("parsedIntent");
        if (parsedIntent == null) {
            parsedIntent = "";
        }

        List<String> keywords = (List<String>) task.getInputData().get("keywords");
        if (keywords == null) {
            keywords = List.of();
        }

        String language = (String) task.getInputData().get("language");
        if (language == null) {
            language = "java";
        }

        // Code filter with node types relevant to the intent
        Map<String, Object> codeFilter = Map.of(
                "language", language,
                "nodeTypes", List.of("function_declaration", "method_definition", "call_expression")
        );

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String textToEmbed = parsedIntent + " " + String.join(" ", keywords) + " " + language;
                List<Double> embedding = callOpenAIEmbedding(textToEmbed, result);
                if (embedding == null) {
                    return result;
                }
                System.out.println("  [embed_code_query] Embedding (live OpenAI) intent: \"" + parsedIntent
                        + "\" with " + keywords.size() + " keywords for " + language
                        + " -> " + embedding.size() + " dims");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("embedding", embedding);
                result.getOutputData().put("codeFilter", codeFilter);
            } catch (Exception e) {
                System.err.println("  [embed_code_query] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            System.out.println("  [embed_code_query] Embedding intent: \"" + parsedIntent
                    + "\" with " + keywords.size() + " keywords for " + language);

            // Fixed deterministic embedding (8 dimensions)
            List<Double> embedding = List.of(0.25, -0.15, 0.68, 0.33, -0.52, 0.41, -0.27, 0.59);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("embedding", embedding);
            result.getOutputData().put("codeFilter", codeFilter);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Double> callOpenAIEmbedding(String text, TaskResult result) throws Exception {
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
            // 429 (rate limit) and 503 (overloaded) are retryable
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

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
        return (List<Double>) data.get(0).get("embedding");
    }
}
