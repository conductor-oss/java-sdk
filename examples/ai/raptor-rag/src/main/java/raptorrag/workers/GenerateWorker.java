package raptorrag.workers;

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
import java.util.stream.Collectors;

/**
 * Worker that generates an answer using the question and multi-level context
 * retrieved from the RAPTOR tree. When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI
 * chat completions API (gpt-4o-mini). Otherwise falls back to a default answer.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "rp_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, Object>> multiLevelContext =
                (List<Map<String, Object>>) task.getInputData().get("multiLevelContext");
        int contextLevels = (multiLevelContext != null) ? multiLevelContext.size() : 0;

        String contextSummary = "";
        if (multiLevelContext != null && !multiLevelContext.isEmpty()) {
            contextSummary = multiLevelContext.stream()
                    .map(ctx -> (String) ctx.get("text"))
                    .collect(Collectors.joining(" "));
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String answer = callOpenAIChat(question, contextSummary, contextLevels, result);
                if (answer == null) {
                    return result;
                }
                System.out.println("  [generate] Answer (live OpenAI) using " + contextLevels + " context levels");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("answer", answer);
                result.getOutputData().put("contextLevelsUsed", contextLevels);
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            System.out.println("  [generate] Generating answer using " + contextLevels + " context levels");

            String answer;
            if (contextLevels > 0) {
                answer = "Based on RAPTOR tree analysis (" + contextLevels
                        + " levels) for \"" + question + "\": " + contextSummary;
            } else {
                answer = "No tree context available to answer: " + question;
            }

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("answer", answer);
            result.getOutputData().put("contextLevelsUsed", contextLevels);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String callOpenAIChat(String question, String contextText, int levels, TaskResult result) throws Exception {
        String systemPrompt = "You are a helpful assistant. Answer the user's question based on the provided "
                + "multi-level RAPTOR tree context. The context comes from " + levels
                + " tree levels (root summaries, cluster details, leaf excerpts).";
        String userPrompt = "Context:\n" + contextText + "\n\nQuestion: " + question;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 512
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
            // 429 (rate limit) and 503 (overloaded) are retryable
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

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
