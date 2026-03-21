package semistructuredrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches structured data sources based on the classified
 * structured fields. Returns results with field, value, table, and match score.
 */
public class SearchStructuredWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ss_search_structured";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        List<Map<String, String>> structuredFields =
                (List<Map<String, String>>) task.getInputData().get("structuredFields");
        int fieldCount = (structuredFields != null) ? structuredFields.size() : 0;

        System.out.println("  [search-structured] Querying " + fieldCount + " structured fields");

        List<Map<String, Object>> results = List.of(
                Map.of("field", "revenue", "value", "$4.2M", "table", "financials_db", "match", 0.95),
                Map.of("field", "employee_count", "value", "342", "table", "hr_db", "match", 0.89),
                Map.of("field", "department", "value", "Engineering", "table", "org_db", "match", 0.82)
        );

        System.out.println("  [search-structured] Found " + results.size() + " structured results");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        return result;
    }
}
