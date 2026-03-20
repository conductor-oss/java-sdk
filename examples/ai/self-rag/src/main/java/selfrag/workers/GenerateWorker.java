package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Generates an answer from relevant documents.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Chat Completions API (gpt-4o-mini).
 * Otherwise returns a fixed answer about Conductor task types.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "sr_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> relevantDocs =
                (List<Map<String, Object>>) task.getInputData().get("relevantDocs");
        int docCount = relevantDocs != null ? relevantDocs.size() : 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [generate] Calling OpenAI to produce answer from " + docCount + " docs...");
            try {
                String question = (String) task.getInputData().get("question");
                StringBuilder contextStr = new StringBuilder();
                if (relevantDocs != null) {
                    for (Map<String, Object> doc : relevantDocs) {
                        contextStr.append("- ").append(doc.get("text")).append("\n");
                    }
                }

                String systemPrompt = "You are a helpful assistant. Answer the question based on the provided context documents. Be concise and factual.";
                String userPrompt = "Context:\n" + contextStr + "\nQuestion: " + (question != null ? question : "");

                String answer = callChatCompletion(systemPrompt, userPrompt, result);
                if (answer == null) {
                    return result;
                }
                result.getOutputData().put("answer", answer);
                result.getOutputData().put("mode", "live");
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI call failed: " + e.getMessage() + " — falling back to deterministic");
                result.getOutputData().put("answer",
                        "Conductor supports SIMPLE tasks (worker-executed), SYSTEM tasks like FORK_JOIN "
                                + "for parallelism, and DO_WHILE for loops. These compose into complex workflows.");
                result.getOutputData().put("mode", "deterministic");
            }
        } else {
            System.out.println("  [generate] Producing answer from " + docCount + " docs...");
            result.getOutputData().put("answer",
                    "Conductor supports SIMPLE tasks (worker-executed), SYSTEM tasks like FORK_JOIN "
                            + "for parallelism, and DO_WHILE for loops. These compose into complex workflows.");
        }
        return result;
    }

    private String callChatCompletion(String systemPrompt, String userPrompt, TaskResult result) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 512,
                "temperature", 0.3
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
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
        return root.path("choices").path(0).path("message").path("content").asText();
    }
}
