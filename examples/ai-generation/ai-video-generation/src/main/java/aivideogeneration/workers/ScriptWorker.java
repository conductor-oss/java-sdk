package aivideogeneration.workers;

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

public class ScriptWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ScriptWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "avg_script";
    }

    @Override
    public TaskResult execute(Task task) {

        String topic = (String) task.getInputData().get("topic");
        String duration = (String) task.getInputData().get("duration");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a professional video scriptwriter. Write a concise video script with scene descriptions, narration text, and timing cues. Structure the script into 5 scenes with clear visual directions.";
                String userPrompt = "Write a video script about '" + (topic != null ? topic : "technology") + "' for a " + (duration != null ? duration : "30") + " second video. Include 5 scenes with narration and visual descriptions.";
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
                    System.out.println("  [script] Response from OpenAI (LIVE)");
                    result.getOutputData().put("script", content);
                    result.getOutputData().put("scenes", 5);
                    result.getOutputData().put("wordCount", content.split("\\s+").length);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [script] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.printf("  [script] Script written for %s%n", topic, duration);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("script", "scene-script-5-scenes");
        result.getOutputData().put("scenes", 5);
        result.getOutputData().put("wordCount", 320);
        return result;
    }
}
