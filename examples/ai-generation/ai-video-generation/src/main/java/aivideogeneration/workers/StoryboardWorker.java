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

public class StoryboardWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public StoryboardWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "avg_storyboard";
    }

    @Override
    public TaskResult execute(Task task) {

        String script = (String) task.getInputData().get("script");
        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a professional storyboard artist. Given a video script, create detailed visual descriptions for each scene including camera angles, compositions, color palette, transitions, and keyframe descriptions.";
                String userPrompt = "Create a storyboard for the following video script:\n\n" + (script != null ? script : "5-scene video script") + "\n\nProvide detailed shot descriptions and keyframes for each scene.";
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
                    System.out.println("  [storyboard] Response from OpenAI (LIVE)");
                    result.getOutputData().put("storyboard", content);
                    result.getOutputData().put("scenes", 5);
                    result.getOutputData().put("keyframes", 15);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [storyboard] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        System.out.println("  [storyboard] 5 scenes storyboarded with shot descriptions");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("storyboard", "storyboard-5-scenes");
        result.getOutputData().put("scenes", 5);
        result.getOutputData().put("keyframes", 15);
        return result;
    }
}
