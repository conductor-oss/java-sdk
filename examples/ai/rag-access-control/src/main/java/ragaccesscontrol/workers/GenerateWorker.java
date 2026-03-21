package ragaccesscontrol.workers;

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
import java.util.stream.Collectors;

/**
 * Worker that generates an answer from the access-controlled and redacted context.
 * Notes when data has been redacted due to access control policies.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "ac_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        List<Map<String, Object>> context = (List<Map<String, Object>>) task.getInputData().get("context");
        String userId = (String) task.getInputData().get("userId");

        String combinedContext = context.stream()
                .map(doc -> (String) doc.get("content"))
                .collect(Collectors.joining(" "));

        boolean hasRedacted = combinedContext.contains("[SSN REDACTED]")
                || combinedContext.contains("[SALARY REDACTED]");

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant. Use the provided context to answer questions accurately. Note if any data has been redacted."),
                        Map.of("role", "user", "content", "Context:\n" + combinedContext + "\n\nQuestion: " + question)
                    ),
                    "max_tokens", 512,
                    "temperature", 0.3
                ));
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> apiResponse = mapper.readValue(response.body(), Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String answer = (String) message.get("content");
                    System.out.println("  [generate] Response from OpenAI API (LIVE)");
                    result.getOutputData().put("answer", answer);
                    result.getOutputData().put("sourcesUsed", context.size());
                    result.getOutputData().put("hasRedactedContent", hasRedacted);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI API error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fall through to fallback mode
        String answer = "Based on your authorized access level, here is the answer to '"
                + question + "': " + combinedContext;

        if (hasRedacted) {
            answer += " Note: Some sensitive information has been redacted based on your clearance level.";
        }

        System.out.println("  [generate] Generated answer for user " + userId
                + " using " + context.size() + " context documents"
                + (hasRedacted ? " (contains redacted data)" : ""));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("sourcesUsed", context.size());
        result.getOutputData().put("hasRedactedContent", hasRedacted);
        return result;
    }
}
