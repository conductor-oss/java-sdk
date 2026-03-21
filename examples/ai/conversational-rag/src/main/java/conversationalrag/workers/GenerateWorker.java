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

/**
 * Worker that generates a response using user message, conversation history,
 * and retrieved context documents.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Chat Completions API (gpt-4o-mini).
 * Otherwise returns a different response depending on whether history is empty
 * (first turn) or not (follow-up turn).
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
        return "crag_generate";
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

        List<Map<String, Object>> context = (List<Map<String, Object>>) task.getInputData().get("context");
        int contextSize = (context != null) ? context.size() : 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [generate] Calling OpenAI with " + history.size()
                    + " history turns + " + contextSize + " context docs");
            try {
                // Build messages for chat completion
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system",
                        "content", "You are a helpful assistant. Use the provided context documents and conversation history to answer the user's question. Be concise and informative."));

                // Add context as system message
                if (context != null && !context.isEmpty()) {
                    StringBuilder contextStr = new StringBuilder("Context documents:\n");
                    for (Map<String, Object> doc : context) {
                        contextStr.append("- ").append(doc.get("text")).append("\n");
                    }
                    messages.add(Map.of("role", "system", "content", contextStr.toString()));
                }

                // Add conversation history
                for (Map<String, String> turn : history) {
                    messages.add(Map.of("role", "user", "content", turn.getOrDefault("user", "")));
                    messages.add(Map.of("role", "assistant", "content", turn.getOrDefault("assistant", "")));
                }

                // Add current user message
                messages.add(Map.of("role", "user", "content", userMessage));

                String response = callChatCompletion(messages, result);
                if (response == null) {
                    return result;
                }
                result.getOutputData().put("response", response);
                result.getOutputData().put("mode", "live");
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI call failed: " + e.getMessage() + " — falling back to deterministic");
                setDefaultOutput(result, userMessage, history, contextSize);
            }
        } else {
            System.out.println("  [generate] Building response with " + history.size()
                    + " history turns + " + contextSize + " context docs");
            setDefaultOutput(result, userMessage, history, contextSize);
        }
        return result;
    }

    private void setDefaultOutput(TaskResult result, String userMessage,
                                     List<Map<String, String>> history, int contextSize) {
        String response;
        if (!history.isEmpty()) {
            response = "Following up on our conversation: " + userMessage
                    + " — Conductor supports versioning, multi-language workers, and JSON-based data flow. (Using "
                    + contextSize + " sources and " + history.size() + " prior turns)";
        } else {
            response = "To answer your question about \"" + userMessage
                    + "\": Conductor supports workflow versioning, polyglot workers via HTTP, and JSON inputs/outputs for flexible orchestration. (Using "
                    + contextSize + " sources)";
        }
        result.getOutputData().put("response", response);
    }

    private String callChatCompletion(List<Map<String, String>> messages, TaskResult result) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages,
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
