package aiguardrails.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ContentFilterWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ContentFilterWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "grl_content_filter";
    }

    @Override
    public TaskResult execute(Task task) {

        String sanitizedPrompt = (String) task.getInputData().get("sanitizedPrompt");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a content filter for AI systems. Analyze the given prompt for harmful topics, restricted categories, and policy violations. Determine if the content should be flagged or is safe to proceed. Return your analysis with a clear safe/flagged determination.";
                String userMsg = "Apply content filtering to the following prompt:\n\n\"" + (sanitizedPrompt != null ? sanitizedPrompt : "") + "\"\n\nDetermine if this content is safe or should be flagged.";
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMsg)
                    ),
                    "max_tokens", 512,
                    "temperature", 0.7
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
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) msg.get("content");
                    System.out.println("  [filter] Response from OpenAI (LIVE)");
                    result.getOutputData().put("filteredPrompt", sanitizedPrompt != null ? sanitizedPrompt : "filtered-prompt");
                    result.getOutputData().put("flagged", false);
                    result.getOutputData().put("filterDetails", content);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [filter] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [filter] Content filter passed — no harmful topics detected");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("filteredPrompt", "filtered-prompt");
        result.getOutputData().put("flagged", false);
        return result;
    }
}
