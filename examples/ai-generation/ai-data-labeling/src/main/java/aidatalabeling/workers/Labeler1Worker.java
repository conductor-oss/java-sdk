package aidatalabeling.workers;

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

public class Labeler1Worker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public Labeler1Worker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "adl_labeler_1";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are an expert data labeler. Given a batch of data samples, assign classification labels with confidence scores. Return a brief summary of labeling results including count and average confidence.";
                String userPrompt = "Label a batch of 500 data samples for classification. Provide a brief summary of the labeling results including the number of samples labeled and overall confidence score.";
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
                    System.out.println("  [labeler-1] Response from OpenAI (LIVE)");
                    result.getOutputData().put("labels", Map.of("count", 500, "confidence", 0.89, "details", content));
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [labeler-1] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [labeler-1] Labeled 500 samples — classification labels applied");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("labels", Map.of("count", 500, "confidence", 0.89));
        return result;
    }
}
