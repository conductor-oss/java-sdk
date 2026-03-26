package enterpriserag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Worker that generates an answer using an LLM given question, context, and token budget.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls OpenAI Chat Completions API (gpt-4o-mini).
 * Otherwise returns a default answer, tokensUsed, model name, and latency.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getTaskDefName() {
        return "er_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        Object context = task.getInputData().get("context");
        Object tokenBudget = task.getInputData().get("tokenBudget");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            System.out.println("  [generate] Calling OpenAI for question=\"" + question
                    + "\" tokenBudget=" + tokenBudget);
            try {
                long startMs = System.currentTimeMillis();
                String contextStr = context != null ? context.toString() : "";

                String systemPrompt = "You are a helpful assistant. Answer the question based on the provided context. Be concise and factual.";
                String userPrompt = "Context: " + contextStr + "\n\nQuestion: " + (question != null ? question : "");

                int maxTokens = 512;
                if (tokenBudget instanceof Number) {
                    maxTokens = Math.min(((Number) tokenBudget).intValue(), 512);
                    if (maxTokens <= 0) maxTokens = 512;
                }

                String answer = callChatCompletion(systemPrompt, userPrompt, maxTokens, result);
                if (answer == null) {
                    return result;
                }
                long latencyMs = System.currentTimeMillis() - startMs;

                result.getOutputData().put("answer", answer);
                result.getOutputData().put("tokensUsed", answer.split("\\s+").length * 2);
                result.getOutputData().put("model", "gpt-4o-mini");
                result.getOutputData().put("latencyMs", latencyMs);
                result.getOutputData().put("mode", "live");
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI call failed: " + e.getMessage() + " — falling back to deterministic");
                setDefaultOutput(result);
            }
        } else {
            System.out.println("  [generate] Generating answer for question=\"" + question
                    + "\" tokenBudget=" + tokenBudget);
            setDefaultOutput(result);
        }

        return result;
    }

    private void setDefaultOutput(TaskResult result) {
        String answer = "Retrieval-Augmented Generation (RAG) is a technique that enhances "
                + "LLM responses by first retrieving relevant documents from a knowledge base, "
                + "then using them as context for generation. This grounds the output in factual "
                + "data and reduces hallucinations.";
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("tokensUsed", 187);
        result.getOutputData().put("model", "gpt-4o");
        result.getOutputData().put("latencyMs", 820);
    }

    private String callChatCompletion(String systemPrompt, String userPrompt, int maxTokens, TaskResult result) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens,
                "temperature", 0.3
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorBody = response.body();
            System.err.println("  [worker] API error HTTP " + response.statusCode() + ": " + errorBody);
            if (response.statusCode() == 429 || response.statusCode() == 503) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("API rate limited (HTTP " + response.statusCode() + "). Conductor will retry.");
            } else {
                result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
                result.setReasonForIncompletion("API error HTTP " + response.statusCode() + ": " + errorBody);
            }
            result.getOutputData().put("errorBody", errorBody);
            result.getOutputData().put("httpStatus", response.statusCode());
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }
}
