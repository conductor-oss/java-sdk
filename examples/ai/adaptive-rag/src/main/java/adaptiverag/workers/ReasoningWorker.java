package adaptiverag.workers;

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
 * Reasoning worker that builds a chain-of-thought from retrieved documents.
 *
 * Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls OpenAI Chat Completions API (gpt-4o-mini).
 */
public class ReasoningWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReasoningWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "ar_reason";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'question' is required and must not be blank");
            return fail;
        }

        List<Map<String, Object>> documents = (List<Map<String, Object>>) task.getInputData().get("documents");
        if (documents == null || documents.isEmpty()) {
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            fail.setReasonForIncompletion("Input 'documents' is required and must contain at least one document");
            return fail;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        System.out.println("  [reasoning] Calling OpenAI to build chain-of-thought...");
        try {
            StringBuilder docsStr = new StringBuilder();
            for (Map<String, Object> doc : documents) {
                docsStr.append("- ").append(doc.get("text")).append("\n");
            }

            String systemPrompt = "You are a reasoning assistant. Build a chain-of-thought analysis from the provided documents. "
                    + "Output exactly 3 reasoning steps, one per line. Each step should build on the previous.";
            String userPrompt = "Documents:\n" + docsStr + "\nQuestion: " + (question != null ? question : "");

            String responseText = callChatCompletion(systemPrompt, userPrompt, result);
            if (responseText == null) {
                return result;
            }
            String[] lines = responseText.split("\n");
            List<String> chain = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    chain.add(trimmed);
                }
            }
            if (chain.isEmpty()) {
                chain.add(responseText.trim());
            }

            result.getOutputData().put("chain", chain);
            result.getOutputData().put("steps", chain.size());
        } catch (Exception e) {
            System.err.println("  [reasoning] OpenAI call failed: " + e.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
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
