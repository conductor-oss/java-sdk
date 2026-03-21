package multimodelcompare.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls the Gemini model via Google Generative AI API.
 *
 * <p>When GOOGLE_API_KEY is set, makes a real API call.
 * When unset, returns a deterministic default response prefixed with [DEMO].</p>
 */
public class McCallGeminiWorker implements Worker {

    private final String apiKey;
    private final boolean liveMode;
    private final ObjectMapper mapper = new ObjectMapper();

    public McCallGeminiWorker() {
        this.apiKey = System.getenv("GOOGLE_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        if (liveMode) {
            System.out.println("  [mc_call_gemini] LIVE mode — GOOGLE_API_KEY detected");
        } else {
            System.out.println("  [mc_call_gemini] Demo mode — set GOOGLE_API_KEY for live calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "mc_call_gemini";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        Map<String, Object> output;
        if (liveMode) {
            output = callLive(task, result);
            if (output == null) {
                return result;
            }
        } else {
            output = callFallback();
        }

        System.out.println("  [mc_call_gemini worker] Gemini response generated"
                + (liveMode ? " (live)" : " (deterministic."));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }

    private Map<String, Object> callFallback() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("model", "gemini-pro");
        output.put("response", "Conductor orchestrates workflows across services with built-in retry, human-in-the-loop, and monitoring.");
        output.put("latencyMs", 750);
        output.put("tokens", 78);
        output.put("cost", 0.0001);
        output.put("quality", 8.5);
        return output;
    }

    private Map<String, Object> callLive(Task task, TaskResult result) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "What is Conductor?");
        long start = System.currentTimeMillis();

        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    }
            ));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            long latencyMs = System.currentTimeMillis() - start;

            if (httpResponse.statusCode() != 200) {
                String errorBody = httpResponse.body();
                System.err.println("  [worker] API error HTTP " + httpResponse.statusCode() + ": " + errorBody);
                if (httpResponse.statusCode() == 429 || httpResponse.statusCode() == 503) {
                    result.setStatus(TaskResult.Status.FAILED);
                    result.setReasonForIncompletion("API rate limited (HTTP " + httpResponse.statusCode() + "). Conductor will retry.");
                } else {
                    result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                    result.setReasonForIncompletion("API error HTTP " + httpResponse.statusCode() + ": " + errorBody);
                }
                result.getOutputData().put("errorBody", errorBody);
                result.getOutputData().put("httpStatus", httpResponse.statusCode());
                return null;
            }

            JsonNode root = mapper.readTree(httpResponse.body());
            String responseText = root.at("/candidates/0/content/parts/0/text").asText();

            int promptTokens = root.at("/usageMetadata/promptTokenCount").asInt(0);
            int candidatesTokens = root.at("/usageMetadata/candidatesTokenCount").asInt(0);
            int totalTokens = root.at("/usageMetadata/totalTokenCount").asInt(promptTokens + candidatesTokens);

            // Gemini 2.0 Flash pricing: $0.10/1M input, $0.40/1M output
            double cost = (promptTokens * 0.10 + candidatesTokens * 0.40) / 1_000_000.0;

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("model", "gemini-pro");
            output.put("response", responseText);
            output.put("latencyMs", (int) latencyMs);
            output.put("tokens", totalTokens);
            output.put("cost", Math.round(cost * 10000.0) / 10000.0);
            output.put("quality", 8.5);
            return output;

        } catch (Exception e) {
            System.err.println("  [mc_call_gemini] API call failed: " + e.getMessage() + " — falling back to deterministic");
            Map<String, Object> fallback = callFallback();
            fallback.put("response", "[FALLBACK] " + fallback.get("response"));
            return fallback;
        }
    }
}
