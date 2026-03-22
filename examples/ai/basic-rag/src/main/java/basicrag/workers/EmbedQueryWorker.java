package basicrag.workers;

import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Worker that embeds a question into a vector representation.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI Embeddings API
 * (text-embedding-3-small).
 */
public class EmbedQueryWorker implements Worker {

    private static final String API_KEY = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    private static final String EMBED_MODEL = configuredEmbedModel();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public EmbedQueryWorker() {
    }

    @Override
    public String getTaskDefName() {
        return "brag_embed_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'question' is required and must not be blank");
            return fail;
        }

        TaskResult result = new TaskResult(task);

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        try {
            return executeReal(task, result, question);
        } catch (Exception e) {
            System.err.println("  [embed] OpenAI API call failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeReal(Task task, TaskResult result, String question) throws Exception {
        String escapedQuestion = question.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        String body = String.format("""
                {"model": "%s", "input": "%s"}""",
                EMBED_MODEL,
                escapedQuestion);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/embeddings"))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("  [worker] API error HTTP " + response.statusCode() + ": " + errorBody);
            // 429 (rate limit) and 503 (overloaded) are retryable in production,
            // but this example intentionally uses retryCount=0 so failures surface immediately.
            if (response.statusCode() == 429 || response.statusCode() == 503) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("API rate limited (HTTP " + response.statusCode()
                        + "). This example uses retryCount=0, so rerun after quota recovers or raise retries for production.");
            } else {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("API error HTTP " + response.statusCode() + ": " + errorBody);
            }
            result.getOutputData().put("errorBody", errorBody);
            result.getOutputData().put("httpStatus", response.statusCode());
            return result;
        }

        JsonNode root = MAPPER.readTree(response.body());
        JsonNode embeddingNode = root.get("data").get(0).get("embedding");

        List<Double> embedding = new ArrayList<>();
        for (JsonNode val : embeddingNode) {
            embedding.add(val.asDouble());
        }

        System.out.println("  [embed] Query: \"" + question + "\" -> " + embedding.size()
                + "-dim vector (OpenAI " + EMBED_MODEL + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("embedding", embedding);
        result.getOutputData().put("model", EMBED_MODEL);
        result.getOutputData().put("dimensions", embedding.size());
        return result;
    }

    public static String configuredEmbedModel() {
        String configured = System.getenv("OPENAI_EMBED_MODEL");
        return configured != null && !configured.isBlank() ? configured : "text-embedding-3-small";
    }
}
