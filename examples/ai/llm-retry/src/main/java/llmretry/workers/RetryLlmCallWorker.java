package llmretry.workers;

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
 * LLM API call that fails with 429 rate-limit errors on the first two
 * attempts, then succeeds on the third.
 *
 * <p>When CONDUCTOR_OPENAI_API_KEY is set, the successful attempt (3rd+) calls the
 * OpenAI Chat Completions API. Otherwise runs in fallback mode with
 * deterministic output.</p>
 *
 * <p>Conductor's retry configuration (EXPONENTIAL_BACKOFF) handles
 * re-scheduling the task automatically.</p>
 */
public class RetryLlmCallWorker implements Worker {

    private static final ConcurrentHashMap<String, Integer> attemptTracker = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;

    public RetryLlmCallWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "retry_llm_call";
    }

    @Override
    public TaskResult execute(Task task) {
        String wfId = task.getWorkflowInstanceId();
        int attempt = attemptTracker.merge(wfId, 1, Integer::sum);

        TaskResult result = new TaskResult(task);

        if (attempt <= 2) {
            System.out.println("  [LLM] Attempt " + attempt + ": HTTP 429 - Rate limited, failing task for retry");
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("429 Too Many Requests (attempt " + attempt + ")");
            result.getOutputData().put("error", "rate_limited");
            result.getOutputData().put("attempt", attempt);
        } else {
            System.out.println("  [LLM] Attempt " + attempt + ": Success! Model responded");

            if (liveMode) {
                try {
                    String prompt = (String) task.getInputData().get("prompt");
                    if (prompt == null) prompt = "Explain how Conductor handles retries for AI pipelines.";

                    Map<String, Object> requestBody = Map.of(
                            "model", "gpt-4o-mini",
                            "messages", List.of(
                                    Map.of("role", "user", "content", prompt)
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

                    if (response.statusCode() != 200) {
                        result.setStatus(TaskResult.Status.FAILED);
                        result.getOutputData().put("error",
                                "OpenAI API error " + response.statusCode() + ": " + response.body());
                        return result;
                    }

                    JsonNode root = MAPPER.readTree(response.body());
                    String llmResponse = root.at("/choices/0/message/content").asText();

                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("response", llmResponse);
                    result.getOutputData().put("attempts", attempt);
                    result.getOutputData().put("model", "gpt-4o-mini");
                } catch (Exception e) {
                    result.setStatus(TaskResult.Status.FAILED);
                    result.getOutputData().put("error", "OpenAI call failed: " + e.getMessage());
                }
            } else {
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("response",
                        "Conductor handles retries so your AI pipeline never drops requests.");
                result.getOutputData().put("attempts", attempt);
                result.getOutputData().put("model", task.getInputData().get("model"));
            }
        }

        return result;
    }

    /** Clears the attempt tracker. Useful for testing. */
    public static void resetAttempts() {
        attemptTracker.clear();
    }
}
