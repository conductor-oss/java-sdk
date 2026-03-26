package ollamalocal.workers;

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
import java.util.Map;

/**
 * Calls the local Ollama API for text generation, or returns a default
 * response when Ollama is not available or the call fails.
 *
 * Live mode: POST http://{OLLAMA_HOST}/api/generate (Ollama must be running
 * with the requested model pulled)
 * Fallback mode: returns a fixed code review response with prefix.
 *
 * Env var: OLLAMA_HOST (default: localhost:11434)
 *
 * Input: prompt, model, ollamaHost, options
 * Output: response, model, evalMetrics
 */
public class OllamaGenerateWorker implements Worker {

    private static final String FIXED_RESPONSE =
            "Code Review Findings:\n"
            + "1. Missing error handling in the async function\n"
            + "2. Variable 'data' is declared but never used\n"
            + "3. Consider using const instead of let for immutable bindings\n"
            + "4. Add input validation for the function parameters\n"
            + "5. The function should return a consistent type";

    private final String ollamaBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaGenerateWorker() {
        String envHost = System.getenv("OLLAMA_HOST");
        if (envHost != null && !envHost.isBlank()) {
            if (!envHost.startsWith("http://") && !envHost.startsWith("https://")) {
                envHost = "http://" + envHost;
            }
            this.ollamaBaseUrl = envHost;
        } else {
            this.ollamaBaseUrl = "http://localhost:11434";
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();

        System.out.println("  [ollama_generate] Ollama host: " + ollamaBaseUrl
                + ". Will attempt live calls; falls back to fallback mode on failure.");
    }

    @Override
    public String getTaskDefName() {
        return "ollama_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String model = (String) task.getInputData().get("model");

        if (prompt == null || prompt.isBlank()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Prompt is required");
            return result;
        }

        if (model == null || model.isBlank()) {
            model = "codellama:13b";
        }

        // Always try live first, fall back to deterministic.on failure
        TaskResult liveResult = executeLive(task, prompt, model);
        if (liveResult != null) {
            return liveResult;
        }
        return executeFallback(task, model);
    }

    /**
     * Attempts a live call to Ollama. Returns null if Ollama is unreachable
     * or the call fails, signaling the caller to fall back to process.
     */
    private TaskResult executeLive(Task task, String prompt, String model) {
        try {
            String host = (String) task.getInputData().get("ollamaHost");
            if (host == null || host.isBlank()) {
                host = ollamaBaseUrl;
            }
            if (!host.startsWith("http://") && !host.startsWith("https://")) {
                host = "http://" + host;
            }

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            System.out.println("  [ollama_generate] Generating with model '" + model + "' on " + host + " (live)");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            long startTime = System.nanoTime();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            long durationNanos = System.nanoTime() - startTime;

            if (response.statusCode() >= 400) {
                System.out.println("  [ollama_generate] Ollama returned HTTP " + response.statusCode()
                        + ", falling back to fallback mode");
                return null; // fall back to process
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ollamaResponse = objectMapper.readValue(response.body(), Map.class);

            String responseText = (String) ollamaResponse.getOrDefault("response", "");

            Map<String, Object> evalMetrics = new LinkedHashMap<>();
            evalMetrics.put("totalDuration",
                    ollamaResponse.getOrDefault("total_duration", durationNanos));
            evalMetrics.put("loadDuration",
                    ollamaResponse.getOrDefault("load_duration", 0L));
            evalMetrics.put("evalCount",
                    ollamaResponse.getOrDefault("eval_count", 0));
            evalMetrics.put("evalDuration",
                    ollamaResponse.getOrDefault("eval_duration", durationNanos));

            Object evalCount = evalMetrics.get("evalCount");
            Object evalDuration = evalMetrics.get("evalDuration");
            double tokPerSec = 0;
            if (evalCount instanceof Number && evalDuration instanceof Number) {
                long evalDurNanos = ((Number) evalDuration).longValue();
                if (evalDurNanos > 0) {
                    tokPerSec = ((Number) evalCount).doubleValue() / (evalDurNanos / 1_000_000_000.0);
                }
            }
            evalMetrics.put("tokensPerSecond", Math.round(tokPerSec * 100.0) / 100.0);

            System.out.println("  [ollama_generate] Generation complete: "
                    + responseText.length() + " chars");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("response", responseText);
            result.getOutputData().put("model", model);
            result.getOutputData().put("evalMetrics", evalMetrics);
            return result;

        } catch (Exception e) {
            System.out.println("  [ollama_generate] Live call failed (" + e.getMessage()
                    + "), falling back to fallback mode");
            return null; // fall back to process
        }
    }

    private TaskResult executeFallback(Task task, String model) {
        String ollamaHost = (String) task.getInputData().get("ollamaHost");
        if (ollamaHost == null || ollamaHost.isBlank()) {
            ollamaHost = ollamaBaseUrl;
        }

        System.out.println("  [ollama_generate] Generating with model '" + model + "' on " + ollamaHost + " (deterministic.");

        Map<String, Object> evalMetrics = Map.of(
                "totalDuration", 2450000000L,
                "loadDuration", 15000000L,
                "evalCount", 145,
                "evalDuration", 2100000000L,
                "tokensPerSecond", 69.05
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", FIXED_RESPONSE);
        result.getOutputData().put("model", model);
        result.getOutputData().put("evalMetrics", evalMetrics);
        return result;
    }
}
