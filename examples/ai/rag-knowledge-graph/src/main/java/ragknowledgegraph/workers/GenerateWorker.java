package ragknowledgegraph.workers;

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
 * Worker that generates a final answer using the question, merged context
 * from both graph and vector retrieval, and extracted entities.
 * Returns a comprehensive answer about Conductor's history and capabilities.
 */
public class GenerateWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "kg_generate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        Map<String, Object> mergedContext =
                (Map<String, Object>) task.getInputData().get("mergedContext");

        List<Map<String, String>> entities =
                (List<Map<String, String>>) task.getInputData().get("entities");
        int entityCount = (entities != null) ? entities.size() : 0;

        int totalSources = 0;
        if (mergedContext != null && mergedContext.get("totalSources") instanceof Number) {
            totalSources = ((Number) mergedContext.get("totalSources")).intValue();
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String context = (mergedContext != null) ? mapper.writeValueAsString(mergedContext) : "No context available.";
                String entityInfo = (entities != null) ? mapper.writeValueAsString(entities) : "No entities.";
                String requestJson = mapper.writeValueAsString(Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant. Use the provided context to answer questions accurately."),
                        Map.of("role", "user", "content", "Context:\n" + context + "\n\nEntities:\n" + entityInfo + "\n\nQuestion: " + question)
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
        System.out.println("  [generate] Generating answer with " + totalSources
                + " sources and " + entityCount + " entities");

        String answer = "Conductor is a workflow orchestration engine originally developed by Netflix "
                + "in 2016 to manage microservices workflows at scale. It was later open-sourced and "
                + "adopted by Orkes for enterprise cloud offerings. Conductor uses a JSON-based DSL "
                + "for defining workflows with support for conditional branching, parallel execution, "
                + "and task queue management. It decouples task execution from workflow logic, "
                + "improving reliability across distributed systems. "
                + "(Based on " + totalSources + " sources across " + entityCount + " identified entities)";

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        return result;
    }
}
