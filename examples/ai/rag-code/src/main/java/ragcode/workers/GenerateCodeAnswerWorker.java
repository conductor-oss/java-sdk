package ragcode.workers;

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
 * Worker that generates a code-aware answer from the question and retrieved code snippets.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI chat completions API (gpt-4o-mini).
 * Otherwise falls back to a default answer with code example from the top snippet.
 */
public class GenerateCodeAnswerWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GenerateCodeAnswerWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "cr_generate_code_answer";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, Object>> codeSnippets = (List<Map<String, Object>>) task.getInputData().get("codeSnippets");
        int snippetCount = (codeSnippets != null) ? codeSnippets.size() : 0;

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String contextText = "";
                if (codeSnippets != null && !codeSnippets.isEmpty()) {
                    contextText = codeSnippets.stream()
                            .map(s -> "File: " + s.get("file") + "\nSignature: " + s.get("signature")
                                    + "\n```\n" + s.get("body") + "\n```")
                            .collect(Collectors.joining("\n\n"));
                }
                String llmAnswer = callOpenAIChat(question, contextText, result);
                if (llmAnswer == null) {
                    return result;
                }
                String codeExample = (codeSnippets != null && !codeSnippets.isEmpty())
                        ? (String) codeSnippets.get(0).get("body")
                        : "// No code example available";
                System.out.println("  [generate_code_answer] Answer (live OpenAI) for: \"" + question
                        + "\" using " + snippetCount + " code snippets");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("answer", llmAnswer);
                result.getOutputData().put("codeExample", codeExample);
            } catch (Exception e) {
                System.err.println("  [generate_code_answer] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            System.out.println("  [generate_code_answer] Generating answer for: \"" + question
                    + "\" using " + snippetCount + " code snippets");

            String answer;
            String codeExample;

            if (codeSnippets != null && !codeSnippets.isEmpty()) {
                Map<String, Object> topSnippet = codeSnippets.get(0);
                String signature = (String) topSnippet.get("signature");
                String file = (String) topSnippet.get("file");
                String body = (String) topSnippet.get("body");

                answer = "Based on " + snippetCount + " code snippets found, here is how the pattern is used: "
                        + "The method '" + signature + "' in " + file
                        + " demonstrates this pattern. It follows standard conventions for error handling and return types.";
                codeExample = body;
            } else {
                answer = "No matching code snippets were found for your question: \"" + question
                        + "\". Try refining your query or specifying a different language.";
                codeExample = "// No code example available";
            }

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("answer", answer);
            result.getOutputData().put("codeExample", codeExample);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String callOpenAIChat(String question, String contextText, TaskResult result) throws Exception {
        String systemPrompt = "You are a code-aware assistant. Answer the user's programming question based on the "
                + "provided code snippets. Include relevant code examples in your answer.";
        String userPrompt = "Code context:\n" + contextText + "\n\nQuestion: " + question;

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
