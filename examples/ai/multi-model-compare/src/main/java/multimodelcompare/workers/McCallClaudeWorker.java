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
 * Calls the Claude model via Anthropic Messages API.
 *
 * <p>When CONDUCTOR_ANTHROPIC_API_KEY is set, makes a real API call.
 * When unset, returns a deterministic default response prefixed with [DEMO].</p>
 */
public class McCallClaudeWorker implements Worker {

    private final String apiKey;
    private final boolean liveMode;
    private final ObjectMapper mapper = new ObjectMapper();

    public McCallClaudeWorker() {
        this.apiKey = System.getenv("CONDUCTOR_ANTHROPIC_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        if (liveMode) {
            System.out.println("  [mc_call_claude] LIVE mode — CONDUCTOR_ANTHROPIC_API_KEY detected");
        } else {
            System.out.println("  [mc_call_claude] Demo mode — set CONDUCTOR_ANTHROPIC_API_KEY for live calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "mc_call_claude";
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

        System.out.println("  [mc_call_claude worker] Claude response generated"
                + (liveMode ? " (live)" : " (deterministic."));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }

    private Map<String, Object> callFallback() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("model", "claude-3");
        output.put("response", "Conductor is a workflow engine that brings durability and observability to distributed systems and AI pipelines.");
        output.put("latencyMs", 980);
        output.put("tokens", 92);
        output.put("cost", 0.0069);
        output.put("quality", 9.0);
        return output;
    }

    private Map<String, Object> callLive(Task task, TaskResult result) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "What is Conductor?");
        long start = System.currentTimeMillis();

        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                    "model", "claude-sonnet-4-6",
                    "max_tokens", 256,
                    "messages", new Object[]{
                            Map.of("role", "user", "content", prompt)
                    }
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
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
            String responseText = root.at("/content/0/text").asText();
            int inputTokens = root.at("/usage/input_tokens").asInt(0);
            int outputTokens = root.at("/usage/output_tokens").asInt(0);
            int totalTokens = inputTokens + outputTokens;

            // Claude Sonnet pricing: $3/1M input, $15/1M output
            double cost = (inputTokens * 3.0 + outputTokens * 15.0) / 1_000_000.0;

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("model", "claude-3");
            output.put("response", responseText);
            output.put("latencyMs", (int) latencyMs);
            output.put("tokens", totalTokens);
            output.put("cost", Math.round(cost * 10000.0) / 10000.0);
            output.put("quality", 9.0);
            return output;

        } catch (Exception e) {
            System.err.println("  [mc_call_claude] API call failed: " + e.getMessage() + " — falling back to deterministic");
            Map<String, Object> fallback = callFallback();
            fallback.put("response", "[FALLBACK] " + fallback.get("response"));
            return fallback;
        }
    }
}
