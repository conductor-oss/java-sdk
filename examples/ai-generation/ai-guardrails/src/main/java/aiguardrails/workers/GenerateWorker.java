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

public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "grl_generate";
    }

    @Override
    public TaskResult execute(Task task) {

        String prompt = (String) task.getInputData().get("prompt");
        String modelId = (String) task.getInputData().get("modelId");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a helpful AI assistant. Generate a clear, accurate, and safe response to the user's prompt. Stay factual and avoid speculation.";
                String userMsg = prompt != null ? prompt : "Explain machine learning briefly.";
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
                    System.out.println("  [generate] Response from OpenAI (LIVE)");
                    result.getOutputData().put("response", content);
                    result.getOutputData().put("tokens", content.split("\\s+").length);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.printf("  [generate] Response generated using %s%n", prompt, modelId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", "Machine learning uses statistical methods to learn from data.");
        result.getOutputData().put("tokens", 18);
        return result;
    }
}
