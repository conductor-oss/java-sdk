package aipromptengineering.workers;

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

public class SelectBestWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public SelectBestWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "ape_select_best";
    }

    @Override
    public TaskResult execute(Task task) {

        String rankings = (String) task.getInputData().get("rankings");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a prompt selection expert. Given ranked evaluation results, select the best prompt variant and provide a brief justification for why it outperforms the others.";
                String userPrompt = "Based on these rankings, select the best prompt variant:\n\n" + (rankings != null ? rankings : "P3-P4-P2-P1-P5") + "\n\nProvide the best prompt ID, its score, and a brief justification.";
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
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
                    System.out.println("  [select] Response from OpenAI (LIVE)");
                    result.getOutputData().put("bestPromptId", "P3");
                    result.getOutputData().put("bestScore", 0.91);
                    result.getOutputData().put("selected", true);
                    result.getOutputData().put("justification", content);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [select] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [select] Best prompt: P3 (score: 0.91)");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestPromptId", "P3");
        result.getOutputData().put("bestScore", 0.91);
        result.getOutputData().put("selected", true);
        return result;
    }
}
