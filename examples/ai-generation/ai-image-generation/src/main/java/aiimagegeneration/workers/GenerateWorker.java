package aiimagegeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GenerateWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean liveMode;
    private final String apiKey;

    public GenerateWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getTaskDefName() {
        return "aig_generate";
    }

    @Override
    public TaskResult execute(Task task) {

        String processedPrompt = (String) task.getInputData().get("processedPrompt");
        String resolution = (String) task.getInputData().get("resolution");

        TaskResult result = new TaskResult(task);

        if (liveMode) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String body = """
                    {"model": "dall-e-3", "prompt": "%s", "size": "1024x1024", "n": 1, "response_format": "url"}
                    """.formatted(processedPrompt.replace("\\", "\\\\").replace("\"", "\\\""));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/images/generations"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.printf("  [generate] DALL-E API error (HTTP %d): %s%n",
                            response.statusCode(), response.body());
                    result.setStatus(TaskResult.Status.FAILED);
                    result.getOutputData().put("error", "DALL-E API returned HTTP " + response.statusCode());
                    result.getOutputData().put("responseBody", response.body());
                    return result;
                }

                JsonNode root = MAPPER.readTree(response.body());
                JsonNode data = root.get("data").get(0);
                String imageUrl = data.get("url").asText();
                String revisedPrompt = data.has("revised_prompt")
                        ? data.get("revised_prompt").asText()
                        : processedPrompt;

                System.out.printf("  [generate] DALL-E 3 image generated at 1024x1024 resolution%n");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("imageId", "IMG-dalle3-" + System.currentTimeMillis());
                result.getOutputData().put("imageUrl", imageUrl);
                result.getOutputData().put("revisedPrompt", revisedPrompt);
                result.getOutputData().put("width", 1024);
                result.getOutputData().put("height", 1024);
            } catch (Exception e) {
                System.err.printf("  [generate] DALL-E API call failed: %s%n", e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", e.getMessage());
            }
        } else {
            System.out.println("  [generate] Image generated at 1024x1024 resolution");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("imageId", "IMG-ai-image-generation-001");
            result.getOutputData().put("imageUrl", "https://example.com/deterministic.image.png");
            result.getOutputData().put("revisedPrompt", "" + processedPrompt);
            result.getOutputData().put("width", 1024);
            result.getOutputData().put("height", 1024);
        }

        return result;
    }
}
