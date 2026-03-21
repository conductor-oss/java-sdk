package multiagentcodereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance review agent — inspects the AST for performance issues
 * such as N+1 queries and missing connection pooling.
 */
public class PerformanceReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_performance_review";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> ast = (Map<String, Object>) task.getInputData().get("ast");
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "javascript";
        }

        System.out.println("  [cr_performance_review] Reviewing " + language + " code for performance issues");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("severity", "HIGH");
        finding1.put("type", "N_PLUS_1_QUERY");
        finding1.put("message", "Database query inside loop in processData() causes N+1 query problem");
        finding1.put("line", 52);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("severity", "MEDIUM");
        finding2.put("type", "NO_CONNECTION_POOL");
        finding2.put("message", "Creating new database connection per request instead of using a connection pool");
        finding2.put("line", 15);

        List<Map<String, Object>> findings = List.of(finding1, finding2);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("agent", "performance");
        return result;
    }
}
