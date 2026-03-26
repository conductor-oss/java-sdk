package multimodalrag.workers;

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
 * Worker that generates a final answer from the question, multimodal search results,
 * and detected modalities. Produces a comprehensive response that references
 * the different modalities used.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "mm_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, Object>> searchResults =
                (List<Map<String, Object>>) task.getInputData().get("searchResults");
        int resultCount = (searchResults != null) ? searchResults.size() : 0;

        List<String> modalities = (List<String>) task.getInputData().get("modalities");
        if (modalities == null) {
            modalities = List.of();
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                StringBuilder context = new StringBuilder();
                if (searchResults != null) {
                    for (Map<String, Object> sr : searchResults) {
                        context.append(sr.toString()).append("\n");
                    }
                }
                String systemPrompt = "You are a multimodal assistant that generates answers using text, image, and audio context from multiple modalities.";
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
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
                    System.out.println("  [generate] Response from OpenAI (LIVE)");
                    result.getOutputData().put("answer", answer);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        String answer = "Based on multimodal analysis of " + String.join(", ", modalities)
                + " sources for your question \"" + question
                + "\": Multimodal RAG enables comprehensive retrieval across text documents, "
                + "images, and audio recordings. The system detected and processed all "
                + modalities.size() + " modalities, retrieving " + resultCount
                + " relevant results to generate this unified answer.";

        System.out.println("  [generate] Generated answer using " + resultCount
                + " search results across " + modalities.size() + " modalities");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
