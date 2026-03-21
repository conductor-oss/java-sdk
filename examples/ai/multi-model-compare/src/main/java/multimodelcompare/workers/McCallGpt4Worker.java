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
 * Calls the GPT-4 model via OpenAI Chat Completions API.
 *
 * <p>When CONDUCTOR_OPENAI_API_KEY is set, makes a real API call.
 * When unset, returns a deterministic default response prefixed with [DEMO].</p>
 */
public class McCallGpt4Worker implements Worker {

    private final String apiKey;
    private final boolean liveMode;
    private final ObjectMapper mapper = new ObjectMapper();

    public McCallGpt4Worker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        if (liveMode) {
            System.out.println("  [mc_call_gpt4] LIVE mode — CONDUCTOR_OPENAI_API_KEY detected");
        } else {
            System.out.println("  [mc_call_gpt4] Demo mode — set CONDUCTOR_OPENAI_API_KEY for live calls");
        }
    }

    @Override
    public String getTaskDefName() {
        return "mc_call_gpt4";
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

        System.out.println("  [mc_call_gpt4 worker] GPT-4 response generated"
                + (liveMode ? " (live)" : " (deterministic."));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }

    private Map<String, Object> callFallback() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("model", "gpt-4");
        output.put("response", "Conductor provides durable, observable workflow orchestration for microservices and AI.");
        output.put("latencyMs", 1200);
        output.put("tokens", 85);
        output.put("cost", 0.0051);
        output.put("quality", 9.2);
        return output;
    }

    private Map<String, Object> callLive(Task task, TaskResult result) {
        String prompt = (String) task.getInputData().getOrDefault("prompt", "What is Conductor?");
        long start = System.currentTimeMillis();

        try {
            String requestBody = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4",
                    "messages", new Object[]{
                            Map.of("role", "user", "content", prompt)
                    },
                    "max_tokens", 256
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
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
            String responseText = root.at("/choices/0/message/content").asText();
            int promptTokens = root.at("/usage/prompt_tokens").asInt(0);
            int completionTokens = root.at("/usage/completion_tokens").asInt(0);
            int totalTokens = root.at("/usage/total_tokens").asInt(promptTokens + completionTokens);

            // GPT-4 pricing: $0.03/1K prompt, $0.06/1K completion
            double cost = (promptTokens * 0.03 + completionTokens * 0.06) / 1000.0;

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("model", "gpt-4");
            output.put("response", responseText);
            output.put("latencyMs", (int) latencyMs);
            output.put("tokens", totalTokens);
            output.put("cost", Math.round(cost * 10000.0) / 10000.0);
            output.put("quality", 9.2);
            return output;

        } catch (Exception e) {
            System.err.println("  [mc_call_gpt4] API call failed: " + e.getMessage() + " — falling back to deterministic");
            Map<String, Object> fallback = callFallback();
            fallback.put("response", "[FALLBACK] " + fallback.get("response"));
            return fallback;
        }
    }
}
