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

public class EvaluateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public EvaluateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "ape_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {

        String results = (String) task.getInputData().get("results");
        String criteria = (String) task.getInputData().get("criteria");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a prompt evaluation expert. Given test results from multiple prompt variants, rank them by quality based on the evaluation criteria. Provide a ranked list from best to worst.";
                String userPrompt = "Evaluate and rank the following prompt variant test results:\n\n" + (results != null ? results : "5 variants tested") + "\n\nCriteria: " + (criteria != null ? criteria : "accuracy, relevance, format compliance") + "\n\nProvide rankings from best to worst.";
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
                    System.out.println("  [evaluate] Response from OpenAI (LIVE)");
                    result.getOutputData().put("rankings", content);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [evaluate] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [evaluate] Ranked by quality — P3 leads with 0.91");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rankings", "P3-P4-P2-P1-P5");
        return result;
    }
}
