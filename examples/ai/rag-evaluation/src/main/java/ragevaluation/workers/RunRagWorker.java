package ragevaluation.workers;

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
 * Worker that demonstrates running a RAG pipeline.
 * Takes a question and returns an answer, context passages, and retrieved doc count.
 */
public class RunRagWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public RunRagWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "re_run_rag";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");

        List<String> context = List.of(
                "Retrieval-Augmented Generation (RAG) enhances LLM outputs by grounding them in external knowledge sources.",
                "The retrieval step uses vector similarity search to find the most relevant documents for a given query.",
                "After retrieval, the generation model conditions its output on both the query and the retrieved passages."
        );

        int retrievedDocs = 3;

        System.out.println("  [run_rag] Processed question: "
                + (question != null ? question : "(default)") + " -> retrieved " + retrievedDocs + " docs");

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String systemPrompt = "You are a RAG pipeline assistant. Answer the question based on the provided context passages.";
                String contextStr = String.join("\n", context);
                String query = (question != null) ? question : "How do RAG pipelines work?";
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
                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("answer", answer);
                    result.getOutputData().put("context", context);
                    result.getOutputData().put("retrievedDocs", retrievedDocs);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("  [generate] OpenAI error, falling back to deterministic. " + e.getMessage());
            }
        }

        String answer = "RAG pipelines combine retrieval and generation to produce grounded answers. "
                + "They first retrieve relevant documents from a knowledge base, then use a language model "
                + "to synthesize an answer based on the retrieved context.";

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("context", context);
        result.getOutputData().put("retrievedDocs", retrievedDocs);
        return result;
    }
}
