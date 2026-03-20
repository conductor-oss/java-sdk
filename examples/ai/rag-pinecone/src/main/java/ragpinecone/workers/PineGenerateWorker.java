package ragpinecone.workers;

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
 * Worker that generates an answer from a question and retrieved Pinecone matches.
 * Builds a deterministic answer by combining the retrieved context passages.
 */
public class PineGenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public PineGenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "pine_generate";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        Object matchesObj = task.getInputData().get("matches");

        if (question == null || question.isBlank()) {
            question = "What is Pinecone?";
        }

        // Build context from matches
        StringBuilder contextBuilder = new StringBuilder();
        if (matchesObj instanceof List<?> matchesList) {
            for (int i = 0; i < matchesList.size(); i++) {
                if (matchesList.get(i) instanceof Map<?, ?> match) {
                    Object metadata = match.get("metadata");
                    if (metadata instanceof Map<?, ?> meta) {
                        String text = (String) meta.get("text");
                        if (text != null) {
                            if (i > 0) {
                                contextBuilder.append(" ");
                            }
                            contextBuilder.append(text);
                        }
                    }
                }
            }
        }
        String context = contextBuilder.toString();

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant. Use the provided context to answer questions accurately."),
                        Map.of("role", "user", "content", "Context:\n" + context + "\n\nQuestion: " + question)
                    ),
                    "max_tokens", 512,
                    "temperature", 0.3
                ));
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Map<String, Object> apiResponse = mapper.readValue(response.body(), Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String answer = (String) message.get("content");
                    System.out.println("  [generate] Response from OpenAI API (LIVE)");
                    result.getOutputData().put("answer", answer);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI API error, falling back to deterministic. " + e.getMessage());
            }
        }

        // Fall through to fallback mode
        StringBuilder answer = new StringBuilder();
        answer.append("Based on ").append(question).append(": ");

        if (matchesObj instanceof List<?> matchesList) {
            for (int i = 0; i < matchesList.size(); i++) {
                if (matchesList.get(i) instanceof Map<?, ?> match) {
                    Object metadata = match.get("metadata");
                    if (metadata instanceof Map<?, ?> meta) {
                        String text = (String) meta.get("text");
                        if (text != null) {
                            if (i > 0) {
                                answer.append(" ");
                            }
                            answer.append(text);
                        }
                    }
                }
            }
        }

        System.out.println("  [generate] Generated answer for: " + question);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer.toString());
        return result;
    }
}
