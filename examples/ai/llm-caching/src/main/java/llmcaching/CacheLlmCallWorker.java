package llmcaching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker 2: cache_llm_call
 * Uses a static ConcurrentHashMap as cache. On cache hit returns stored response
 * with cacheHit:true, latencyMs:0. On cache miss calls OpenAI (live mode) or
 * returns a default response (fallback mode), stores it, and reports latency.
 *
 * <p>When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI Chat Completions API on cache miss.
 * Otherwise runs in fallback mode with deterministic output.</p>
 */
public class CacheLlmCallWorker implements Worker {

    static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    static final String FIXED_RESPONSE =
            "Conductor provides durable workflow execution, making it ideal for "
            + "orchestrating LLM calls with built-in retry, timeout, and observability.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;

    public CacheLlmCallWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "cache_llm_call";
    }

    @Override
    public TaskResult execute(Task task) {
        String cacheKey = (String) task.getInputData().get("cacheKey");
        String prompt = (String) task.getInputData().get("prompt");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        String cached = CACHE.get(cacheKey);
        if (cached != null) {
            result.addOutputData("response", cached);
            result.addOutputData("cacheHit", true);
            result.addOutputData("latencyMs", 0);
        } else {
            if (liveMode) {
                try {
                    long start = System.currentTimeMillis();

                    Map<String, Object> requestBody = Map.of(
                            "model", "gpt-4o-mini",
                            "messages", List.of(
                                    Map.of("role", "user", "content",
                                            prompt != null ? prompt : cacheKey)
                            )
                    );

                    String body = MAPPER.writeValueAsString(requestBody);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();

                    HttpResponse<String> response = httpClient.send(request,
                            HttpResponse.BodyHandlers.ofString());

                    long latency = System.currentTimeMillis() - start;

                    if (response.statusCode() != 200) {
                        result.setStatus(TaskResult.Status.FAILED);
                        result.addOutputData("error",
                                "OpenAI API error " + response.statusCode() + ": " + response.body());
                        return result;
                    }

                    JsonNode root = MAPPER.readTree(response.body());
                    String llmResponse = root.at("/choices/0/message/content").asText();

                    CACHE.put(cacheKey, llmResponse);
                    result.addOutputData("response", llmResponse);
                    result.addOutputData("cacheHit", false);
                    result.addOutputData("latencyMs", latency);
                } catch (Exception e) {
                    result.setStatus(TaskResult.Status.FAILED);
                    result.addOutputData("error", "OpenAI call failed: " + e.getMessage());
                }
            } else {
                CACHE.put(cacheKey, FIXED_RESPONSE);
                result.addOutputData("response", FIXED_RESPONSE);
                result.addOutputData("cacheHit", false);
                result.addOutputData("latencyMs", 850);
            }
        }

        return result;
    }
}
