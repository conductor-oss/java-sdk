package structuredoutput.workers;

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
 * Worker that generates a JSON structure for a given entity and fields.
 * Returns the JSON data along with a schema describing required fields and types.
 *
 * <p>Requires CONDUCTOR_OPENAI_API_KEY to be set. Calls the OpenAI Chat Completions API with
 * structured output instructions.</p>
 */
public class GenerateJsonWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final HttpClient httpClient;

    public GenerateJsonWorker() {
        this.apiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Set CONDUCTOR_OPENAI_API_KEY environment variable to run this worker");
        }
        this.httpClient = HttpClient.newHttpClient();
    }

    /** Package-private constructor for testing with an explicit HTTP client. */
    GenerateJsonWorker(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    @Override
    public String getTaskDefName() {
        return "so_generate_json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String entity = (String) task.getInputData().get("entity");
        Object fields = task.getInputData().get("fields");

        System.out.println("  [so_generate_json] Generating JSON for entity: " + entity
                + ", fields: " + fields);

        Map<String, Object> schema = Map.of(
                "required", List.of("name", "industry", "founded", "employees"),
                "types", Map.of(
                        "name", "string",
                        "industry", "string",
                        "founded", "number",
                        "employees", "number"
                )
        );

        TaskResult result = new TaskResult(task);

        try {
            String prompt = "Generate a JSON object for a " + entity + " entity with these fields: "
                    + fields + ". Include: name (string), industry (string), founded (number), "
                    + "employees (number), headquarters (object with city and state), "
                    + "and tags (array of strings). Return ONLY valid JSON, no markdown or explanation.";

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
            String rawContent = root.at("/choices/0/message/content").asText();

            // Strip markdown code fences if present
            String jsonStr = rawContent.strip();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.replaceFirst("```[a-zA-Z]*\\n?", "");
                jsonStr = jsonStr.replaceFirst("\\n?```$", "");
                jsonStr = jsonStr.strip();
            }

            Map<String, Object> jsonOutput = MAPPER.readValue(jsonStr, Map.class);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("jsonOutput", jsonOutput);
            result.getOutputData().put("schema", schema);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "OpenAI call failed: " + e.getMessage());
        }

        return result;
    }
}
