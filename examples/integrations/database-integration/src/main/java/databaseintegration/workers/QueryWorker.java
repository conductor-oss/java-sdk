package databaseintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Queries data from source database.
 * Input: connectionId, query
 * Output: rows, rowCount
 */
public class QueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbi_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");

        java.util.List<java.util.Map<String, Object>> rows = java.util.List.of(
                java.util.Map.of("id", 1, "name", "Alice", "email", "alice@example.com", "status", "active"),
                java.util.Map.of("id", 2, "name", "Bob", "email", "bob@example.com", "status", "active"),
                java.util.Map.of("id", 3, "name", "Charlie", "email", "charlie@example.com", "status", "inactive"));
        System.out.println("  [query] Executed: \"" + query + "\" -> " + rows.size() + " rows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rows", rows);
        result.getOutputData().put("rowCount", rows.size());
        return result;
    }
}
