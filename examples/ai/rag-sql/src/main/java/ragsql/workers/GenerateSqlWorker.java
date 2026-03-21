package ragsql.workers;

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
 * Worker that generates a SQL query from the parsed intent, entities, and schema.
 * When CONDUCTOR_OPENAI_API_KEY is set, calls the OpenAI chat completions API (gpt-4o-mini).
 * Otherwise falls back to a default SQL query.
 */
public class GenerateSqlWorker implements Worker {

    private final String openaiApiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GenerateSqlWorker() {
        this.openaiApiKey = System.getenv("CONDUCTOR_OPENAI_API_KEY");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String getTaskDefName() {
        return "sq_generate_sql";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String intent = (String) task.getInputData().get("intent");
        if (intent == null) {
            intent = "";
        }

        Map<String, String> entities = (Map<String, String>) task.getInputData().get("entities");
        if (entities == null) {
            entities = Map.of();
        }

        String schema = (String) task.getInputData().get("schema");
        if (schema == null) {
            schema = "";
        }

        TaskResult result = new TaskResult(task);

        if (openaiApiKey != null && !openaiApiKey.isBlank()) {
            try {
                String sql = callOpenAIChat(intent, entities, schema, result);
                if (sql == null) {
                    return result;
                }
                System.out.println("  [generate_sql] SQL (live OpenAI) for intent \"" + intent + "\": " + sql);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.getOutputData().put("sql", sql);
                result.getOutputData().put("confidence", 0.92);
            } catch (Exception e) {
                System.err.println("  [generate_sql] OpenAI API error: " + e.getMessage());
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("OpenAI API error: " + e.getMessage());
            }
        } else {
            String table = entities.getOrDefault("table", "workflow_executions");
            String metric = entities.getOrDefault("metric", "execution_count");
            String filter = entities.getOrDefault("filter", "status = 'COMPLETED'");
            String groupBy = entities.getOrDefault("groupBy", "workflow_name");

            String sql = "SELECT " + groupBy + ", COUNT(*) AS " + metric
                    + ", AVG(duration_sec) AS avg_duration_sec"
                    + " FROM " + table
                    + " WHERE " + filter
                    + " AND created_at >= NOW() - INTERVAL '7 days'"
                    + " GROUP BY " + groupBy
                    + " ORDER BY " + metric + " DESC"
                    + " LIMIT 10";

            System.out.println("  [generate_sql] Generated SQL for intent \"" + intent + "\": " + sql);

            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("sql", sql);
            result.getOutputData().put("confidence", 0.92);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String callOpenAIChat(String intent, Map<String, String> entities, String schema, TaskResult result) throws Exception {
        String systemPrompt = "You are a SQL expert. Generate a safe, read-only SQL query based on the given intent, "
                + "entities, and schema. Return ONLY the SQL query, no explanation. "
                + "Never use DROP, DELETE, INSERT, UPDATE, or TRUNCATE.";
        String userPrompt = "Intent: " + intent
                + "\nEntities: " + entities
                + "\nSchema: " + schema
                + "\nGenerate the SQL query:";

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
        return ((String) message.get("content")).trim();
    }
}
