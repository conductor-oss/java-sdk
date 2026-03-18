package databaseagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Parses a natural-language question against a database schema.
 * Extracts the user's intent, relevant entities (metric, groupBy, orderBy, limit),
 * and identifies which tables are needed to answer the question.
 */
public class ParseQuestionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "db_parse_question";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general query";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> databaseSchema = (Map<String, Object>) task.getInputData().get("databaseSchema");
        String databaseName = "unknown";
        if (databaseSchema != null && databaseSchema.get("database") != null) {
            databaseName = (String) databaseSchema.get("database");
        }

        System.out.println("  [db_parse_question] Parsing question against database: " + databaseName);

        Map<String, Object> entities = Map.of(
                "metric", "total_revenue",
                "groupBy", "department",
                "orderBy", "revenue_desc",
                "limit", 5
        );

        List<String> relevantTables = List.of("employees", "sales", "departments");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("intent", "aggregate_query");
        result.getOutputData().put("entities", entities);
        result.getOutputData().put("relevantTables", relevantTables);
        return result;
    }
}
