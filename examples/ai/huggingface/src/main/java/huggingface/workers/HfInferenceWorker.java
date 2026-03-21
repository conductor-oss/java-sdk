package huggingface.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the Hugging Face Inference API, or returns a default response when
 * HUGGINGFACE_TOKEN is not set.
 *
 * Live mode: POST https://api-inference.huggingface.co/models/{modelId}
 * Fallback mode: returns a fixed summarization response.
 *
 * Input: modelId, inputs (text to process)
 * Output: rawOutput
 */
public class HfInferenceWorker implements Worker {

    private static final String HF_API_BASE = "https://api-inference.huggingface.co/models/";

    private final boolean liveMode;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HfInferenceWorker() {
        this.token = System.getenv("HUGGINGFACE_TOKEN");
        this.liveMode = token != null && !token.isBlank();
        this.httpClient = liveMode ? HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build() : null;
        this.objectMapper = new ObjectMapper();

        if (liveMode) {
            System.out.println("  [hf_inference] Live mode: HUGGINGFACE_TOKEN detected");
        } else {
            System.out.println("  [hf_inference] Fallback mode: set HUGGINGFACE_TOKEN for live API calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "hf_inference";
    }

    @Override
    public TaskResult execute(Task task) {
        if (liveMode) {
            return executeLive(task);
        }
        return executeFallback(task);
    }

    private TaskResult executeLive(Task task) {
        try {
            String modelId = (String) task.getInputData().get("modelId");
            if (modelId == null || modelId.isBlank()) {
                modelId = "facebook/bart-large-cnn";
            }

            Object inputs = task.getInputData().get("inputs");
            if (inputs == null) {
                inputs = "No input provided";
            }

            String jsonBody = objectMapper.writeValueAsString(Map.of("inputs", inputs));

            System.out.println("  [infer] Calling HF Inference API (live): " + modelId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HF_API_BASE + modelId))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error",
                        "HuggingFace API returned HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }

            Object rawOutput = objectMapper.readValue(response.body(), Object.class);

            System.out.println("  [infer] Inference complete");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("rawOutput", rawOutput);
            return result;

        } catch (Exception e) {
            System.err.println("  [infer] API call failed: " + e.getMessage());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "HuggingFace API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeFallback(Task task) {
        String modelId = (String) task.getInputData().get("modelId");

        System.out.println("  [infer] Calling HF Inference API (deterministic.: " + modelId);

        // deterministic summarization response
        List<Map<String, String>> rawOutput = List.of(
                Map.of("summary_text",
                        "A comprehensive study of 2,500 remote workers across 12 countries "
                        + "found that hybrid work models (3 days office, 2 days remote) "
                        + "produced the highest productivity scores, outperforming both "
                        + "fully remote and fully in-office arrangements by 18% and 23% "
                        + "respectively. Key factors included reduced commute stress, "
                        + "better work-life balance, and maintained team cohesion through "
                        + "regular in-person collaboration.")
        );

        System.out.println("  [infer] Inference complete");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rawOutput", rawOutput);
        return result;
    }
}
