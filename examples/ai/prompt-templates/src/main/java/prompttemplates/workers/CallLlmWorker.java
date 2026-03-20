package prompttemplates.workers;

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

/**
 * Calls an LLM with a prompt.
 *
 * <p>When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI Chat Completions API.
 * Otherwise runs in fallback mode with deterministic output.</p>
 */
public class CallLlmWorker implements Worker {

    private static final String FIXED_RESPONSE =
            "Conductor is a workflow orchestration platform that enables durable execution "
            + "of microservices and AI pipelines with built-in observability and retry mechanisms.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;

    public CallLlmWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "pt_call_llm";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String model = (String) task.getInputData().get("model");

        System.out.println("  [pt_call_llm] Calling " + model + " with prompt (" + prompt.length() + " chars)");

        TaskResult result = new TaskResult(task);

        if (liveMode) {
            try {
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
                int totalTokens = root.at("/usage/total_tokens").asInt();

                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("response", llmResponse);
                result.getOutputData().put("tokens", totalTokens);
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "OpenAI call failed: " + e.getMessage());
            }
        } else {
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("response", FIXED_RESPONSE);
            result.getOutputData().put("tokens", 42);
        }
        return result;
    }
}
