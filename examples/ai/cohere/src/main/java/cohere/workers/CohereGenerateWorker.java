package cohere.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the Cohere Generate API, or returns a default response when
 * COHERE_API_KEY is not set.
 *
 * Live mode: POST https://api.cohere.ai/v1/generate
 * Fallback mode: returns a default response with 3 generations.
 *
 * Input: requestBody (from cohere_build_prompt)
 * Output: { apiResponse }
 */
public class CohereGenerateWorker implements Worker {

    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/generate";

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CohereGenerateWorker() {
        this.apiKey = System.getenv("COHERE_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = liveMode ? HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build() : null;
        this.objectMapper = new ObjectMapper();

        if (liveMode) {
            System.out.println("  [cohere_generate] Live mode: COHERE_API_KEY detected");
        } else {
            System.out.println("  [cohere_generate] Fallback mode: set COHERE_API_KEY for live API calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "cohere_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        if (liveMode) {
            return executeLive(task);
        }
        return executeFallback(task);
    }

    private TaskResult executeLive(Task task) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) task.getInputData().get("requestBody");
            if (requestBody == null) {
                requestBody = new LinkedHashMap<>();
                requestBody.put("prompt", "Write a compelling marketing copy.");
                requestBody.put("model", "command");
                requestBody.put("max_tokens", 300);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            System.out.println("  [cohere_generate worker] Calling Cohere API (live)...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COHERE_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error",
                        "Cohere API returned HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> apiResponse = objectMapper.readValue(response.body(), Map.class);

            System.out.println("  [cohere_generate worker] Cohere API call completed successfully");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("apiResponse", apiResponse);
            return result;

        } catch (Exception e) {
            System.err.println("  [cohere_generate worker] API call failed: " + e.getMessage());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Cohere API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeFallback(Task task) {
        System.out.println("  [cohere_generate worker] Performing Cohere API call...");

        Map<String, Object> gen0 = new LinkedHashMap<>();
        gen0.put("text", "Transform your workflow with SmartBoard Pro. "
                + "The AI-powered project management tool that learns how your team works "
                + "and adapts in real-time. Cut meeting time by 40%. Ship 2x faster. "
                + "Start your free trial today.");
        gen0.put("finish_reason", "COMPLETE");
        gen0.put("likelihood", -1.82);

        Map<String, Object> gen1 = new LinkedHashMap<>();
        gen1.put("text", "Your team deserves better than spreadsheets and sticky notes. "
                + "SmartBoard Pro brings AI-driven insights to every sprint, automatically "
                + "identifying bottlenecks before they slow you down. Join 5,000+ engineering "
                + "teams who've already made the switch.");
        gen1.put("finish_reason", "COMPLETE");
        gen1.put("likelihood", -1.65);

        Map<String, Object> gen2 = new LinkedHashMap<>();
        gen2.put("text", "Stop managing projects. Start leading them. SmartBoard Pro's AI engine "
                + "handles task prioritization, risk detection, and resource allocation "
                + "— so you can focus on building great products. Free for teams under 10.");
        gen2.put("finish_reason", "COMPLETE");
        gen2.put("likelihood", -1.91);

        List<Map<String, Object>> generations = List.of(gen0, gen1, gen2);

        Map<String, Object> apiVersion = new LinkedHashMap<>();
        apiVersion.put("version", "1");

        Map<String, Object> billedUnits = new LinkedHashMap<>();
        billedUnits.put("input_tokens", 42);
        billedUnits.put("output_tokens", 185);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("api_version", apiVersion);
        meta.put("billed_units", billedUnits);

        Map<String, Object> apiResponse = new LinkedHashMap<>();
        apiResponse.put("generations", generations);
        apiResponse.put("meta", meta);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("apiResponse", apiResponse);
        return result;
    }
}
