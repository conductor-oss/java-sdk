package webscrapingrag.workers;

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
 * Worker that generates a final answer from the question and retrieved context.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "wsrag_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> context = (List<Map<String, Object>>) task.getInputData().get("context");
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                StringBuilder contextStr = new StringBuilder();
                if (context != null) {
                    for (Map<String, Object> c : context) {
                        contextStr.append(c.toString()).append("\n");
                    }
                }
                String systemPrompt = "You are a helpful assistant that answers questions based on scraped web content. Provide accurate answers with source attribution.";
                String query = question.isEmpty() ? "What does Conductor do?" : question;
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Context:\n" + contextStr + "\n\nQuestion: " + query)
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
                    System.out.println("  [generate] Response from OpenAI (LIVE)");
                    result.getOutputData().put("answer", answer);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        String answer = "Based on the scraped documentation: Conductor orchestrates microservices and AI pipelines "
                + "with durable execution. Workers are the building blocks, each handling specific task types by "
                + "polling the server. Found in " + context.size() + " scraped sources.";

        System.out.println("  [generate] Answer produced from " + context.size() + " sources");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
