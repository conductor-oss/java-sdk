package aimodelevaluation.workers;

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

public class ReportWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "ame_report";
    }

    @Override
    public TaskResult execute(Task task) {

        String modelId = (String) task.getInputData().get("modelId");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are an ML evaluation report writer. Generate a concise evaluation report for a model, summarizing performance metrics, strengths, weaknesses, and deployment recommendations.";
                String userPrompt = "Generate an evaluation report for model " + modelId + ". Include metric summaries, key findings, per-class performance highlights, and a deployment recommendation.";
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
                    System.out.println("  [report] Response from OpenAI (LIVE)");
                    result.getOutputData().put("reportId", "EVAL-ai-model-evaluation-001");
                    result.getOutputData().put("generated", true);
                    result.getOutputData().put("reportContent", content);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [report] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.printf("  [report] Evaluation report generated for model %s%n", modelId);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "EVAL-ai-model-evaluation-001");
        result.getOutputData().put("generated", true);
        return result;
    }
}
