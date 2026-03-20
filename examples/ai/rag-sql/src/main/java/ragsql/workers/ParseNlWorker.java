package ragsql.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that parses a natural-language question against a database schema.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI chat completions API (gpt-4o-mini)
 * to extract intent and entities. Otherwise falls back to fixed deterministic parsing.
 */
public class ParseNlWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ParseNlWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "sq_parse_nl";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        String dbSchema = (String) task.getInputData().get("dbSchema");
        if (dbSchema == null) {
            dbSchema = "";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                Map<String, Object> parsed = callOpenAIParse(question, dbSchema, result);
                if (parsed == null) {
                    return result;
                }
                String intent = (String) parsed.getOrDefault("intent", "aggregate_query");
                @SuppressWarnings("unchecked")
                Map<String, String> entities = (Map<String, String>) parsed.getOrDefault("entities", Map.of());
                System.out.println("  [parse_nl] Parsed (live OpenAI) question: \"" + question + "\"");
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("intent", intent);
                result.getOutputData().put("entities", entities);
            } catch (Exception e) {
                System.err.println("  [parse_nl] OpenAI API error, falling back to deterministic. " + e.getMessage());
                // Fall back to deterministic.on error
                setDefaultResult(question, result);
            }
        } else {
            System.out.println("  [parse_nl] Parsing question: \"" + question + "\"");
            setDefaultResult(question, result);
        }

        return result;
    }

    private void setDefaultResult(String question, TaskResult result) {
        Map<String, String> entities = new LinkedHashMap<>();
        entities.put("table", "workflow_executions");
        entities.put("metric", "execution_count");
        entities.put("filter", "status = 'COMPLETED'");
        entities.put("groupBy", "workflow_name");
        entities.put("timeRange", "last 7 days");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("intent", "aggregate_query");
        result.getOutputData().put("entities", entities);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callOpenAIParse(String question, String dbSchema, TaskResult result) throws Exception {
        String systemPrompt = "You are a SQL query planner. Given a natural language question and database schema, "
                + "extract the intent and entities. Return a JSON object with:\n"
                + "- \"intent\": string (e.g., \"aggregate_query\", \"lookup\", \"filter\")\n"
                + "- \"entities\": object with keys: table, metric, filter, groupBy, timeRange\n"
                + "Return ONLY valid JSON, no other text.";
        String userPrompt = "Question: " + question + "\nSchema: " + dbSchema;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 256
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
        String content = ((String) message.get("content")).trim();

        // Strip markdown code fences if present
        if (content.startsWith("```")) {
            content = content.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "").trim();
        }

        return objectMapper.readValue(content, Map.class);
    }
}
