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
import java.util.List;
import java.util.Map;

/**
 * Worker that generates an answer using retrieved context documents.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI Chat Completions API
 * (gpt-4o-mini).
 */
public class GenerateAnswerWorker implements Worker {

    private static final String API_KEY = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    private static final String CHAT_MODEL = configuredChatModel();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GenerateAnswerWorker() {
    }

    @Override
    public String getTaskDefName() {
        return "brag_generate_answer";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        List<Map<String, Object>> context = (List<Map<String, Object>>) task.getInputData().get("context");

        int contextSize = (context != null) ? context.size() : 0;

        TaskResult result = new TaskResult(task);

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        try {
            return executeReal(task, result, question, context, contextSize);
        } catch (Exception e) {
            System.err.println("  [generate] OpenAI API call failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeReal(Task task, TaskResult result, String question,
                                   List<Map<String, Object>> context, int contextSize) throws Exception {
        // Build context string from retrieved documents
        StringBuilder contextBuilder = new StringBuilder();
        if (context != null) {
            for (int i = 0; i < context.size(); i++) {
                Map<String, Object> doc = context.get(i);
                contextBuilder.append("[").append(i + 1).append("] ");
                contextBuilder.append(doc.getOrDefault("text", ""));
                contextBuilder.append("\n");
            }
        }

        String systemPrompt = "Answer the question using only the provided context. "
                + "Be concise and accurate. If the context does not contain enough information, say so.";
        String userPrompt = "Context:\n" + contextBuilder + "\nQuestion: " + question;

        String escapedSystem = escapeJson(systemPrompt);
        String escapedUser = escapeJson(userPrompt);

        String body = String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": 500,
                  "temperature": 0.3
                }""", CHAT_MODEL, escapedSystem, escapedUser);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
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
        String answer = root.get("choices").get(0).get("message").get("content").asText();
        int totalTokens = root.get("usage").get("total_tokens").asInt();
        String model = root.get("model").asText();

        System.out.println("  [generate] Produced answer using " + contextSize
                + " context docs (OpenAI " + model + ", " + totalTokens + " tokens)");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("tokensUsed", totalTokens);
        result.getOutputData().put("model", model);
        return result;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String configuredChatModel() {
        String configured = System.getenv("OPENAI_CHAT_MODEL");
        return configured != null && !configured.isBlank() ? configured : "gpt-4o-mini";
    }
}
