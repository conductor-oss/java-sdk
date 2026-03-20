package systemprompts.workers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls an LLM. Returns a response based on the style.
 *
 * <p>When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI Chat Completions API using the
 * system prompt, few-shot examples, and user message from fullPrompt.
 * Otherwise runs in fallback mode with deterministic output.</p>
 */
public class SpCallLlmWorker implements Worker {

    private static final Map<String, String> RESPONSES = Map.of(
            "formal", "Conductor is a distributed workflow orchestration engine that provides durable execution semantics, comprehensive observability, and declarative workflow definitions for enterprise-grade microservice coordination.",
            "casual", "Conductor is basically a traffic controller for your code — it makes sure all your services talk to each other in the right order, and if something crashes, it picks up right where it left off."
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final boolean liveMode;
    private final String apiKey;
    private final HttpClient httpClient;

    public SpCallLlmWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.liveMode = apiKey != null && !apiKey.isBlank();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "sp_call_llm";
    }

    @Override
    public TaskResult execute(Task task) {
        String style = (String) task.getInputData().get("style");
        String fullPrompt = (String) task.getInputData().get("fullPrompt");

        if (style == null || style.isBlank()) {
            style = "formal";
        }

        TaskResult result = new TaskResult(task);

        if (liveMode) {
            try {
                List<Map<String, String>> messages = new ArrayList<>();

                // Parse fullPrompt JSON to extract system, fewShot, user
                if (fullPrompt != null && !fullPrompt.isBlank()) {
                    JsonNode promptData = MAPPER.readTree(fullPrompt);

                    if (promptData.has("system")) {
                        messages.add(Map.of("role", "system", "content",
                                promptData.get("system").asText()));
                    }

                    if (promptData.has("fewShot")) {
                        for (JsonNode example : promptData.get("fewShot")) {
                            messages.add(Map.of(
                                    "role", example.get("role").asText(),
                                    "content", example.get("content").asText()));
                        }
                    }

                    if (promptData.has("user")) {
                        messages.add(Map.of("role", "user", "content",
                                promptData.get("user").asText()));
                    }
                } else {
                    messages.add(Map.of("role", "user", "content",
                            "Explain Conductor in a " + style + " style."));
                }

                Map<String, Object> requestBody = Map.of(
                        "model", "gpt-4o-mini",
                        "messages", messages
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

                System.out.println("  [sp_call_llm] Generated " + style + " response (" + llmResponse.length() + " chars)");

                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("response", llmResponse);
                result.getOutputData().put("style", style);
            } catch (Exception e) {
                result.setStatus(TaskResult.Status.FAILED);
                result.getOutputData().put("error", "OpenAI call failed: " + e.getMessage());
            }
        } else {
            String response = RESPONSES.getOrDefault(style, RESPONSES.get("formal"));

            System.out.println("  [sp_call_llm] Generated " + style + " response (" + response.length() + " chars)");

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("response", response);
            result.getOutputData().put("style", style);
        }
        return result;
    }
}
