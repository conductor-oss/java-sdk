package streamingllm.workers;

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
 * Collects streamed chunks from an LLM (OpenAI streaming API) and assembles
 * them into a full response, or returns a default response when
 * CONDUCTOR_OPENAI_API_KEY is not set.
 *
 * Live mode: POST https://api.openai.com/v1/chat/completions with stream=true,
 * collects SSE chunks and assembles the full response.
 * Fallback mode: returns a fixed chunked response with prefix.
 *
 * Input: prompt, model, maxTokens
 * Output: fullResponse, chunkCount, streamDurationMs, tokensPerSecond
 */
public class StreamCollectChunksWorker implements Worker {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private static final List<String> DEFAULT_CHUNKS = List.of(
            "Conductor ",
            "orchestrates ",
            "complex workflows ",
            "with built-in ",
            "durability, ",
            "retry logic, ",
            "and full ",
            "observability \u2014 ",
            "making it ideal ",
            "for production ",
            "AI pipelines."
    );

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StreamCollectChunksWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = liveMode ? HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build() : null;
        this.objectMapper = new ObjectMapper();

        if (liveMode) {
            System.out.println("  [stream_collect_chunks] Live mode: CONDUCTOR_OPENAI_API_KEY detected");
        } else {
            System.out.println("  [stream_collect_chunks] Fallback mode: set CONDUCTOR_OPENAI_API_KEY for live streaming");
        }
    }

    @Override
    public String getTaskDefName() {
        return "stream_collect_chunks";
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
            String prompt = (String) task.getInputData().get("prompt");
            String model = (String) task.getInputData().get("model");
            Object maxTokens = task.getInputData().get("maxTokens");

            if (model == null || model.isBlank()) {
                model = "gpt-4";
            }
            if (maxTokens == null) {
                maxTokens = 200;
            }

            System.out.println("  [stream] Opening stream to " + model + " (live)...");

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt != null ? prompt : "")));
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", true);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            long durationMs = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 400) {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error",
                        "OpenAI API returned HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }

            // Parse SSE chunks from the response body
            String body = response.body();
            StringBuilder assembled = new StringBuilder();
            int chunkCount = 0;

            for (String line : body.split("\n")) {
                line = line.trim();
                if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                    String jsonData = line.substring(6);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> chunk = objectMapper.readValue(jsonData, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> choices =
                                (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> delta =
                                    (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = (String) delta.get("content");
                                assembled.append(content);
                                chunkCount++;
                                System.out.println("  [stream] Chunk " + chunkCount
                                        + ": \"" + content.trim() + "\"");
                            }
                        }
                    } catch (Exception ignored) {
                        // Skip malformed chunks
                    }
                }
            }

            String fullResponse = assembled.toString().trim();
            double tokensPerSecond = durationMs > 0 ? (chunkCount * 1000.0 / durationMs) : 0;

            System.out.println("  [stream] Stream complete: " + chunkCount + " chunks assembled");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("fullResponse", fullResponse);
            result.getOutputData().put("chunkCount", chunkCount);
            result.getOutputData().put("streamDurationMs", (int) durationMs);
            result.getOutputData().put("tokensPerSecond", (int) Math.round(tokensPerSecond));
            return result;

        } catch (Exception e) {
            System.err.println("  [stream] Streaming API call failed: " + e.getMessage());
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "OpenAI streaming API call failed: " + e.getMessage());
            return result;
        }
    }

    private TaskResult executeFallback(Task task) {
        System.out.println("  [stream] Opening stream to " + task.getInputData().get("model") + "...");

        StringBuilder assembled = new StringBuilder();
        for (int i = 0; i < DEFAULT_CHUNKS.size(); i++) {
            assembled.append(DEFAULT_CHUNKS.get(i));
            System.out.println("  [stream] Chunk " + (i + 1) + "/" + DEFAULT_CHUNKS.size()
                    + ": \"" + DEFAULT_CHUNKS.get(i).trim() + "\"");
        }

        System.out.println("  [stream] Stream complete: " + DEFAULT_CHUNKS.size() + " chunks assembled");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fullResponse", assembled.toString().trim());
        result.getOutputData().put("chunkCount", DEFAULT_CHUNKS.size());
        result.getOutputData().put("streamDurationMs", 1240);
        result.getOutputData().put("tokensPerSecond", 45);
        return result;
    }
}
